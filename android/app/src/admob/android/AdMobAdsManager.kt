package co.median.android.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobAdsManager : AdsManager {
    private val tag = "AdMobAdsManager"
    private var app: Application? = null
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var appOpen: AppOpenAd? = null

    override fun init(app: Application) {
        this.app = app
        MobileAds.initialize(app) {
            Log.i(tag, "AdMob initialised")
            preload()
        }
    }

    override fun preload() {
        loadInterstitial(); loadRewarded(); loadAppOpen()
    }

    private fun loadInterstitial() {
        val ctx = app ?: return
        val id = AdsConfigHolder.config.admobInterstitial.ifBlank { TEST_INTERSTITIAL }
        InterstitialAd.load(ctx, id, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null; Log.w(tag, "interstitial load failed: $e") }
        })
    }

    private fun loadRewarded() {
        val ctx = app ?: return
        val id = AdsConfigHolder.config.admobRewarded.ifBlank { TEST_REWARDED }
        RewardedAd.load(ctx, id, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null; Log.w(tag, "rewarded load failed: $e") }
        })
    }

    private fun loadAppOpen() {
        val ctx = app ?: return
        val id = AdsConfigHolder.config.admobAppOpen.ifBlank { TEST_APP_OPEN }
        AppOpenAd.load(ctx, id, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { appOpen = null; Log.w(tag, "appOpen load failed: $e") }
        })
    }

    override fun showInterstitial(activity: Activity, onClosed: (() -> Unit)?) {
        val ad = interstitial
        if (ad == null) { loadInterstitial(); onClosed?.invoke(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial(); onClosed?.invoke() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null; loadInterstitial(); onClosed?.invoke() }
        }
        ad.show(activity)
    }

    override fun showRewarded(activity: Activity, placement: String?, onReward: (String, Int) -> Unit, onClosed: (() -> Unit)?) {
        val ad = rewarded
        if (ad == null) { loadRewarded(); onClosed?.invoke(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; loadRewarded(); onClosed?.invoke() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rewarded = null; loadRewarded(); onClosed?.invoke() }
        }
        ad.show(activity) { reward -> onReward(reward.type ?: "", reward.amount) }
    }

    override fun showAppOpen(activity: Activity) {
        val ad = appOpen ?: run { loadAppOpen(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { appOpen = null; loadAppOpen() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { appOpen = null; loadAppOpen() }
        }
        ad.show(activity)
    }

    companion object {
        // Google-provided test unit IDs (safe default).
        const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    }
}