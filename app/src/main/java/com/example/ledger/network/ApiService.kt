package com.example.ledger.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

data class LoginRequest(val phone: String, val code: String)
data class LoginResponse(val token: String, val userId: String, val isVip: Boolean, val vipExpireAt: Long?)

data class WechatLoginRequest(val code: String)

data class SyncDataRequest(val dataJson: String)
data class SyncDataResponse(val success: Boolean, val message: String)

data class VipPayRequest(val planId: String, val payMethod: String) // payMethod: "alipay" or "wechat"
data class VipPayResponse(val orderId: String, val payUrl: String)

interface ApiService {
    // 1. 手机号登录/注册
    @POST("/api/auth/login/phone")
    suspend fun loginByPhone(@Body request: LoginRequest): LoginResponse

    // 2. 微信登录/注册
    @POST("/api/auth/login/wechat")
    suspend fun loginByWechat(@Body request: WechatLoginRequest): LoginResponse

    // 3. 数据云同步 (需要开通VIP会员)
    @POST("/api/sync/upload")
    suspend fun uploadSyncData(
        @Header("Authorization") token: String,
        @Body request: SyncDataRequest
    ): SyncDataResponse

    // 4. 获取支付参数，购买VIP
    @POST("/api/vip/pay")
    suspend fun createVipOrder(
        @Header("Authorization") token: String,
        @Body request: VipPayRequest
    ): VipPayResponse
}
