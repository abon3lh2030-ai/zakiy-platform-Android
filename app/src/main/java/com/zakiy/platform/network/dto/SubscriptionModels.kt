package com.zakiy.platform.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanLimits(
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("price_monthly") val priceMonthly: Double,
    @SerialName("price_annual") val priceAnnual: Double,
    @SerialName("library_limit") val libraryLimit: Int? = null,
    @SerialName("solo_daily") val soloDaily: Int? = null,
    @SerialName("group_daily") val groupDaily: Int? = null,
    @SerialName("lesson_daily") val lessonDaily: Int? = null,
    @SerialName("archive_limit") val archiveLimit: Int? = null,
    @SerialName("performance_limit") val performanceLimit: Int? = null,
)

@Serializable
data class SubscriptionPlansResponse(val plans: Map<String, PlanLimits>)

@Serializable
data class SubscriptionMeResponse(
    val tier: String,
    val period: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val unlimited: Boolean = false,
    val limits: PlanLimits,
)

@Serializable
data class CheckoutRequest(val plan: String, val period: String)

@Serializable
data class CheckoutResponse(
    @SerialName("order_id") val orderId: String,
    val plan: String,
    val period: String,
    val amount: Double,
    val currency: String,
)

@Serializable
data class GoogleVerifyRequest(@SerialName("product_id") val productId: String)

@Serializable
data class SubscriptionSyncResponse(
    val tier: String,
    val period: String,
    @SerialName("expires_at") val expiresAt: String,
)
