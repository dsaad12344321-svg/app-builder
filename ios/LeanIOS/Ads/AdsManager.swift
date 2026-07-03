import Foundation
import UIKit

/// Public interface used from Objective-C via the generated `GonativeIO-Swift.h` header.
@objc public protocol AdsManaging {
    @objc func start(with config: AdConfig)
    @objc func showInterstitial(from vc: UIViewController)
    @objc func showRewarded(from vc: UIViewController, placement: String?, reward: @escaping (String, Int) -> Void)
    @objc func showAppOpen(from vc: UIViewController)
}

@objc public class AdConfig: NSObject {
    @objc public let admobAppId: String
    @objc public let admobInterstitial: String
    @objc public let admobRewarded: String
    @objc public let admobAppOpen: String
    @objc public let applovinSdkKey: String
    @objc public let applovinInterstitial: String
    @objc public let applovinRewarded: String
    @objc public let applovinAppOpen: String

    @objc public init(dict: [String: Any]) {
        let ads = (dict["services"] as? [String: Any])?["ads"] as? [String: Any] ?? [:]
        let am = ads["admob"] as? [String: Any] ?? [:]
        let al = ads["applovin"] as? [String: Any] ?? [:]
        func s(_ d: [String: Any], _ k: String) -> String { (d[k] as? String) ?? "" }
        admobAppId = s(am, "appId")
        admobInterstitial = s(am, "interstitial")
        admobRewarded = s(am, "rewarded")
        admobAppOpen = s(am, "appOpen")
        applovinSdkKey = s(al, "sdkKey")
        applovinInterstitial = s(al, "interstitial")
        applovinRewarded = s(al, "rewarded")
        applovinAppOpen = s(al, "appOpen")
    }
}

@objc public class AdsHub: NSObject {
    @objc public static let shared = AdsHub()
    @objc public var manager: AdsManaging?

    @objc public func bootstrap(configDict: [String: Any]) {
        let cfg = AdConfig(dict: configDict)
        #if AD_NETWORK_ADMOB
        let m: AdsManaging = AdMobAdsManager()
        #elseif AD_NETWORK_APPLOVIN
        let m: AdsManaging = AppLovinAdsManager()
        #else
        let m: AdsManaging = NoopAdsManager()
        #endif
        m.start(with: cfg)
        self.manager = m
    }
}

final class NoopAdsManager: NSObject, AdsManaging {
    func start(with config: AdConfig) {}
    func showInterstitial(from vc: UIViewController) {}
    func showRewarded(from vc: UIViewController, placement: String?, reward: @escaping (String, Int) -> Void) {}
    func showAppOpen(from vc: UIViewController) {}
}