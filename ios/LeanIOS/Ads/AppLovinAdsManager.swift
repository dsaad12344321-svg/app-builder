#if AD_NETWORK_APPLOVIN
import Foundation
import UIKit
import AppLovinSDK

final class AppLovinAdsManager: NSObject, AdsManaging, MAAdDelegate, MARewardedAdDelegate {
    private var config = AdConfig(dict: [:])
    private var interstitial: MAInterstitialAd?
    private var rewarded: MARewardedAd?
    private var appOpen: MAAppOpenAd?
    private var rewardCallback: ((String, Int) -> Void)?

    func start(with config: AdConfig) {
        self.config = config
        guard !config.applovinSdkKey.isEmpty else {
            NSLog("[AppLovin] SDK key missing in appConfig.json services.ads.applovin.sdkKey"); return
        }
        let initConfig = ALSdkInitializationConfiguration(sdkKey: config.applovinSdkKey) { _ in }
        ALSdk.shared().initialize(with: initConfig) { _ in
            self.setupInterstitial(); self.setupRewarded(); self.setupAppOpen()
        }
    }

    private func setupInterstitial() {
        guard !config.applovinInterstitial.isEmpty else { return }
        let a = MAInterstitialAd(adUnitIdentifier: config.applovinInterstitial)
        a.delegate = self; interstitial = a; a.load()
    }
    private func setupRewarded() {
        guard !config.applovinRewarded.isEmpty else { return }
        let a = MARewardedAd.shared(withAdUnitIdentifier: config.applovinRewarded)
        a.delegate = self; rewarded = a; a.load()
    }
    private func setupAppOpen() {
        guard !config.applovinAppOpen.isEmpty else { return }
        let a = MAAppOpenAd(adUnitIdentifier: config.applovinAppOpen)
        a.delegate = self; appOpen = a; a.load()
    }

    func showInterstitial(from vc: UIViewController) {
        if let a = interstitial, a.isReady { a.show() } else { interstitial?.load() }
    }
    func showRewarded(from vc: UIViewController, placement: String?, reward: @escaping (String, Int) -> Void) {
        rewardCallback = reward
        if let a = rewarded, a.isReady { a.show(forPlacement: placement) } else { rewarded?.load() }
    }
    func showAppOpen(from vc: UIViewController) {
        if let a = appOpen, a.isReady { a.show() } else { appOpen?.load() }
    }

    // MARK: - MAAdDelegate
    func didLoad(_ ad: MAAd) {}
    func didFailToLoadAd(forAdUnitIdentifier id: String, withError error: MAError) {}
    func didDisplay(_ ad: MAAd) {}
    func didHide(_ ad: MAAd) {
        // Reload the same slot.
        switch ad.format {
        case .interstitial: interstitial?.load()
        case .rewarded:     rewarded?.load()
        case .appOpen:      appOpen?.load()
        default: break
        }
    }
    func didClick(_ ad: MAAd) {}
    func didFail(toDisplay ad: MAAd, withError error: MAError) {}

    // MARK: - MARewardedAdDelegate
    func didRewardUser(for ad: MAAd, with reward: MAReward) {
        rewardCallback?(reward.label ?? "", reward.amount)
    }
}
#endif