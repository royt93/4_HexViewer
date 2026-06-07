# 📊 AD SYSTEM — HEXVIEWER MIGRATION PLAN (v1.1.5)

> **SDK:** `com.roy.sdkadbmob` (AdmobApplovinWrapper) | **JitPack:** `com.github.royt93:AdmobApplovinWrapper:1.1.5` | **Provider:** AppLovin MAX (Fallback AdMob) | **minSdk:** 24 | **Cập nhật:** 2026-06-07 | **Build:** ✅ READY

---

## 0. BỐI CẢNH (SECTION 0)

| Field | Giá trị |
|---|---|
| Project hiện tại (cwd, absolute path) | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer` |
| Lib reference (absolute path) | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper` |
| File spec gốc của lib | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper/doc/AD.MD` |
| Lib coordinate + version | `com.github.royt93:AdmobApplovinWrapper:1.1.5` |
| Build variant phải xanh (debug) | `assembleDevDebug` |
| Build variant phải xanh (release) | `assembleProductionRelease` |
| Provider chính (active runtime) | `AppLovin MAX` |
| Provider fallback | `AdMob` (chỉ active khi `BuildConfig.IS_ENABLE_ADMOB=true`) |
| AppLovin SDK key | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` |
| AppLovin banner ID | `aa53ac5866596140` |
| AppLovin interstitial ID | `580476ed6f6e6d5b` |
| AppLovin app open ID | `2d9943cb1d30a275` |
| AppLovin rewarded ID | `18d3030a0f4eec4b` |
| AdMob banner ID (Release) | `ca-app-pub-3612191981543807/5282471019` |
| AdMob interstitial ID (Release) | `ca-app-pub-3612191981543807/2777620817` |
| AdMob app open ID (Release) | `ca-app-pub-3612191981543807/4547973843` |
| AdMob rewarded ID (Release) | `ca-app-pub-3940256099942544/5224354917` (dùng test ID theo yêu cầu) |
| VIP key 30 ngày (plain) | `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` |
| VIP key 3 ngày (plain) | `eQ7@93L0f!2Y2707xN04021993u0I#2aK` |
| Mức che giấu VIP key | Base64 hardcode (AI tự encode plain → Base64 khi compile vào code) |
| Privacy Policy URL | `https://loitp.notion.site/Term-Privacy-Policy-Disclaimer-319b1cd8783942fa8923d2a3c9bce60f` |

---

## 1. TOUCHPOINT TABLE

| Slot | File hiện tại | Trigger | API mới | API cũ thay thế |
|---|---|---|---|---|
| **Banner** | `ActMain.java`<br>`ActRecentlyOpen.java` | `setupViews()` | `AdManager.loadBanner` | `AdManager.INSTANCE.loadBanner` (cũ) |
| **Interstitial** | `ActMain.java` | Click nút "Recently Open", "Settings", "Hash Calculator", "File Info Analyzer" | `AdManager.showInterstitial` | `AdManager.INSTANCE.showInterstitial` (cũ) |
| **App Open** | `SplashActivity.java` + foreground resume | warmup ở splash + `ProcessLifecycleOwner` cho resume | `AdManager.awaitSplashComplete` | `AdManager.INSTANCE.initSplashScreen` (cũ) |
| **Rewarded** | VIP Screen (`FVipManagement.kt`) | User click nút "Xem quảng cáo → 3 ngày VIP" | `AdManager.loadRewarded` + `showRewarded` | `(chưa có)` |

---

## 2. STEP-BY-STEP MIGRATE PLAN

### Bước 1: Setup Dependencies
- Cập nhật Gradle: Thay đổi coordinate từ `com.github.royt93:AdmobWrapper:1.1.1` sang `com.github.royt93:AdmobApplovinWrapper:1.1.5` trong [app/build.gradle](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/build.gradle).
- Thêm thư viện confetti `nl.dionsegijn:konfetti-xml:2.0.5` cho VIP activation.
- Đảm bảo `buildFeatures` bật `viewBinding = true`.

