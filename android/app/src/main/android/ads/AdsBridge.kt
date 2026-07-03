package co.median.android.ads

import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

/**
 * Exposed to the WebView as window.NativeAds. Web JS:
 *
 *   NativeAds.showInterstitial()
 *   NativeAds.showRewarded("placement-id")   // fires window.NativeAds.onRewardEarned(type, amount)
 *   NativeAds.showAppOpen()
 */
class AdsBridge(
    private val activityProvider: () -> Activity?,
    private val webViewProvider: () -> WebView?,
) {
    @JavascriptInterface
    fun showInterstitial() {
        val a = activityProvider() ?: return
        a.runOnUiThread { Ads.get().showInterstitial(a, null) }
    }

    @JavascriptInterface
    fun showRewarded(placement: String?) {
        val a = activityProvider() ?: return
        a.runOnUiThread {
            Ads.get().showRewarded(a, placement, { type, amount ->
                val wv = webViewProvider() ?: return@showRewarded
                val payload = JSONObject().put("type", type).put("amount", amount).toString()
                a.runOnUiThread {
                    wv.evaluateJavascript(
                        "window.NativeAds && window.NativeAds.onRewardEarned && window.NativeAds.onRewardEarned($payload);",
                        null,
                    )
                }
            })
        }
    }

    @JavascriptInterface
    fun showAppOpen() {
        val a = activityProvider() ?: return
        a.runOnUiThread { Ads.get().showAppOpen(a) }
    }

    companion object {
        /** JS shim injected into every page so web code has a stable API even before native fires. */
        const val INJECTED_JS = """
        (function(){
          if (window.NativeAds && window.NativeAds.__native) return;
          var b = window.__AdsBridge;
          window.NativeAds = {
            __native: !!b,
            showInterstitial: function(){ if(b) b.showInterstitial(); },
            showRewarded: function(p){ if(b) b.showRewarded(p||''); },
            showAppOpen: function(){ if(b) b.showAppOpen(); },
            onRewardEarned: null
          };
        })();
        """
    }
}