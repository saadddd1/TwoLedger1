package com.example.ledger.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.example.ledger.R
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AccessibilityMonitorService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var db: AppDatabase

    companion object {
        private const val CHANNEL_ID = "ledger_accessibility_service"
        private const val FOREGROUND_ID = 1002

        private val TARGET_PACKAGES = setOf(
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "com.unionpay",
            "com.jingdong.app.mall",
            "com.sankuai.meituan",
            "com.taobao.taobao"
        )

        // 2026最新微信/支付宝匹配规则
        private val PAY_SUCCESS_KEYWORDS = listOf(
            "支付成功", "付款成功", "完成支付", "交易成功",
            "已付款", "支付完成", "付款完成", "转账成功",
            "微信支付凭证", "支付宝账单", "账单详情", "支付结果",
            "付款给", "收款方", "交易详情", "订单详情", "交易金额", "付款金额"
        )

        private val AMOUNT_PATTERNS = listOf(
            Regex("""[¥￥-]\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""(?:付款金额|支付金额|交易金额|实付款|总价)[:：]?\s*[¥￥]?\s*(\d+(?:\.\d{1,2})?)"""),
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""),
            // 保底提取符合 0.00 格式的浮点数，避开日期格式如 2026-04-25
            Regex("""(?<!\d-|-)(?<=\s|^)(\d+\.\d{2})(?=\s|$)""")
        )

        private val MERCHANT_PATTERNS = listOf(
            Regex("""(?:收款方|商户全称|交易对象|付款给|商户名称)[:：]?\s*(.+?)(?:\s|$)"""),
            Regex("""在(.+?)消费"""),
            Regex("""(.+?)(?:-.*?商品|的微店|的小店)""")
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AppDatabase.getDatabase(applicationContext)
        Log.d("AccessibilityMonitor", "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (packageName !in TARGET_PACKAGES) return

        try {
            val root = rootInActiveWindow ?: return
            processPage(root, packageName)
        } catch (e: Exception) {
            Log.e("AccessibilityMonitor", "处理页面失败", e)
        }
    }

    private fun processPage(root: AccessibilityNodeInfo, packageName: String) {
        val allText = mutableListOf<String>()
        traverseNode(root, allText)

        val pageContent = allText.joinToString(" ")
        if (PAY_SUCCESS_KEYWORDS.none { pageContent.contains(it) }) {
            return
        }

        // 提取金额
        var amount: Double? = null
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(pageContent)
            if (match != null) {
                // 尝试提取捕获组 1 中的数字
                val valueStr = match.groups[1]?.value ?: match.value
                val value = valueStr.trim().replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                if (value != null && value > 0) {
                    amount = value
                    break
                }
            }
        }

        if (amount == null) return

        // 提取商户名
        val merchantName = extractMerchantName(pageContent, packageName)

        // 去重
        if (!BillDeduplicator.shouldRecordBill(amount, packageName, System.currentTimeMillis())) {
            return
        }

        scope.launch {
            val appSource = when (packageName) {
                "com.tencent.mm" -> "Wechat (微信)"
                "com.eg.android.AlipayGphone" -> "Alipay (支付宝)"
                "com.unionpay" -> "UnionPay (云闪付)"
                "com.jingdong.app.mall" -> "京东"
                "com.sankuai.meituan" -> "美团"
                "com.taobao.taobao" -> "淘宝"
                else -> "Other"
            }

            db.autoBillDao().insertAutoBill(
                AutoBill(
                    appSource = appSource,
                    merchantName = merchantName,
                    amount = amount,
                    paymentMethod = "无障碍服务",
                    fullPayeeName = merchantName,
                    timestampMillis = System.currentTimeMillis()
                )
            )

            Log.d("AccessibilityMonitor", "捕获账单: $appSource - $amount - $merchantName")
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo, result: MutableList<String>) {
        node.text?.let {
            if (it.isNotBlank()) {
                result.add(it.toString())
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            child?.let { traverseNode(it, result) }
        }
    }

    private fun extractMerchantName(content: String, packageName: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(content)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank() && name.length < 50) {
                    return name
                }
            }
        }
        return when (packageName) {
            "com.tencent.mm" -> "微信支付"
            "com.eg.android.AlipayGphone" -> "支付宝支付"
            else -> "未命名账单"
        }
    }

    override fun onInterrupt() {
        // Accessibility feedback interrupted. Do nothing.
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.d("AccessibilityMonitor", "无障碍服务已销毁")
    }
}
