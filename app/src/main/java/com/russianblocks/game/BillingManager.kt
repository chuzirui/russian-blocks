package com.russianblocks.game

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*

/**
 * Manages Google Play Billing for consumable bomb packs.
 *
 * Product IDs must be created in the Google Play Console under
 * In-app products as consumable items:
 *   bomb_pack_3   → 3 bombs
 *   bomb_pack_10  → 10 bombs
 *   bomb_pack_25  → 25 bombs
 */
class BillingManager(
    private val activity: Activity,
    private val onBombsPurchased: (amount: Int) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        val PRODUCTS = listOf(
            BombProduct("bomb_pack_3",  3,  "3 Bombs"),
            BombProduct("bomb_pack_10", 10, "10 Bombs"),
            BombProduct("bomb_pack_25", 25, "25 Bombs")
        )
    }

    data class BombProduct(
        val id: String,
        val bombCount: Int,
        val title: String,
        var price: String = "..."
    )

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetails = mutableMapOf<String, ProductDetails>()

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryProducts()
                    handlePendingPurchases()
                } else {
                    Log.d(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing disconnected")
            }
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(PRODUCTS.map { product ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(product.id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            })
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in detailsList) {
                    productDetails[details.productId] = details
                    PRODUCTS.find { it.id == details.productId }?.price =
                        details.oneTimePurchaseOfferDetails?.formattedPrice ?: "..."
                }
                Log.d(TAG, "Loaded ${detailsList.size} products")
            }
        }
    }

    fun launchPurchase(productId: String) {
        val details = productDetails[productId]
        if (details == null) {
            Log.d(TAG, "Product not found: $productId")
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            ))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled")
        } else {
            Log.d(TAG, "Purchase error: ${result.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val productId = purchase.products.firstOrNull() ?: return
        val bombProduct = PRODUCTS.find { it.id == productId } ?: return

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Consumed: $productId, awarding ${bombProduct.bombCount} bombs")
                activity.runOnUiThread {
                    onBombsPurchased(bombProduct.bombCount)
                }
            }
        }
    }

    private fun handlePendingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
