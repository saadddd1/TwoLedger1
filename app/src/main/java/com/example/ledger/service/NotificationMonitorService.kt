package com.example.ledger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.ledger.R
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationMonitorService : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    companion object {
        private const val CHANNEL_ID = "ledger_monitor_service"
        private const val FOREGROUND_ID = 1001

        // 最新支付关键词适配
        private val SUCCESS_KEYWORDS = listOf(
            "支付付款", "已支付", "支付成功", "完成付款", "付款金额",
            "付款成功", "交易成功", "消费成功", "转账成功", "扫码付款",
            "支付完成", "付款完成", "支付款项", "支出通知", "交易人民币",
            "微信支付", "支付凭证", "转账给", "已收钱", "收款到账",
            "订单支付", "实付款",
            // 微信红包/转账
            "红包已发送", "已领取", "红包记录", "发出红包", "已被领取",
            "对方已收钱", "已转账", "转账通知",
            // 银行消费
            "消费支出", "支出提醒", "交易提醒", "消费提醒",
            "快捷支付", "银联支付", "网上银行", "支出人民币", "消费人民币"
        )

        // 最新金额匹配规则
        private val PATTERNS = listOf(
            Regex("""(?:¥|￥|人民币|-|支付成功|付款成功|消费)\s*([0-9,]+\.\d{2})"""),
            Regex("""(?:支出|消费|支付|交易|付款金额|实付款|红包金额|转账金额)\s*([0-9,]+\.\d{2})\s*[元]?"""),
            Regex("""(?:金额|付款|付款给)(?:[：:]?)\s*([0-9,]+\.\d{2})"""),
            Regex("""(?:支出人民币|消费人民币|付款)\s*[¥￥]?\s*([0-9,]+\.\d{2})"""),
            // 银行短信: 尾号1234卡...人民币500.00
            Regex("""尾号\d{4}.*?([0-9,]+\.\d{2})"""),
            Regex("""([0-9,]+\.\d{2})\s*元"""),
            // 保底提取
            Regex("""(?<!\d-|-)(?<=\s|^)([0-9,]+\.\d{2})(?=\s|$)""")
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        db = AppDatabase.getDatabase(applicationContext)
        Log.d("NotificationMonitor", "通知监听服务已连接")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combinedContent = listOf(title, text, subText, bigText)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        // 扩展支持的应用列表
        val isPaymentApp = when (packageName) {
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.unionpay",
            "com.jingdong.app.mall",
            "com.sankuai.meituan",
            "com.taobao.taobao",
            "com.xunmeng.pinduoduo",
            "com.ss.android.ugc.aweme",
            "me.ele",
            "com.sdu.did.psnger",
            "cmb.pb",
            "com.icbc",
            "com.chinamworld.boc",
            "com.android.bankabc",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.messaging" -> true
            else -> packageName.contains("bank", ignoreCase = true) ||
                    packageName.contains("ccb", ignoreCase = true)
        }

        if (!isPaymentApp) return

        if (SUCCESS_KEYWORDS.none { combinedContent.contains(it, ignoreCase = true) }) return

        scope.launch {
            try {
                // 顺序匹配所有规则
                var amount: Double? = null
                for (pattern in PATTERNS) {
                    val match = pattern.find(combinedContent)
                    if (match != null) {
                        amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                        if (amount != null && amount > 0) break
                    }
                }

                val finalAmount = amount ?: return@launch
                val now = sbn.postTime

                // 先提取商户名，再去做重（商户名参与指纹）
                val merchantName = extractMerchantName(combinedContent, packageName, title)

                // 全局去重
                if (!BillDeduplicator.shouldRecordBill(finalAmount, packageName, merchantName, now)) {
                    return@launch
                }

                val appSource = when (packageName) {
                    "com.tencent.mm" -> "Wechat (微信)"
                    "com.eg.android.AlipayGphone" -> "Alipay (支付宝)"
                    "com.unionpay" -> "UnionPay (云闪付)"
                    "com.jingdong.app.mall" -> "京东"
                    "com.sankuai.meituan" -> "美团"
                    "com.taobao.taobao" -> "淘宝"
                    "com.xunmeng.pinduoduo" -> "拼多多"
                    "com.ss.android.ugc.aweme" -> "抖音"
                    "me.ele" -> "饿了么"
                    "com.sdu.did.psnger" -> "滴滴"
                    "cmb.pb" -> "招商银行"
                    "com.icbc" -> "工商银行"
                    "com.chinamworld.boc" -> "中国银行"
                    "com.android.bankabc" -> "农业银行"
                    "com.android.mms", "com.google.android.apps.messaging", "com.android.messaging" -> "SMS (短信)"
                    else -> "Bank App (银行)"
                }

                db.autoBillDao().insertAutoBill(
                    AutoBill(
                        appSource = appSource,
                        merchantName = merchantName,
                        amount = finalAmount,
                        paymentMethod = "通知读取",
                        fullPayeeName = merchantName,
                        timestampMillis = now
                    )
                )

                Log.d("NotificationMonitor", "捕获账单: $appSource - $finalAmount - $merchantName")

            } catch (e: Exception) {
                Log.e("NotificationMonitor", "解析通知失败", e)
            }
        }
    }

    private fun extractMerchantName(content: String, packageName: String, title: String): String {
        return when (packageName) {
            "com.tencent.mm" -> {
                Regex("""收款方(?:：|:)?(.*?)(?:\s|$)""").find(content)?.groupValues?.get(1)?.trim()
                    ?: Regex("""付款给(.*?)(?:\s|$)""").find(content)?.groupValues?.get(1)?.trim()
                    ?: Regex("""来自(.+?)的""").find(content)?.groupValues?.get(1)?.trim()
                    ?: if (title.isNotBlank() && title !in listOf("微信支付", "支付结果通知", "服务通知")) title.trim() else "微信支付"
            }
            "com.eg.android.AlipayGphone" -> {
                Regex("""收款方(?:：|:)?(.*?)(?:\s|$)""").find(content)?.groupValues?.get(1)?.trim()
                    ?: Regex("""在(.*?)支付成功""").find(content)?.groupValues?.get(1)?.trim()
                    ?: "支付宝支付"
            }
            "com.xunmeng.pinduoduo" -> "拼多多"
            "com.ss.android.ugc.aweme" -> "抖音"
            "me.ele" -> "饿了么"
            "com.sdu.did.psnger" -> "滴滴出行"
            // 银行 SMS: 尝试提取「在XXX消费」
            else -> {
                Regex("""在(.+?)消费""").find(content)?.groupValues?.get(1)?.trim()
                    ?: Regex("""(.+?)支出""").find(content)?.groupValues?.get(1)?.trim()
                    ?: title.takeIf { it.isNotBlank() }
                    ?: "未命名账单"
            }
        }.replace("交易成功", "").replace("支付成功", "").replace("消费人民币", "").trim()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotificationMonitor", "通知监听服务已断开")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.d("NotificationMonitor", "通知监听服务已销毁")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}

