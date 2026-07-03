import Foundation
import WebKit

/// Registered on every WKWebView as `nativeAds`. Web JS calls:
///   window.webkit.messageHandlers.nativeAds.postMessage({action:"showInterstitial"});
///   window.webkit.messageHandlers.nativeAds.postMessage({action:"showRewarded", placement:"foo"});
///   window.webkit.messageHandlers.nativeAds.postMessage({action:"showAppOpen"});
@objc public class AdsBridge: NSObject, WKScriptMessageHandler {
    @objc public static let handlerName = "nativeAds"
    @objc public static let injectedJS = """
    (function(){
      if (window.NativeAds && window.NativeAds.__native) return;
      var h = window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.nativeAds;
      function post(a, extra){ if(!h) return; var p={action:a}; if(extra) for(var k in extra) p[k]=extra[k]; h.postMessage(p); }
      window.NativeAds = {
        __native: !!h,
        showInterstitial: function(){ post('showInterstitial'); },
        showRewarded: function(placement){ post('showRewarded', {placement: placement||''}); },
        showAppOpen: function(){ post('showAppOpen'); },
        onRewardEarned: null
      };
    })();
    """

    private weak var presenter: UIViewController?
    private weak var webView: WKWebView?

    @objc public init(presenter: UIViewController, webView: WKWebView) {
        self.presenter = presenter
        self.webView = webView
    }

    public func userContentController(_ ucc: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any],
              let action = body["action"] as? String,
              let vc = presenter,
              let mgr = AdsHub.shared.manager else { return }
        switch action {
        case "showInterstitial": mgr.showInterstitial(from: vc)
        case "showRewarded":
            let placement = body["placement"] as? String
            mgr.showRewarded(from: vc, placement: placement) { [weak self] type, amount in
                let payload = "{\"type\":\"\(type)\",\"amount\":\(amount)}"
                self?.webView?.evaluateJavaScript(
                    "window.NativeAds && window.NativeAds.onRewardEarned && window.NativeAds.onRewardEarned(\(payload));"
                )
            }
        case "showAppOpen": mgr.showAppOpen(from: vc)
        default: break
        }
    }
}