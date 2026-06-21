# AD SDK TEST CASES — HexViewer

> **SDK:** `com.github.royt93:AdmobApplovinWrapper:1.1.5` | **Provider:** AppLovin MAX (Fallback: AdMob) | **Cập nhật:** 2026-06-21

---

## MỤC LỤC

1. [Ad SDK Initialization](#1-ad-sdk-initialization)
2. [Splash / App Open Ad](#2-splash--app-open-ad)
3. [Banner Ad](#3-banner-ad)
4. [Interstitial Ad](#4-interstitial-ad)
5. [Rewarded Ad](#5-rewarded-ad)
6. [VIP Feature](#6-vip-feature)
7. [Offline Scenarios](#7-offline-scenarios)
8. [Online Scenarios](#8-online-scenarios)
9. [Network Transition](#9-network-transition)
10. [Safety Limits](#10-safety-limits)

---

## LEGEND

| Ký hiệu | Nghĩa |
|---------|-------|
| ✅ Pass | Test đạt yêu cầu |
| ❌ Fail | Test thất bại cần fix |
| 🔵 Manual | Cần test thủ công trên device |
| 🤖 Auto | Unit/Instrumented test tự động |
| `[OFFLINE]` | Tắt WiFi + mobile data trước khi test |
| `[ONLINE]` | Bật mạng bình thường |

---

## 1. AD SDK INITIALIZATION

**File liên quan:** `MyApplication.java` | **Class:** `AdSdkConfig`, `AdManager`

### TC-INIT-001 — AppLovin mode (IS_ENABLE_ADMOB=false) 🤖
```
Arrange: Tạo AdSdkConfig với isEnableAdmob=false, applovinSdkKey hợp lệ
Act:     AdManager.setConfig(config)
Assert:  AdManager.adConfig.isEnableAdmob == false
         AdManager.adConfig.applovinSdkKey không empty
```

### TC-INIT-002 — AdMob fallback mode (IS_ENABLE_ADMOB=true) 🤖
```
Arrange: Tạo AdSdkConfig với isEnableAdmob=true, admobAppId hợp lệ
Act:     AdManager.setConfig(config)
Assert:  AdManager.adConfig.isEnableAdmob == true
```

### TC-INIT-003 — Initialize callback trả về success=true [ONLINE] 🔵
```
Arrange: Config hợp lệ, device có mạng
Act:     AdManager.INSTANCE.initialize(context, (success, gaid) -> ...)
Assert:  callback nhận success=true
         gaid không null (GAID được lấy thành công)
```

### TC-INIT-004 — Initialize khi offline — không crash [OFFLINE] 🔵
```
Arrange: Tắt tất cả mạng, config hợp lệ
Act:     AdManager.INSTANCE.initialize(context, callback)
Assert:  Không crash, callback vẫn được gọi
         App chạy bình thường (không block UI)
```

### TC-INIT-005 — Debug mode dùng test ad unit IDs 🤖
```
Arrange: BuildConfig.DEBUG = true
Act:     Build devDebug variant
Assert:  ADMOB_BANNER_ID == "ca-app-pub-3940256099942544/6300978111" (test ID)
         IS_ENABLE_ADMOB == false (dùng AppLovin)
```

---

## 2. SPLASH / APP OPEN AD

**File liên quan:** `SplashActivity.java` | **Method:** `checkShowAd()`, `runSplashAdFlow()`

### TC-SPLASH-001 — Offline: bỏ qua consent, đi thẳng vào Main [OFFLINE] 🔵
```
Arrange: Tắt WiFi và Mobile Data hoàn toàn
Act:     Mở app từ launcher
Assert:  SplashActivity KHÔNG gọi requestConsentInfoUpdate()
         Chuyển sang ActMain ngay lập tức (không delay consent)
         Không có dialog consent xuất hiện
         Không có App Open ad hiển thị
```

### TC-SPLASH-002 — Online: consent flow chạy bình thường [ONLINE] 🔵
```
Arrange: Bật mạng
Act:     Mở app lần đầu (chưa có consent)
Assert:  Dialog consent xuất hiện
         Sau khi user chọn → chuyển sang ActMain
```

### TC-SPLASH-003 — Online: App Open ad hiển thị sau consent [ONLINE] 🔵
```
Arrange: Bật mạng, đã có consent trước (canRequestAds=true)
Act:     Mở app
Assert:  initSplashScreen() được gọi
         App Open ad hiển thị trước khi vào ActMain
         Sau khi ad đóng → vào ActMain
```

### TC-SPLASH-004 — Timeout guard 5 giây [ONLINE] 🔵
```
Arrange: Simulate consent callback bị stall (không gọi về)
Act:     Chờ 5 giây
Assert:  consentTimeoutRunnable kích hoạt
         goToMain() được gọi — app không bị treo ở splash
```

### TC-SPLASH-005 — hasNavigated guard chống double-navigate 🔵
```
Arrange: Trigger goToMain() hai lần liên tiếp (race condition)
Act:     Gọi goToMain() lần 2 khi lần 1 đã chạy
Assert:  ActMain chỉ start đúng 1 lần
         Không crash với "Activity already started"
```

### TC-SPLASH-006 — Activity bị destroy trong khi consent callback đang bay [ONLINE] 🔵
```
Arrange: Mở splash, nhấn back ngay lập tức
Act:     Consent callback gọi về sau khi isDestroyed()=true
Assert:  Không crash (null check guard hoạt động đúng)
         Không navigate sang ActMain
```

### TC-SPLASH-007 — onDestroy hủy tất cả animation và callback 🔵
```
Arrange: Mở splash activity
Act:     Gọi onDestroy (hoặc nhấn back)
Assert:  Tất cả ViewPropertyAnimator.cancel() được gọi
         consentTimeoutRunnable bị removeCallbacks
         finishRunnable bị removeCallbacks
```

---

## 3. BANNER AD

**File liên quan:** `ActMain.java:734`, `ActRecentlyOpen.java` | **Layout:** `layout_ad_banner.xml`

### TC-BANNER-001 — Banner hiển thị khi user không có VIP [ONLINE] 🔵
```
Arrange: User chưa kích hoạt VIP
Act:     Mở ActMain
Assert:  bannerContainer visible (View.VISIBLE)
         Banner ad load và hiển thị trong vài giây
```

### TC-BANNER-002 — Banner ẩn khi user có VIP active 🔵
```
Arrange: Kích hoạt VIP bằng key 30 ngày
Act:     Mở ActMain
Assert:  bannerContainer GONE/INVISIBLE
         AdManager.INSTANCE.isVipByKeyActive() == true
```

### TC-BANNER-003 — Banner ẩn/hiện khi VIP hết hạn 🔵
```
Arrange: Kích hoạt VIP 3 ngày, sau đó simulate VIP hết hạn
Act:     Quay lại ActMain (onResume)
Assert:  refreshBannerState() được gọi
         Banner hiển thị trở lại sau khi VIP hết hạn
```

### TC-BANNER-004 — SDK tự manage lifecycle (không crash khi back/resume) 🔵
```
Arrange: Banner đang hiển thị trong ActMain
Act:     Nhấn Home → mở lại app (background/foreground)
Assert:  Không crash
         Banner tự pause/resume đúng
         Không cần gọi thủ công bannerPause()/bannerResume()
```

### TC-BANNER-005 — Banner trong ActRecentlyOpen [ONLINE] 🔵
```
Arrange: Mở ActRecentlyOpen
Act:     Quan sát bottom banner
Assert:  Banner load thành công
         VIP check được áp dụng tương tự ActMain
```

### TC-BANNER-006 — Offline: Banner load fail — không crash [OFFLINE] 🔵
```
Arrange: Tắt mạng, mở ActMain
Act:     SDK cố load banner nhưng không có network
Assert:  Không crash
         bannerContainer vẫn có thể visible (chờ retry của SDK)
         Không hiển thị error UI bất thường
```

---

## 4. INTERSTITIAL AD

**File liên quan:** `ActMain.java:132,138,534,546`

### TC-INTER-001 — Preload interstitial khi vào ActMain [ONLINE] 🔵
```
Arrange: Mở ActMain
Act:     setupViews() hoàn tất
Assert:  AdManager.INSTANCE.loadInterstitial(this) được gọi
         Ad được tải sẵn (preload) để giảm latency
```

### TC-INTER-002 — Hash Calculator button hiển thị interstitial [ONLINE] 🔵
```
Arrange: ActMain mở, interstitial đã preload
Act:     Tap "Hash Calculator" button
Assert:  showInterstitial() được gọi
         Interstitial ad hiển thị
         Sau khi đóng ad → ActHashCalculator được mở
```

### TC-INTER-003 — File Info button hiển thị interstitial [ONLINE] 🔵
```
Arrange: ActMain mở, interstitial đã preload
Act:     Tap "File Info Analyzer" button
Assert:  showInterstitial() được gọi
         ActFileInfo được mở sau khi ad đóng
```

### TC-INTER-004 — Recently Open button [ONLINE] 🔵
```
Arrange: ActMain mở
Act:     Tap "Recently Open" từ popup menu
Assert:  showInterstitial() gọi trước khi navigate
         ActRecentlyOpen mở sau ad
```

### TC-INTER-005 — Settings button [ONLINE] 🔵
```
Arrange: ActMain mở
Act:     Tap "Settings" từ popup
Assert:  showInterstitial() gọi
         Settings screen mở sau ad
```

### TC-INTER-006 — Offline: interstitial không load — navigate vẫn xảy ra [OFFLINE] 🔵
```
Arrange: Tắt mạng hoàn toàn
Act:     Tap "Hash Calculator"
Assert:  showInterstitial callback vẫn được gọi (adShown=false)
         ActHashCalculator vẫn mở (không block navigation)
         Không crash
```

### TC-INTER-007 — Callback adShown=false khi không có ad để show 🤖
```
Arrange: loadInterstitial chưa hoàn tất hoặc fail
Act:     showInterstitial(context, adShown -> ...)
Assert:  callback được gọi với adShown=false (không phải null)
         Code trong callback (navigate) vẫn chạy
```

---

## 5. REWARDED AD

**File liên quan:** `ActVipManagement.kt:164-177`

### TC-REWARD-001 — User xem đủ rewarded ad → nhận 3 ngày VIP [ONLINE] 🔵
```
Arrange: Mở ActVipManagement, rewarded ad đã load
Act:     Tap "Xem quảng cáo → 3 ngày VIP"
         Xem hết ad (earned=true)
Assert:  grantVipFromAd() được gọi
         VIP active 3 ngày trong SharedPreferences
         UI hiển thị VIP status
```

### TC-REWARD-002 — User tắt rewarded ad sớm → fallback interstitial [ONLINE] 🔵
```
Arrange: Rewarded ad đang chạy
Act:     User đóng ad trước khi hết (earned=false)
Assert:  Interstitial ad được show thay thế
         grantVipFromAd() CHỈ được gọi nếu interstitial hiển thị
```

### TC-REWARD-003 — Fallback interstitial fail (offline) → không grant VIP [OFFLINE] 🔵
```
Arrange: Rewarded ad earned=false, mạng off
Act:     Fallback showInterstitial() gọi
Assert:  Interstitial không show (shown=false)
         VIP KHÔNG được grant
         User thấy thông báo phù hợp (nếu có)
```

### TC-REWARD-004 — isFinishing check guard trong callback 🔵
```
Arrange: Activity đang finishing khi reward callback bay về
Act:     Reward callback gọi isFinishing=true
Assert:  Không crash
         Không gọi grantVipFromAd() hoặc showInterstitial()
```

### TC-REWARD-005 — Offline: nút xem ad bị disable hoặc báo lỗi [OFFLINE] 🔵
```
Arrange: Tắt mạng, mở ActVipManagement
Act:     Quan sát trạng thái nút "Xem quảng cáo"
Assert:  Nút disabled HOẶC khi tap hiển thị "No ads available"
         Không crash
```

---

## 6. VIP FEATURE

**File liên quan:** `VipKeys.kt`, `VipPrefs.kt`, `ActVipManagement.kt`

### TC-VIP-001 — Kích hoạt VIP bằng key 30 ngày 🤖
```
Arrange: AdManager.clearVipByKey() để reset
         config với vipKeySecret hợp lệ
Act:     AdManager.activateVipByKey(context, secret, 30)
Assert:  return true
         isVipByKeyActive() == true
         getVipByKeyExpiry() > currentTime
         remainingDays ≈ 30 (±0.01)
```

### TC-VIP-002 — Kích hoạt VIP bằng key 3 ngày 🤖
```
Arrange: VipKeys.VIP_3D_KEY hợp lệ
Act:     VipKeys.lookupDays(VIP_3D_KEY)
         AdManager.activateVipByKey(context, secret, days)
Assert:  days == 3
         isVipByKeyActive() == true
         remainingDays > 2.9 && remainingDays <= 3.0
```

### TC-VIP-003 — VIP bị clear 🤖
```
Arrange: VIP đang active
Act:     AdManager.clearVipByKey()
Assert:  isVipByKeyActive() == false
         Banner ad hiển thị trở lại
```

### TC-VIP-004 — VIP key sai → không kích hoạt 🤖
```
Arrange: Key không hợp lệ "wrong_key_123"
Act:     activateVipByKey(context, "wrong_key_123", 30)
Assert:  return false HOẶC isVipByKeyActive() == false
```

### TC-VIP-005 — VIP expiry check đúng sau khi app restart 🔵
```
Arrange: Kích hoạt VIP 3 ngày
Act:     Force kill app, mở lại
Assert:  VIP vẫn active (persist qua SharedPreferences)
         Expiry timestamp đúng
```

### TC-VIP-006 — UI Badge VIP hiển thị khi VIP active 🔵
```
Arrange: VIP đang active (30 ngày)
Act:     Mở ActMain, quan sát toolbar
Assert:  VIP badge container visible
         Label hiển thị đúng (số ngày còn lại hoặc "VIP")
         Icon VIP hiển thị
```

### TC-VIP-007 — UI Badge VIP ẩn khi không có VIP 🔵
```
Arrange: VIP không active hoặc đã hết hạn
Act:     Mở ActMain
Assert:  VIP badge container GONE
```

---

## 7. OFFLINE SCENARIOS

> Test tất cả với **Airplane Mode** hoặc tắt cả WiFi + Mobile Data.

### TC-OFF-001 — App launch khi offline — không crash, vào Main bình thường 🔵
```
Arrange: [OFFLINE] Tắt tất cả mạng
Act:     Mở app từ launcher
Assert:  SplashActivity load
         isNetworkAvailable() trả về false
         goToMain() gọi ngay (không chờ consent)
         ActMain mở thành công
         Không có dialog consent
         Không có App Open ad
```

### TC-OFF-002 — Banner không crash khi offline 🔵
```
Arrange: [OFFLINE] App đang ở ActMain
Act:     refreshBannerState() và loadBanner() chạy
Assert:  SDK xử lý gracefully (retry sau / không load)
         Không crash NullPointerException hay NetworkException
         bannerContainer vẫn hiện nếu user không VIP
```

### TC-OFF-003 — Tất cả interstitial triggers vẫn navigate khi offline 🔵
```
Arrange: [OFFLINE]
Act:     Tap lần lượt: Hash Calculator, File Info, Recently Open, Settings
Assert:  Mỗi destination vẫn mở được
         Không bị block bởi "chờ ad"
         adShown callback nhận false, navigation vẫn chạy
```

### TC-OFF-004 — Rewarded ad không crash khi offline 🔵
```
Arrange: [OFFLINE] Mở ActVipManagement
Act:     Tap nút xem rewarded ad
Assert:  Không crash
         Hiển thị thông báo lỗi phù hợp ("No internet" hoặc "No ad available")
         VIP không được grant
```

### TC-OFF-005 — VIP key activation vẫn hoạt động khi offline 🔵
```
Arrange: [OFFLINE]
Act:     Nhập VIP key 30 ngày vào VIP screen
Assert:  Key được verify locally (không cần network)
         VIP được kích hoạt thành công
         Banner ẩn đi
```

### TC-OFF-006 — App không ANR khi offline 🔵
```
Arrange: [OFFLINE] Mở app, dùng các tính năng trong 2 phút
Act:     Duyệt các màn hình, tắt/mở nhiều lần
Assert:  Không ANR (main thread không bị block > 5 giây)
         UI responsive
```

---

## 8. ONLINE SCENARIOS

### TC-ON-001 — Full ad flow online: Splash → Banner → Interstitial → Rewarded 🔵
```
Arrange: [ONLINE] Cài app fresh (chưa có consent)
Act:     1. Mở app → consent dialog
         2. Đồng ý consent
         3. App Open ad hiển thị → vào Main
         4. Banner load ở ActMain
         5. Tap Hash Calculator → Interstitial → ActHashCalculator
         6. Quay lại, vào VIP screen → tap xem ad → Rewarded
Assert:  Mỗi bước ad hiển thị đúng loại, đúng thứ tự
         Navigation đúng sau mỗi ad
         Không crash trong toàn bộ flow
```

### TC-ON-002 — Consent từ chối → không có ads, vào Main bình thường [ONLINE] 🔵
```
Arrange: [ONLINE] Fresh install, lần đầu mở app
Act:     Consent dialog → chọn từ chối / manage options → reject all
Assert:  canRequestAds = false
         goToMain() gọi thay vì runSplashAdFlow()
         Không có App Open ad
         App hoạt động bình thường (chỉ không có ads)
```

### TC-ON-003 — Consent đã đồng ý trước → App Open hiển thị ngay [ONLINE] 🔵
```
Arrange: [ONLINE] Consent đã có sẵn (canRequestAds=true) từ lần trước
Act:     Mở lại app
Assert:  Không có dialog consent lần 2
         App Open ad hiển thị trực tiếp
         Transition vào ActMain sau ad
```

### TC-ON-004 — AppLovin adapter load banner thành công [ONLINE] 🔵
```
Arrange: IS_ENABLE_ADMOB=false (AppLovin active)
Act:     loadBanner() trong ActMain
Assert:  Banner hiển thị với AppLovin ad
         Không fallback sang AdMob
```

### TC-ON-005 — VIP badge animation chạy đúng khi online [ONLINE] 🔵
```
Arrange: VIP active
Act:     onResume() trong ActMain
Assert:  startPillAnimation(mVipBadgeContainer) gọi
         Animation chạy mượt
         stopPillAnimation() gọi trong onPause()
```

---

## 9. NETWORK TRANSITION

### TC-NET-001 — Online → Offline trong khi banner đang hiển thị 🔵
```
Arrange: [ONLINE] Banner đang visible ở ActMain
Act:     Tắt WiFi/Data (online → offline)
Assert:  Banner không crash
         Ad hiện tại có thể biến mất (bình thường)
         Không ANR, không crash
```

### TC-NET-002 — Offline → Online: app vẫn dùng được, ad load lại 🔵
```
Arrange: [OFFLINE] Mở app, dùng bình thường
Act:     Bật mạng lại (offline → online)
         Minimize/maximize app hoặc chuyển màn hình
Assert:  SDK tự retry load banner và interstitial
         Sau vài giây banner hiển thị trở lại
         Không cần restart app
```

### TC-NET-003 — Offline → Online trong Splash (rare edge case) 🔵
```
Arrange: Bật airplane mode, mở app (isNetworkAvailable=false → goToMain ngay)
Act:     Ngay sau khi vào ActMain, bật mạng lại
Assert:  App không bị ảnh hưởng (consent flow đã skip)
         Banner sẽ load nếu SDK retry
```

---

## 10. SAFETY LIMITS

**Class:** `AdSafetyLimits`

### TC-SAFE-001 — Debug dùng TEST safety limits 🤖
```
Arrange: BuildConfig.DEBUG = true
Act:     AdSafetyLimits.TEST
Assert:  Impression limits cao hơn hoặc disabled (cho test)
         Frequency cap thấp hơn
```

### TC-SAFE-002 — Release dùng production safety limits 🤖
```
Arrange: BuildConfig.DEBUG = false
Act:     new AdSafetyLimits() (default constructor)
Assert:  Tham số giới hạn frequency cap đúng production
         Không dùng TEST config trong release build
```

### TC-SAFE-003 — Interstitial không spam quá giới hạn 🔵
```
Arrange: [ONLINE] Production safety limits
Act:     Tap các button nhanh liên tiếp 5-10 lần
Assert:  AdManager đếm và giới hạn số lần show
         Không show ad quá nhanh (min interval giữa 2 interstitial)
         Sau khi hết limit, callback vẫn gọi nhưng adShown=false
```

---

## SUMMARY TABLE

| ID | Loại Ad | Scenario | Online | Offline | Priority |
|----|---------|----------|--------|---------|----------|
| TC-SPLASH-001 | App Open | Skip consent khi offline | | ✓ | P0 |
| TC-SPLASH-002 | App Open | Consent flow normal | ✓ | | P0 |
| TC-SPLASH-003 | App Open | Ad hiển thị sau consent | ✓ | | P0 |
| TC-SPLASH-004 | App Open | Timeout guard 5s | ✓ | | P1 |
| TC-BANNER-001 | Banner | Hiển thị bình thường | ✓ | | P0 |
| TC-BANNER-002 | Banner | Ẩn khi VIP active | ✓ | | P0 |
| TC-BANNER-006 | Banner | Offline không crash | | ✓ | P0 |
| TC-INTER-002 | Inter | Hash Calculator | ✓ | | P0 |
| TC-INTER-006 | Inter | Navigate vẫn chạy khi offline | | ✓ | P0 |
| TC-REWARD-001 | Rewarded | Xem đủ → nhận VIP | ✓ | | P0 |
| TC-REWARD-002 | Rewarded | Skip → fallback inter | ✓ | | P1 |
| TC-REWARD-003 | Rewarded | Fallback fail offline | | ✓ | P1 |
| TC-VIP-001 | VIP | Key 30 ngày | Any | | P0 |
| TC-VIP-002 | VIP | Key 3 ngày | Any | | P0 |
| TC-VIP-005 | VIP | Persist sau restart | Any | | P1 |
| TC-OFF-001 | General | Launch offline | | ✓ | P0 |
| TC-OFF-003 | General | Tất cả navigation offline | | ✓ | P0 |
| TC-NET-002 | General | Offline → Online ad reload | ✓→ | →✓ | P2 |

---

## CÁCH CHẠY TEST TỰ ĐỘNG

```bash
# Chạy tất cả unit tests
./gradlew testDevDebugUnitTest

# Chỉ chạy Ad Integration tests
./gradlew testDevDebugUnitTest --tests "com.galaxyjoy.hexviewer.feature.vip.AdIntegrationTest"

# Chạy với coverage
./gradlew testDevDebugUnitTest jacocoTestReport
```

**Xem kết quả:**
```
app/build/reports/tests/testDevDebugUnitTest/index.html
app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

---

## CHECKLIST TRƯỚC KHI RELEASE

- [ ] TC-OFF-001: App launch offline không crash
- [ ] TC-OFF-003: Navigation hoạt động offline
- [ ] TC-OFF-005: VIP key activation không cần mạng
- [ ] TC-SPLASH-001: Consent bị skip khi offline
- [ ] TC-INTER-006: Hash/FileInfo/Settings mở được khi offline
- [ ] TC-VIP-001: Key 30 ngày hoạt động
- [ ] TC-VIP-002: Key 3 ngày hoạt động
- [ ] TC-BANNER-002: Banner ẩn khi VIP active
- [ ] TC-REWARD-001: Rewarded → VIP 3 ngày
- [ ] TC-SAFE-002: Release build không dùng TEST safety limits
