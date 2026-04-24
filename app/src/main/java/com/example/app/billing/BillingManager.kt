package com.example.app.billing

import android.app.Activity
import android.content.Context
import androidx.annotation.OptIn
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class BillingManager(private val context: Context) {

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_99rub"
    }

    private var billingClient: BillingClient? = null
    private val purchaseEvent = Channel<Result<Boolean>>(Channel.BUFFERED)

    val purchasesFlow: Flow<Result<Boolean>> = purchaseEvent.receiveAsFlow()

    private var isConnected = false

    fun connect() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .build()
            .apply { startConnection(billingClientStateListener) }
    }

    fun purchase(activity: Activity) {
        val productIdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productIdParams))
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    fun queryPurchases() {
        if (!isConnected) {
            purchaseEvent.trySend(Result.failure(Exception("Billing not connected")))
            return
        }

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(queryPurchasesParams) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                purchaseEvent.trySend(Result.success(hasPremium))
            } else {
                purchaseEvent.trySend(Result.failure(Exception("Query failed: ${result.debugMessage}")))
            }
        }
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchaseEvent.trySend(Result.success(true))
                    } else {
                        purchaseEvent.trySend(Result.failure(Exception("Acknowledge failed")))
                    }
                }
            } else {
                purchaseEvent.trySend(Result.success(true))
            }
        } else {
            purchaseEvent.trySend(Result.success(false))
        }
    }

    private val billingClientStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            isConnected = result.responseCode == BillingClient.BillingResponseCode.OK
            if (isConnected) {
                queryPurchases()
            }
        }

        override fun onBillingServiceDisconnected() {
            isConnected = false
        }
    }

    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
        isConnected = false
    }
}