#if AD_NETWORK_ADMOB
import Foundation
import UIKit
import GoogleMobileAds

final class AdMobAdsManager: NSObject, AdsManaging, FullScreenContentDelegate {
    private var config = AdConfig(dict: [:])
    private var interstitial: InterstitialAd?
    private var rewarded: RewardedAd?
    private var appOpen: AppOpenAd?
    private var rewardCallback: ((String, Int) -> Void)?

    // Google-provided test unit IDs.
    private let testInterstitial = "ca-app-pub-3940256099942544/4411468910"
    private let testRewarded = "ca-app-pub-3940256099942544/1712485313"
    private let testAppOpen = "ca-app-pub-3940256099942544/5575463023"

    func start(with config: AdConfig) {
        self.config = config
        MobileAds.shared.start { _ in
            self.loadInterstitial(); self.loadRewarded(); self.loadAppOpen()
        }
    }

    private func iid(_ v: String, _ fallback: String) -> String { v.isEmpty ? fallback : v }

    private func loadInterstitial() {
        InterstitialAd.load(with: iid(config.admobInterstitial, testInterstitial), request: Request()) { ad, err in
            if let err = err { NSLog("[AdMob] interstitial load err: \(err)"); return }
            ad?.fullScreenContentDelegate = self; self.interstitial = ad
        }
    }

    private func loadRewarded() {
        RewardedAd.load(with: iid(config.admobRewarded, testRewarded), request: Request()) { ad, err in
            if let err = err { NSLog("[AdMob] rewarded load err: \(err)"); return }
            ad?.fullScreenContentDelegate = self; self.rewarded = ad
        }
    }

    private func loadAppOpen() {
        AppOpenAd.load(with: iid(config.admobAppOpen, testAppOpen), request: Request()) { ad, err in
            if let err = err { NSLog("[AdMob] appOpen load err: \(err)"); return }
            ad?.fullScreenContentDelegate = self; self.appOpen = ad
        }
    }

    func showInterstitial(from vc: UIViewController) {
        if let ad = interstitial { ad.present(from: vc) } else { loadInterstitial() }
    }

    func showRewarded(from vc: UIViewController, placement: String?, reward: @escaping (String, Int) -> Void) {
        guard let ad = rewarded else { loadRewarded(); return }
        rewardCallback = reward
        ad.present(from: vc) {
            let r = ad.adReward
            reward(r.type, r.amount.intValue)
        }
    }

    func showAppOpen(from vc: UIViewController) {
        if let ad = appOpen { ad.present(from: vc) } else { loadAppOpen() }
    }

    // Reload after dismissal.
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        if ad is InterstitialAd { interstitial = nil; loadInterstitial() }
        if ad is RewardedAd     { rewarded = nil;     loadRewarded() }
        if ad is AppOpenAd      { appOpen = nil;      loadAppOpen() }
    }
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        NSLog("[AdMob] present failed: \(error)")
    }
}
#endif