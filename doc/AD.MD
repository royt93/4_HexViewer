# 📊 AD SYSTEM — HEXVIEWER

> **SDK:** `com.roy.sdkadbmob` (AdmobWrapper) | **JitPack:** `com.github.royt93:AdmobWrapper:1.1.1` | **Provider:** AppLovin MAX | **minSdk:** 24 | **Cập nhật:** 2026-03-25 | **Build:** ✅ SUCCESSFUL

---

## 1. CẤU HÌNH AD IDs

| Loại | Debug | Release |
|---|---|---|
| **Provider** | AppLovin MAX | AppLovin MAX |
| **IS_ENABLE_ADMOB** | `false` | `false` |
| **AdMob App ID** (Manifest) | `ca-app-pub-3612191981543807~2249113565` | (same) |
| **AppLovin SDK Key** | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` | (same) |
| **AppLovin Banner ID** | `aa53ac5866596140` | (same) |
| **AppLovin Interstitial ID** | `580476ed6f6e6d5b` | (same) |
| **AppLovin App Open ID** | `2d9943cb1d30a275` | (same) |
| **AppLovin Reward ID** | `18d3030a0f4eec4b` | *(không dùng)* |

AdMob IDs vẫn giữ trong `buildConfigFields` (reserve cho trường hợp switch về AdMob sau):

| Loại | Debug (test) | Release |
|---|---|---|
| AdMob Banner | `ca-app-pub-3940256099942544/6300978111` | `ca-app-pub-3612191981543807/5282471019` |
| AdMob Interstitial | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3612191981543807/2777620817` |
| AdMob App Open | `ca-app-pub-3940256099942544/9257395921` | `ca-app-pub-3612191981543807/4547973843` |

---

## 2. TOUCHPOINTS ĐẦY ĐỦ (6 ĐIỂM)

| # | Touchpoint | Ad Type | File | Method | Trạng thái |
|---|---|---|---|---|---|
| 1 | Splash Screen | App Open | `SplashActivity.java` | `checkShowAd()` | ✅ |
| 2 | Resume từ Background | App Open | `MyApplication.java` | `registerAppOpenAdLifecycle()` | ✅ |
| 3 | ActMain | Banner | `ActMain.java` | `setupViews()` | ✅ |
| 4 | ActRecentlyOpen | Banner | `ActRecentlyOpen.java` | `setupViews()` | ✅ |
| 5 | Nút "Recently Open" | Interstitial | `ActMain.java` | `onPopupItemClick(actionRecentlyOpen)` | ✅ |
| 6 | Nút "Settings" | Interstitial | `ActMain.java` | `onPopupItemClick(actionSettings)` | ✅ |

> ⚠️ **Không có Reward Ad** trong app — không tích hợp mới.

---

## 3. LUỒNG HOẠT ĐỘNG

### App Open — Splash Screen (Touchpoint #1)
```
SplashActivity.onCreate()
  └─ checkShowAd()
       └─ AdManager.initSplashScreen(activity, onAdLoaded)
            ├─ Chờ EventBus (SDK init xong)
            ├─ loadAppOpenAd() → show AppOpenAd
            ├─ Timeout 8s tối đa → force navigate nếu ad chưa load
            └─ onAdDismiss → goToMain() → preload App Open kế tiếp
```

### App Open — Resume từ Background (Touchpoint #2)
```
MyApplication.setupAd()
  └─ AdManager.setConfig(adConfig)   // Main Thread — trước mọi thứ
  └─ AdManager.earlyInit(this)        // Main Thread — khởi AdSafety session clock sớm
  └─ AppLovinSdkInitializationConfiguration.builder(APPLOVIN_SDK_KEY)
       .setMediationProvider(MAX).build()
  └─ AppLovinSdk.getInstance(this).initialize(initConfig) {
       └─ AdManager.init(this, adConfig) { success, gaid ->
            if (success):
              Handler(Looper.getMainLooper()).post {  // PHẢI Main Thread!
                AdManager.registerAppOpenAdLifecycle(application)
                  └─ ProcessLifecycleOwner.addObserver(...)
                       ├─ onStop()  → recordAppWentBackground() [AdSafety timestamp]
                       └─ onStart() → canShowAppOpenOnResume()?
                            ├─ skip: cold start / resume < 5s / SplashActivity
                            ├─ ad ready → showAppOpenAd() → preload sau dismiss
                            └─ no ad → loadAppOpenAd() (save for next resume)
              }
       }
  }
```

