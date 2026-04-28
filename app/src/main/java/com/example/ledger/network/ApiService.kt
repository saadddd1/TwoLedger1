package com.example.ledger.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

data class LoginRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String
)
data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("isVip") val isVip: Boolean,
    @SerializedName("vipExpireAt") val vipExpireAt: Long?
)

data class WechatLoginRequest(
    @SerializedName("code") val code: String
)

data class SyncDataRequest(
    @SerializedName("dataJson") val dataJson: String
)
data class SyncDataResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class VipPayRequest(
    @SerializedName("planId") val planId: String,
    @SerializedName("payMethod") val payMethod: String
) // payMethod: "alipay" or "wechat"
data class VipPayResponse(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("payUrl") val payUrl: String
)

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
