package co.median.android.ads

object AdsFactory {
    fun create(): AdsManager = AdMobAdsManager()
}