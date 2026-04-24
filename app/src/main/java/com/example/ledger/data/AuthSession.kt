package com.example.ledger.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 简单的全局状态管理，实际生产环境可考虑 DataStore 或 SharedPreferences 固化存储
object AuthSession {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isVip = MutableStateFlow(false)
    val isVip: StateFlow<Boolean> = _isVip.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun login(token: String, isVip: Boolean) {
        this._token.value = token
        this._isVip.value = isVip
        this._isLoggedIn.value = true
    }

    fun logout() {
        this._token.value = null
        this._isVip.value = false
        this._isLoggedIn.value = false
    }

    fun updateVipStatus(isVip: Boolean) {
        this._isVip.value = isVip
    }
}
