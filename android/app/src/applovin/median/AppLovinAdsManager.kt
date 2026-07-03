package co.median.android.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration

class AppLovinAdsManager : AdsManager {
    private val tag = "AppLovinAdsManager"
    private var app: Application? = null
    private var interstitial: MaxInterstitialAd? = null
    private var rewarded: MaxRewardedAd? = null
    private var appOpen: MaxAppOpenAd? = null
    private var pendingReward: ((String, Int) -> Unit)? = null
    private var pendingClosed: (() -> Unit)? = null

    override fun init(app: Application) {
        this.app = app
        val cfg = AdsConfigHolder.config
        val key = cfg.applovinSdkKey.ifBlank {
            Log.w(tag, "AppLovin SDK key not set in appConfig.json → services.ads.applovin.sdkKey"); return
        }
        val initConfig = AppLovinSdkInitializationConfiguration.builder(key, app).build()
        AppLovinSdk.getInstance(app).initialize(initConfig) {
            Log.i(tag, "AppLovin initialised")
            preload()
        }
    }

    override fun preload() {
        loadInterstitial(); loadRewarded(); loadAppOpen()
    }

    private fun loadInterstitial() {
        val a = app ?: return
        val id = AdsConfigHolder.config.applovinInterstitial
        if (id.isBlank()) return
        val ad = MaxInterstitialAd(id, a)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {}
            override fun onAdDisplayed(ad: MaxAd) {}
            override fun onAdHidden(ad: MaxAd) { pendingClosed?.invoke(); pendingClosed = null; interstitial?.loadAd() }
            override fun onAdClicked(ad: MaxAd) {}
            override fun onAdLoadFailed(id: String, e: MaxError) {}
            override fun onAdDisplayFailed(ad: MaxAd, e: MaxError) { pendingClosed?.invoke(); pendingClosed = null }
        })
        interstitial = ad
        ad.loadAd()
    }

    private fun loadRewarded() {
        val a = app ?: return
        val id = AdsConfigHolder.config.applovinRewarded
        if (id.isBlank()) return
        val ad = MaxRewardedAd.getInstance(id, a)
        ad.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd) {}
            override fun onAdDisplayed(ad: MaxAd) {}
            override fun onAdHidden(ad: MaxAd) { pendingClosed?.invoke(); pendingClosed = null; pendingReward = null; rewarded?.loadAd() }
            override fun onAdClicked(ad: MaxAd) {}
            override fun onAdLoadFailed(id: String, e: MaxError) {}
            override fun onAdDisplayFailed(ad: MaxAd, e: MaxError) { pendingClosed?.invoke(); pendingClosed = null; pendingReward = null }
            override fun onUserRewarded(ad: MaxAd, reward: MaxReward) { pendingReward?.invoke(reward.label ?: "", reward.amount) }
        })
        rewarded = ad
        ad.loadAd()
    }

    private fun loadAppOpen() {
        val a = app ?: return
        val id = AdsConfigHolder.config.applovinAppOpen
        if (id.isBlank()) return
        val ad = MaxAppOpenAd(id, a)
        ad.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {}
            override fun onAdDisplayed(ad: MaxAd) {}
            override fun onAdHidden(ad: MaxAd) { appOpen?.loadAd() }
            override fun onAdClicked(ad: MaxAd) {}
            override fun onAdLoadFailed(id: String, e: MaxError) {}
            override fun onAdDisplayFailed(ad: MaxAd, e: MaxError) {}
        })
        appOpen = ad
        ad.loadAd()
    }

    override fun showInterstitial(activity: Activity, onClosed: (() -> Unit)?) {
        val ad = interstitial ?: return onClosed?.invoke() ?: Unit
        if (ad.isReady) { pendingClosed = onClosed; ad.showAd(activity) } else { ad.loadAd(); onClosed?.invoke() }
    }

    override fun showRewarded(activity: Activity, placement: String?, onReward: (String, Int) -> Unit, onClosed: (() -> Unit)?) {
        val ad = rewarded ?: return onClosed?.invoke() ?: Unit
        if (ad.isReady) { pendingReward = onReward; pendingClosed = onClosed; ad.showAd(activity, placement) } else { ad.loadAd(); onClosed?.invoke() }
    }

    override fun showAppOpen(activity: Activity) {
        val ad = appOpen ?: return
        if (ad.isReady) ad.showAd(activity) else ad.loadAd()
    }
}