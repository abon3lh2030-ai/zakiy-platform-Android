package com.zakiy.platform.billing

/** باقات الاشتراك الأربع - نفس أسعار وحدود SUBSCRIPTION_PLANS بالباك إند
 * بالضبط (لازم يبقيان متطابقين لو تغيّرت الأسعار مستقبلًا). معرّفات المنتجات
 * لازم تُنشأ فعليًا بـ Google Play Console بنفس هذي القيم بالضبط قبل
 * ما الاشتراك الفعلي يشتغل. */
enum class PlanTier(val key: String) { FREE("free"), PLUS("plus"), PRO("pro"), ULTIMATE("ultimate") }

data class PlanProductIds(val monthly: String, val yearly: String)

object PlanCatalog {
    val productIds = mapOf(
        PlanTier.PLUS to PlanProductIds("zakiy_plus_monthly", "zakiy_plus_yearly"),
        PlanTier.PRO to PlanProductIds("zakiy_pro_monthly", "zakiy_pro_yearly"),
        PlanTier.ULTIMATE to PlanProductIds("zakiy_ultimate_monthly", "zakiy_ultimate_yearly"),
    )

    val allProductIds: List<String> = productIds.values.flatMap { listOf(it.monthly, it.yearly) }

    fun tierFor(productId: String): PlanTier =
        productIds.entries.firstOrNull { it.value.monthly == productId || it.value.yearly == productId }?.key ?: PlanTier.FREE
}
