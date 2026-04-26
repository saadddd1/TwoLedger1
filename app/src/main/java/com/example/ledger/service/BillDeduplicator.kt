package com.example.ledger.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object BillDeduplicator {
    private data class BillFingerprint(
        val amount: Double,
        val appSource: String,
        val merchantName: String,
        val timestamp: Long
    )

    private val recentBills = ConcurrentHashMap<BillFingerprint, Long>()
    private val DEDUPLICATION_WINDOW_MS = TimeUnit.SECONDS.toMillis(30)

    @Synchronized
    fun shouldRecordBill(amount: Double, appSource: String, merchantName: String, timestamp: Long): Boolean {
        cleanupExpired()

        // 跨引擎去重：同应用+同金额+同商户，30秒内只记录一次
        // 加入商户名防止连续支付两笔相同金额给不同商户时误去重
        val targetMerchant = merchantName.ifBlank { "unknown" }
        val existing = recentBills.keys.find {
            it.amount == amount && it.appSource == appSource &&
                    it.merchantName == targetMerchant &&
                    Math.abs(timestamp - recentBills[it]!!) < DEDUPLICATION_WINDOW_MS
        }

        return if (existing != null) {
            // 重复账单，跳过（双引擎互斥）
            false
        } else {
            recentBills[BillFingerprint(amount, appSource, targetMerchant, timestamp)] = timestamp
            true
        }
    }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        recentBills.entries.removeIf { (_, time) ->
            now - time > DEDUPLICATION_WINDOW_MS
        }
    }

    fun clear() {
        recentBills.clear()
    }
}
