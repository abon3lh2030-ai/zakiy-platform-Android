package com.zakiy.platform.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.zakiy.platform.network.NetworkModule
import com.zakiy.platform.network.dto.GoogleVerifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** يدير اشتراكات Google Play Billing - يزامن أي شراء ناجح مع الباك إند
 * (POST /api/subscription/google/verify) عشان الاشتراك يبقى معروف بالحساب
 * مو محلي بالجهاز بس. لازم تُنشأ معرّفات المنتجات (PlanCatalog) فعليًا
 * بـ Play Console قبل ما هذا يشتغل بالإنتاج. */
class BillingManager(context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var productDetailsList: List<ProductDetails> = emptyList()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return@PurchasesUpdatedListener
        purchases?.forEach { handlePurchase(it) }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases()
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    restorePurchases()
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                // يعيد الاتصال تلقائيًا عند أول محاولة شراء لاحقة
            }
        })
    }

    private fun loadProducts() {
        val products = PlanCatalog.allProductIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        client.queryProductDetailsAsync(params) { _, result ->
            productDetailsList = result.productDetailsList ?: emptyList()
        }
    }

    fun productDetailsFor(productId: String): ProductDetails? = productDetailsList.firstOrNull { it.productId == productId }

    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetailsFor(productId) ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        client.launchBillingFlow(activity, flowParams)
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { _, purchases -> purchases.forEach { handlePurchase(it) } }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(ackParams) {}
        }
        val productId = purchase.products.firstOrNull() ?: return
        scope.launch {
            runCatching { NetworkModule.backendApi.subscriptionGoogleVerify(GoogleVerifyRequest(productId)) }
        }
    }
}