### Bước 2: App Init & Consent Flow
- Trong [MyApplication.java](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java), thay đổi `setupAd()` để sử dụng constructor đầy đủ của `AdSdkConfig` (thêm các tham số rewarded ID, SDK key, vipKeySecret).
- Đơn giản hóa quá trình khởi tạo sử dụng `AdManager.INSTANCE.initialize(this, (success, gaid) -> ...)` thay cho việc gọi AppLovin SDK initialization thủ công.
- Trong [SplashActivity.java](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/SplashActivity.java) (Java):
  - Gọi `AdManager.INSTANCE.requestConsentInfoUpdate(this, false, canRequestAds -> ...)` tại `onCreate`.
  - Nếu `canRequestAds == true`, tiếp tục chạy splash flow bằng cách gọi callback `AdManager.INSTANCE.initSplashScreen(this, () -> { goToMain(); return null; })`.
  - Nếu user từ chối hoặc gặp lỗi, đi thẳng tới `goToMain()`.

### Bước 3: Thiết lập màn hình VIP UI/UX (Fragment)
- Tạo package `com.galaxyjoy.hexviewer.feature.vip` chứa các file:
  - `FVipManagement.kt` (Fragment chứa 13 component giao diện và 5 hiệu ứng animation).
  - `VipKeys.kt` (Xử lý Base64 mã hóa VIP key 3 ngày và 30 ngày).
  - `VipPrefs.kt` (Lưu trữ thời điểm kích hoạt cục bộ để hiển thị Progress Bar).
- Tạo layout `f_vip_management.xml` theo tiêu chuẩn Material 3.
- Bổ sung tài nguyên drawable (`ic_workspace_premium`, `bg_vip_status_header_active`, v.v.) và animation (`slide_in_bottom`).
- Thêm string resources tiếng Anh (default) vào `values/strings.xml` và tiếng Việt vào `values-vi-rVN/strings.xml`.

### Bước 4: Tích hợp Banner & Lifecycle
- Trong [ActMain.java](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java) và [ActRecentlyOpen.java](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260405_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActRecentlyOpen.java), loại bỏ việc gọi thủ công `bannerPause`, `bannerResume`, và `bannerDestroy` trong các phương thức lifecycle vì SDK 1.1.5 tự động quản lý qua `autoManageLifecycle = true` (mặc định).
- Ẩn/hiện container Banner phụ thuộc vào trạng thái VIP: kiểm tra `AdManager.INSTANCE.isVipByKeyActive()` hoặc GAID whitelist.

### Bước 5: Interstitial & Rewarded
- Giữ nguyên call site Interstitial trong `ActMain.java`.
- Bổ sung call site Rewarded trong `FVipManagement.kt` cho nút xem quảng cáo nhận 3 ngày VIP.

---

## 3. BẢNG SO SÁNH TRƯỚC / SAU MIGRATION

| Tiêu chí | Trước migration (v1.1.1) | Sau migration (v1.1.5) |
|---|---|---|
| **Provider** | AppLovin MAX (AdMob fallback) | AppLovin MAX (AdMob fallback) |
| **SDK Dependency** | `AdmobWrapper:1.1.1` | `AdmobApplovinWrapper:1.1.5` |
| **UMP Consent (GDPR)** | Chưa tích hợp | Tích hợp hoàn toàn ở Splash screen |
| **VIP Feature** | Không hỗ trợ | VIP bằng Key, Whitelist GAID, Auto 1-day Trial |
| **Banner Lifecycle** | Gọi manual ở Activity | Auto-managed bằng SDK lifecycle tracker |
| **Rewarded Ad** | Chưa hỗ trợ | Xem ad nhận 3 ngày VIP |
| **Dialog Style** | AppCompat Dialog | Material You Dialog (Material 3) |
| **Ad Safety Guard** | Base config | `AdSafetyLimits` linh hoạt (Debug vs Release) |