### Banner (Touchpoints #3, #4)
```
Activity.onCreate() → setupViews()
  └─ AdManager.loadBanner(this, bannerContainer, tvLabelAd, AdSize.BANNER)
       ├─ AppLovin: tạo MaxAdView → loadAd() → auto show/hide container
       └─ Lifecycle:
            ├─ onResume()  → AdManager.bannerResume(adView)  → startAutoRefresh()
            ├─ onPause()   → AdManager.bannerPause(adView)   → stopAutoRefresh()
            └─ onDestroy() → AdManager.bannerDestroy(adView) → null listener → destroy()
```

### Interstitial (Touchpoints #5, #6)
```
ActMain.setupViews()
  └─ AdManager.loadInterstitial(this) → MaxInterstitialAd.loadAd()

User bấm nút → onPopupItemClick()
  └─ AdManager.showInterstitial(activity, adShown -> { action() })
       ├─ AdSafety: session/hourly/daily caps → skip nếu vượt
       ├─ loading overlay 1s → show MaxInterstitialAd
       ├─ onAdHidden → callback adShown=true → start activity
       ├─ not ready  → callback adShown=false → start activity ngay
       └─ auto-preload sau dismiss
```

---

## 4. MEMORY LEAK ANALYSIS ✅

| Source | Potential Leak | Cách xử lý |
|---|---|---|
| `adView` Banner | Activity context nếu không destroy | `bannerDestroy()` null listener trước `destroy()`, `adView = null` ✅ |
| Interstitial listener | Orphaned callback | `maxInterstitialAd.setListener(null)` + `destroy()` trước khi tạo mới ✅ |
| App Open Activity ref | `currentActivity` giữ strong ref | SDK dùng `WeakReference<Activity>` + `registerActivityLifecycleCallbacks` tự update ✅ |
| `splashScope` coroutine | Coroutine leak nếu Activity destroy | SDK dùng `WeakReference(activity)` + check `act.isFinishing` ✅ |
| `splashTimeoutHandler` | Handler leak | `splashTimeoutHandler.removeCallbacksAndMessages(null)` khi điều hướng ✅ |
| Loading overlay dialog | Dialog leak nếu Activity destroy | `WeakReference(activity)` + check `isFinishing` trước show ✅ |
| `ProcessLifecycleOwner` | Không bao giờ leak | Application-level lifecycle, không bind vào Activity ✅ |
| MaxAppOpenAd | Listener leak khi reload | `setListener(null)` + `maxAppOpenAd = null` trước khi tạo mới ✅ |

---

## 5. FILES ĐÃ THAY ĐỔI

| File | Thay đổi |
|---|---|
| `settings.gradle` | Thêm `maven { url "https://jitpack.io" }` |
| `app/build.gradle` | AppLovin IDs + `IS_ENABLE_ADMOB=false` + `AdmobWrapper:1.1.1` + `minSdk 24` |
| `AndroidManifest.xml` | Thêm `applovin.sdk.key` meta-data |
| `MyApplication.java` | `setupAd()`: AppLovinSdk init → `AdManager.init()` → `registerAppOpenAdLifecycle()` |
| `SplashActivity.java` | `AdManager.initSplashScreen()` thay `AdMobManager.initSplashScreen()` |
| `ActMain.java` | Banner (4 lifecycle) + 2 Interstitial, xóa `InterstitialAdListener` |
| `ActRecentlyOpen.java` | Banner (4 lifecycle) |
| `sdkadbmob/UIUtils.kt` | Tái tạo sau khi `AdMobManager.kt` bị xóa (đã có 8 Activities dùng) |
| `proguard-rules.pro` | AppLovin + AdmobWrapper keep rules |
| `sdkadbmob/AdMobManager.kt` | **ĐÃ XÓA** ✅ |

---

## 6. BUILD STATUS

```
BUILD SUCCESSFUL in 9s
70 actionable tasks: 10 executed, 60 up-to-date
```

> ✅ Debug build OK. Release build: cần test ProGuard với `assembleProductionRelease`.

---

## 7. ADSAFETY (TỰ ĐỘNG — KHÔNG CẦN CODE THÊM)

| # | Guard | Mô tả |
|---|---|---|
| 1 | Session minimum | User phải ở app ≥ 10s trước impression đầu tiên |
| 2 | Session cap | Tối đa 6 fullscreen/lần mở |
| 3 | Hourly cap | Tối đa 3 fullscreen/giờ |
| 4 | Daily cap | Tối đa 5 fullscreen/ngày |
| 5 | Throttle | Cách nhau ≥ 60s giữa 2 fullscreen |
| 6 | CTR Anomaly | CTR > 10% → flag suspicious |
| 7 | Resume cooldown | App Open skip nếu resume < 5s hoặc cold start |
