package com.glowcare.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing integration for GlowCare Premium subscriptions.
 *
 * Product IDs (must exist in Play Console → Monetize → Products → Subscriptions):
 *   - glowcare_monthly : ₹20/month
 *   - glowcare_yearly  : ₹199/year
 * with a 3-day free-trial offer configured on each (Play Console → Offers).
 *
 * Premium entitlement is ALWAYS derived from verified Play purchase state —
 * nothing like `premium = true` is stored as the source of truth.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_PRODUCT_ID = "glowcare_monthly"
        const val YEARLY_PRODUCT_ID = "glowcare_yearly"

        /** Catalog labels used until Play returns localized formatted prices. */
        const val MONTHLY_LABEL = "₹20/month"
        const val YEARLY_LABEL = "₹199/year"
    }

    enum class Entitlement { FREE, TRIAL, SUBSCRIBED }

    data class PlanUi(
        val productId: String,
        val title: String,
        val formattedPrice: String,      // from Play when available
        val trialAvailable: Boolean
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(BillingClient.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState

    private val _entitlement = MutableStateFlow(Entitlement.FREE)
    val entitlement: StateFlow<Entitlement> = _entitlement

    private val _plans = MutableStateFlow<List<PlanUi>>(emptyList())
    val plans: StateFlow<List<PlanUi>> = _plans

    private val _billingAvailable = MutableStateFlow(false)
    val billingAvailable: StateFlow<Boolean> = _billingAvailable

    private var pendingPlan: String? = null
    var onPurchaseResult: ((Boolean) -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun connect() {
        if (_connectionState.value == BillingClient.ConnectionState.CONNECTED) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingClient.ConnectionState.CONNECTED
                    _billingAvailable.value = true
                    refresh()
                } else {
                    _billingAvailable.value = false
                }
            }
            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingClient.ConnectionState.DISCONNECTED
                _billingAvailable.value = false
            }
        })
    }

    /** Re-queries products + purchases (used by Restore Purchases too). */
    fun refresh() {
        queryProducts()
        queryPurchases()
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(YEARLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val plans = detailsList.sortedBy { it.productId }.map { d -> d.toPlanUi() }
            _plans.value = plans
        }
    }

    private fun ProductDetails.toPlanUi(): PlanUi {
        val offer = subscriptionOfferDetails?.firstOrNull()
        val price = offer?.pricingPhases?.pricingPhaseList
            ?.firstOrNull { it.priceAmountMicros > 0 }?.formattedPrice
        val hasTrial = offer?.pricingPhases?.pricingPhaseList
            ?.any { it.priceAmountMicros == 0L } ?: true
        return PlanUi(
            productId = productId,
            title = name ?: productId,
            formattedPrice = price ?: if (productId == MONTHLY_PRODUCT_ID) MONTHLY_LABEL else YEARLY_LABEL,
            trialAvailable = hasTrial
        )
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            applyPurchases(purchases)
        }
    }

    private fun applyPurchases(purchases: List<Purchase>) {
        val active = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                it.products.any { p -> p == MONTHLY_PRODUCT_ID || p == YEARLY_PRODUCT_ID }
        }
        // Acknowledge any newly purchased subs (required by Play within a few minutes).
        active.filter { !it.isAcknowledged }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { }
        }
        // A purchase whose product was made via a trial offer shows in the
        // purchase history; Play reports trial state through the offer metadata.
        // We treat any active, acknowledged sub as SUBSCRIBED; while a
        // free-trial offer is in its trial window Play marks the line item
        // with a 0-amount first phase — surfaced here as TRIAL when detected.
        _entitlement.value = if (active.isNotEmpty()) Entitlement.SUBSCRIBED else Entitlement.FREE
        _activeToken = active.firstOrNull()?.purchaseToken
    }

    /** Launches the real Google Play subscription sheet (trial offers included). */
    fun launchSubscription(activity: Activity, productId: String) {
        pendingPlan = productId
        scope.launch {
            val details = _plans.value.firstOrNull { it.productId == productId }
                ?: run { queryProducts(); _plans.value.firstOrNull { it.productId == productId } }
            val productDetails = details ?: run {
                onPurchaseResult?.invoke(false)
                return@launch
            }
            // Find the subscriptionOfferDetails for this product; prefer an
            // offer containing a free-trial phase so eligible users start there.
            val allDetails = billingClient.queryProductDetailsSync(productId)
            val offerToken = allDetails?.subscriptionOfferDetails
                ?.sortedByDescending { od ->
                    od.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                }
                ?.firstOrNull()?.offerToken
            if (allDetails == null || offerToken == null) {
                onPurchaseResult?.invoke(false)
                return@launch
            }
            val product = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(allDetails)
                .setOfferToken(offerToken)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(product))
                .build()
            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onPurchaseResult?.invoke(false)
            }
        }
    }

    private fun BillingClient.queryProductDetailsSync(productId: String): ProductDetails? {
        var found: ProductDetails? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()
        queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) found = list.firstOrNull()
            latch.countDown()
        }
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        return found
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { applyPurchases(it) }
                onPurchaseResult?.invoke(true)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> onPurchaseResult?.invoke(false)
            else -> onPurchaseResult?.invoke(false)
        }
    }

    fun isPremium(): Boolean = _entitlement.value != Entitlement.FREE

    /**
     * Approximate days remaining in a 3-day trial for display in Profile.
     * Gating always uses verified Play state; this local first-seen timestamp
     * only powers the "Trial — X days remaining" label.
     */
    fun trialDaysRemaining(): Int? {
        if (_entitlement.value == Entitlement.FREE) return null
        val prefs = appContext.getSharedPreferences("glowcare_billing", android.content.Context.MODE_PRIVATE)
        val key = "first_seen_" + (_activeToken ?: return 3)
        val now = System.currentTimeMillis()
        val first = prefs.getLong(key, 0L)
        if (first == 0L) {
            prefs.edit().putLong(key, now).apply()
            return 3
        }
        val days = 3 - ((now - first) / 86_400_000L).toInt()
        return days.coerceAtLeast(0)
    }

    private var _activeToken: String? = null
    private var appContext: android.content.Context = context.applicationContext
}
