package com.androdevcafe.simbadchecker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.androdevcafe.simbadchecker.model.PurchasableItem
import com.android.billingclient.api.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_info.*

// App name to remove from SKU Titles
const val APP_NAME = "SimBad Checker"

class InfoActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    private val skuList = mutableListOf("donate")

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {

        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            Snackbar.make(root, R.string.thanks_for_donation, Snackbar.LENGTH_SHORT)
                .show()
            donate.isEnabled = false
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            Snackbar.make(root, R.string.cancel_donation, Snackbar.LENGTH_SHORT)
                .show()
        } else {
            Snackbar.make(root, R.string.error_donation, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        showLoading()
        billingClient = BillingClient.newBuilder(this).setListener(this).build()
        startConnection()
    }

    private fun showLoading() {
        donate.isEnabled = false
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                // Retrieve the list of items already bought
                val purchasesResponse = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.

                    // Create the params to query the items details
                    val params = SkuDetailsParams.newBuilder()
                    // We have only in app items (not subscriptions)
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

                    billingClient.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                        if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null && skuDetailsList.size == 1) {

                            // Check if item is already bought
                            val purchaseList =
                                if (purchasesResponse != null && purchasesResponse.purchasesList != null) {
                                    purchasesResponse.purchasesList
                                } else {
                                    emptyList()
                                }

                            // Update button only if the user didn't buy the object yet
                            if (purchaseList.isEmpty()) {
                                // Map each sku to our model
                                val skuDetails = skuDetailsList[0]
                                val title = skuDetails.title.replace(APP_NAME, "")
                                val purchasableItem =
                                    PurchasableItem(skuDetails.sku, title, skuDetails.description, skuDetails.price)
                                donate.setOnClickListener { startBilling(purchasableItem) }
                                donate.isEnabled = true
                            }
                        } else {
                            Snackbar.make(root, R.string.error_loading_store, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.retry) { startConnection() }
                                .show()
                        }
                    }
                } else {
                    Snackbar.make(root, R.string.error_loading_store, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.retry) { startConnection() }
                        .show()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Snackbar.make(root, R.string.error_loading_store, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.retry) { startConnection() }
                    .show()
            }
        })
    }

    private fun startBilling(purchasableItem: PurchasableItem) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSku(purchasableItem.id)
            // All our items are in app items (no subscriptions)
            .setType(BillingClient.SkuType.INAPP)
            .build()

        // Don't handle the return code, we handle it in the callback
        billingClient.launchBillingFlow(this, flowParams)
    }
}
