package com.example.app.ads

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdSize
import com.yandex.mobile.ads.common.AdUnitId
import com.yandex.mobile.ads.common.ErrorCode

class YandexBannerAd(
    private val context: Context,
    private val container: FrameLayout,
    private val adUnitId: String = "R-M-2438180-1"
) {
    private var bannerAdView: BannerAdView? = null

    fun load() {
        if (bannerAdView != null) return

        bannerAdView = BannerAdView(context).apply {
            setAdUnitId(adUnitId)
            setAdSize(AdSize.flexibleSize(320, 50))
            setBannerAdEventListener(bannerAdEventListener)
        }

        container.removeAllViews()
        container.addView(bannerAdView)

        val adRequest = AdRequest.Builder().build()
        bannerAdView?.loadAd(adRequest)
    }

    fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
    }

    private val bannerAdEventListener = object : BannerAdEventListener {
        override fun onAdLoaded() {}
        override fun onAdFailedToLoad(errorCode: ErrorCode) {}
        override fun onAdClicked() {}
        override fun onLeftApplication() {}
        override fun onReturnedToApplication() {}
        override fun onImpression( impressionData: com.yandex.mobile.ads.common.ImpressionData?) {}
    }
}

class YandexInterstitialAd(
    private val context: Context,
    private val adUnitId: String = "R-M-2438180-2"
) {
    private var interstitialAd: com.yandex.mobile.ads.interstitial.InterstitialAd? = null
    private var isLoading = false

    fun load(onLoaded: () -> Unit = {}) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        com.yandex.mobile.ads.interstitial.InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener {
                override fun onAdLoaded(ad: com.yandex.mobile.ads.interstitial.InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    onLoaded()
                }

                override fun onAdFailedToLoad(error: com.yandex.mobile.ads.common.AdRequestError) {
                    isLoading = false
                }
            }
        )
    }

    fun show() {
        interstitialAd?.show()
        interstitialAd = null
    }

    fun destroy() {
        interstitialAd?.destroy()
        interstitialAd = null
    }
}