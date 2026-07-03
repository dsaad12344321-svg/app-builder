package co.median.android.ads

import android.app.Activity
import android.app.Application

/**
 * Ad-network-agnostic contract. A concrete implementation lives in each
 * flavor source set (src/admob/ and src/applovin/).
 */
interface AdsManager {
    fun init(app: Application)
    fun preload()
    fun showInterstitial(activity: Activity, onClosed: (() -> Unit)? = null)
    fun showRewarded(activity: Activity, placement: String?, onReward: (String, Int) -> Unit, onClosed: (() -> Unit)? = null)
    fun showAppOpen(activity: Activity)
}

object Ads {
    @Volatile private var instance: AdsManager? = null

    fun get(): AdsManager = instance
        ?: error("AdsManager not initialised. Call Ads.init() from Application.onCreate().")

    fun init(app: Application, impl: AdsManager) {
        instance = impl
        impl.init(app)
    }
}

data class AdConfig(
    val admobAppId: String = "",
    val admobInterstitial: String = "",
    val admobRewarded: String = "",
    val admobAppOpen: String = "",
    val applovinSdkKey: String = "",
    val applovinInterstitial: String = "",
    val applovinRewarded: String = "",
    val applovinAppOpen: String = "",
) {
    companion object {
        fun fromAppConfigJson(json: org.json.JSONObject?): AdConfig {
            val ads = json?.optJSONObject("services")?.optJSONObject("ads") ?: return AdConfig()
            val am = ads.optJSONObject("admob") ?: org.json.JSONObject()
            val al = ads.optJSONObject("applovin") ?: org.json.JSONObject()
            return AdConfig(
                admobAppId = am.optString("appId"),
                admobInterstitial = am.optString("interstitial"),
                admobRewarded = am.optString("rewarded"),
                admobAppOpen = am.optString("appOpen"),
                applovinSdkKey = al.optString("sdkKey"),
                applovinInterstitial = al.optString("interstitial"),
                applovinRewarded = al.optString("rewarded"),
                applovinAppOpen = al.optString("appOpen"),
            )
        }
    }
}

internal object AdsConfigHolder {
    @Volatile var config: AdConfig = AdConfig()
}