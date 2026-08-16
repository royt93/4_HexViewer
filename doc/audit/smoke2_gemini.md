# Smoke-test vòng 2 — GEMINI — 2026-08-16

## Build status

- **assembleDevDebug**: **PASS**
  - Thực thi `./gradlew assembleDevDebug --no-build-cache --rerun-tasks` → `BUILD SUCCESSFUL`.
  - APK sinh ra: [`app/build/outputs/apk/dev/debug/app-dev-debug.apk`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build/outputs/apk/dev/debug/app-dev-debug.apk) (27.38 MB).

- **assembleProductionRelease**: **PASS**
  - Thực thi `./gradlew assembleProductionRelease --no-build-cache --rerun-tasks` → `BUILD SUCCESSFUL`.
  - R8 minification (`minifyProductionReleaseWithR8`), resource shrinking (`shrinkProductionReleaseRes`) và signing đều hoàn tất không có lỗi.
  - APK sinh ra: [`app/build/outputs/apk/production/release/app-production-release.apk`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build/outputs/apk/production/release/app-production-release.apk) (21.82 MB).

- **Xác nhận 2 bug đã fix trước đó**:
  1. **Fix compile error release (`UMP_DEBUG_GEOGRAPHY` / `UMP_TEST_DEVICE_HASH`)**: ĐÃ ĐÚNG. Hai `buildConfigField` đã được chuyển vào `defaultConfig` tại [`app/build.gradle:55-56`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L55-L56). Nhờ đó cả hai build variants (`devDebug` và `productionRelease`) đều có symbol tương ứng trong class `BuildConfig`, loại bỏ hoàn toàn lỗi unresolved reference.
  2. **Fix hiển thị số ngày VIP khi cộng dồn (`showActivationSuccess`)**: ĐÃ ĐÚNG. Hàm `showActivationSuccess(daysGranted: Int? = null)` tại [`ActVipManagement.kt:509-526`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L509-L526) nhận `REWARD_VIP_DAYS` (=3) khi xem video reward (hiển thị chuỗi `"Successfully activated VIP for 3 days!"`), và dùng `null` khi nhập token/redeem code (hiển thị chuỗi `vip_success_message_until`: `"VIP activated! Active until <date>."`). Cách xử lý này giải quyết triệt để lỗi đánh lừa số ngày do SDK 1.6.16 ghi đè `grantedAtMs = now` mỗi khi cộng dồn expiry.

---

## Xác nhận claim cũ (doc/AD.MD mục 5.3, 6.2, 6.4)

| Claim | Kết quả | Bằng chứng kiểm tra (File:Line) |
|---|---|---|
| **F1** — Nâng SDK 1.1.5 lên 1.6.16 | **ĐÚNG** | [`app/build.gradle:225`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L225) (`implementation("com.github.royt93:AdmobApplovinWrapper:1.6.16")`), artifact AAR nằm tại `~/.m2/repository/com/github/royt93/AdmobApplovinWrapper/1.6.16/`. |
| **F2 / F13 / F22** — `vipKeySecret` độc lập với VIP redeem key, reward dùng `grantVipDays` | **ĐÚNG** | [`app/build.gradle:39`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L39) sinh random 32-byte Base64 secret; [`AdSetup.kt:37`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt#L37); [`ActVipManagement.kt:248`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L248) gọi `AdManager.grantVipDays(this, REWARD_VIP_DAYS)`. |
| **F3** — Keypair ECDSA P-256 (`vipTokenPublicKey`), private key không đóng gói | **ĐÚNG** | [`app/build.gradle:43`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L43); [`AdSetup.kt:39`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt#L39); private key lưu tại [`local.properties:15`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/local.properties#L15) (nằm trong `.gitignore`). |
| **F3** — Wire `appOpenExcludedActivities`, `applovinPrivacyPolicyUrl` | **ĐÚNG** | [`AdSetup.kt:44-45`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt#L44-L45) cấu hình `listOf(SplashActivity::class.java)` và `BuildConfig.PRIVACY_POLICY_URL`. |
| **F3** — Wire `paidEventListener` và `errorReporter` | **ĐÚNG** | [`AdSetup.kt:55-66`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt#L55-L66) được gọi từ [`MyApplication.java:597-599`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java#L597-L599) trong `Application.onCreate()`. |
| **F4 / F6 / F9** — AdMob ID production, `IS_ENABLE_ADMOB=true`, Manifest App ID | **ĐÚNG** | [`app/build.gradle:112-115,121`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L112-L121) dùng publisher ID `3004713799155145`; [`AndroidManifest.xml:44`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/AndroidManifest.xml#L44) dùng App ID `ca-app-pub-3004713799155145~6239626749` khớp [`doc/ad/ad.md`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/doc/ad/ad.md). |
| **F5** — Scaffold `setTestDeviceIds` | **ĐÚNG** | [`AdSetup.kt:73-78`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt#L73-L78) đọc `BuildConfig.TEST_DEVICE_GAID_1/2`. |
| **F8** — Khai báo `<attribution>` tag | **ĐÚNG** | [`AndroidManifest.xml:19-22`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/AndroidManifest.xml#L19-L22); [`res/values/strings.xml:243`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/res/values/strings.xml#L243). |
| **F12** — Fallback Interstitial tuyệt đối KHÔNG cấp VIP | **ĐÚNG** | [`ActVipManagement.kt:189-203`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L189-L203): chỉ khi `earned == true` mới gọi `grantVipFromAd()`, nhánh `showInterstitial` chỉ hiển thị `showNoRewardDialog()` / `showNoAdDialog()`. |
| **F14** — SDK là single source of truth cho VIP timestamp | **ĐÚNG** | [`VipPrefs.kt:5-15`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefs.kt#L5-L15); [`ActVipManagement.kt:261`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L261) đọc trực tiếp từ `AdManager.getVipGrantedAtMs()`. |
| **F15** — Bỏ `markUserRedeemed()` khỏi nhánh xem quảng cáo | **ĐÚNG** | [`ActVipManagement.kt:243-254`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L243-L254) không còn gọi `markUserRedeemed()`; hàm chỉ được gọi tại nhánh nhập key [`ActVipManagement.kt:158`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L158). |
| **F18** — SDK tự resolve mã thẻ cào + ECDSA token | **ĐÚNG** | [`ActVipManagement.kt:155`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L155); [`VipKeys.kt:22-27`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipKeys.kt#L22-L27); SDK 1.6.16 `AdManager.kt:3550-3650` verify token ECDSA trước rồi tới `vipRedeemCodes`. |
| **F20 / F24** — Xoá import thừa `AdSize` & TODO | **ĐÚNG** | Đã dọn dẹp sạch trong `ActMain.java`, `ActRecentlyOpen.java`, `MyApplication.java`. |
| **QC-1** — UMP Publisher misconfiguration cho App ID mới | **ĐÚNG diễn giải** | Code AndroidManifest đã đặt đúng App ID mới. Trên AdMob Console (apps.admob.com), cần đảm bảo Privacy & messaging → European regulations message đã được **Published** cho đúng App ID `ca-app-pub-3004713799155145~6239626749`. |
| **QC-2** — Watchdog 30s tạo `IllegalStateException` khi offline | **ĐÚNG** | SDK 1.6.16 `AdManager.kt:1484-1495` arm timer 30s sau init; nếu không có consent update, exception được gửi qua `errorReporter` (không crash). Đây là hành vi SDK, cần filter trong Crashlytics khi tích hợp sau này. |

---

## Bug/regression MỚI tự tìm

### 1. 🔴 CAO: Toàn bộ Unit Test và AndroidTest suites bị gãy biên dịch do refactor SDK
- **File:line**:
  - [`app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementTest.kt:53,166`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementTest.kt#L53)
  - [`app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/AdIntegrationTest.kt:129`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/AdIntegrationTest.kt#L129)
  - [`app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/VipKeysTest.kt:16,23,29,34,35`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/VipKeysTest.kt#L16)
  - [`app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefsTest.kt:21,31,38,39,44,45,46`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefsTest.kt#L21)
  - [`app/src/androidTest/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementInstrumentationTest.kt:49`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/androidTest/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementInstrumentationTest.kt#L49)
  - [`app/src/androidTest/java/com/galaxyjoy/hexviewer/ui/ActMainPillAnimationInstrumentationTest.java:42-47`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/androidTest/java/com/galaxyjoy/hexviewer/ui/ActMainPillAnimationInstrumentationTest.java#L42-L47)
- **Mô tả**: Khi thực hiện nâng cấp SDK 1.6.16 và sửa các finding F14/F18/F22, các hàm `VipKeys.lookupDays` và `VipPrefs.saveGrantedAtMs/getGrantedAtMs/clearGrantedAtMs` đã bị xoá khỏi production source code. Tuy nhiên, các file test trong thư mục `src/test` và `src/androidTest` không được cập nhật theo, dẫn đến việc chạy `./gradlew testDevDebugUnitTest` và `./gradlew compileDevDebugAndroidTestKotlin` bị lỗi biên dịch (15 unresolved references).
- **Patch đề xuất**:
  1. Cập nhật `VipKeysTest.kt` để test `VipKeys.REDEEM_CODES`.
  2. Cập nhật `VipPrefsTest.kt` chỉ test `markUserRedeemed` / `userRedeemedAtLeastOnce`.
  3. Cập nhật `ActVipManagementTest.kt` và `AdIntegrationTest.kt` dùng `AdManager.getVipGrantedAtMs()` / `AdManager.activateVipByKey(ctx, key, 0)`.
  4. Cập nhật `ActMainPillAnimationInstrumentationTest.java` dùng `AdSetup` helper hoặc Kotlin builder để khởi tạo `AdSdkConfig`.

---

### 2. 🟡 TRUNG: `android:inputType="textCapCharacters"` làm bàn phím ảo tự động VIẾT HOA, gây lỗi khi nhập mã VIP Base64 và ECDSA Token
- **File:line**: [`app/src/main/res/layout/f_vip_management.xml:227`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/res/layout/f_vip_management.xml#L227)
- **Mô tả**: `TextInputEditText` cho ô nhập mã VIP (`etVipKey`) được cấu hình `android:inputType="textCapCharacters"`. Trong khi đó:
  - Mã thẻ cào Base64 (`9fA0q7eN!27cLx04@21993Y2u0I7#Q0`, `eQ7@93L0f!2Y2707xN04021993u0I#2aK`) và ECDSA Token (`v2|...`) **phân biệt chữ hoa / chữ thường (case-sensitive)**.
  - Thuộc tính `textCapCharacters` ép bàn phím ảo luôn ở trạng thái Shift/Caps lock, khiến người dùng gõ phím bị biến thành chữ hoa toàn bộ (ví dụ: `9FA0Q7EN...`) dẫn tới kích hoạt thất bại.
- **Patch đề xuất**:
```xml
--- a/app/src/main/res/layout/f_vip_management.xml
+++ b/app/src/main/res/layout/f_vip_management.xml
@@ -224,4 +224,4 @@
                                 android:id="@+id/etVipKey"
                                 android:layout_width="match_parent"
                                 android:layout_height="wrap_content"
-                                android:inputType="textCapCharacters"
+                                android:inputType="textNoSuggestions|textVisiblePassword"
                                 android:maxLines="1" />
```

---

### 3. 🟡 TRUNG: Nguy cơ `android.view.WindowLeaked` khi người dùng thoát màn hình lúc đang xác thực mã VIP
- **File:line**: [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:138-165,560-569`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L138-L165)
- **Mô tả**: Biến `val progressDialog` được khởi tạo cục bộ trong click listener của nút Activate. Nếu người dùng bấm icon quay lại trên toolbar (`binding.toolbar.setNavigationOnClickListener`) trong khoảng thời gian delay 1000ms, hàm `onDestroy()` huỷ `activateRunnable` nhưng không thể dismiss `progressDialog` (vì biến cục bộ đã mất scope), dẫn đến exception `android.view.WindowLeaked` trên logcat khi Activity bị huỷ.
- **Patch đề xuất**: Chuyển `progressDialog` thành biến thành viên `private var verifyingDialog: Dialog? = null`, và thêm `verifyingDialog?.dismiss()` trong `onDestroy()`.

---

### 4. 🟢 THẤP: Thiếu chuỗi dịch trong `values-vi-rVN/strings.xml` và các locale khác
- **File:line**: [`app/src/main/res/values-vi-rVN/strings.xml`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/res/values-vi-rVN/strings.xml)
- **Mô tả**: Các chuỗi mới được thêm vào `res/values/strings.xml` (`vip_success_message_until`, `attribution_ads_label`, `vip_no_reward_title`, `vip_no_reward_message`) chưa được bổ sung vào các file ngôn ngữ khác (ví dụ: tiếng Việt `values-vi-rVN`). Android sẽ fallback về tiếng Anh (không crash), nhưng khiến người dùng tiếng Việt thấy dialog "No Reward This Time" hay "VIP activated! Active until..." bằng tiếng Anh.
- **Patch đề xuất**: Bổ sung vào `app/src/main/res/values-vi-rVN/strings.xml`:
```xml
    <string name="vip_success_message_until">Đã kích hoạt VIP! Có hạn đến %1$s.</string>
    <string name="attribution_ads_label">Mạng quảng cáo (AdMob / AppLovin MAX)</string>
    <string name="vip_no_reward_title">Chưa có phần thưởng lần này</string>
    <string name="vip_no_reward_message">Hiện chưa có video nhận thưởng, nên chưa thể cộng ngày VIP. Vui lòng bấm \"Xem quảng cáo\" lại sau ít phút.</string>
```

---

## Live device test

- **Thiết bị**: `FUJZIFIR7DQCNRWW` (OPPO CPH1989 / Android 11).
- **Cold-start test (debug build)**:
  - Cài đặt bản debug APK và khởi chạy `SplashActivity` thành công.
  - Logcat xác nhận UMP consent fetch thành công (`consent fetch OK`, `canRequestAds=true`).
  - AdMob initialization hoàn tất thành công (`startProviderInitialization 🔄 state=INITIALIZING (isEnableAdmob=true)`, `ADS STATUS: ✅ READY`).
  - Banner, Interstitial, App Open, và Rewarded test ads đều được queue/load đúng theo thiết kế.
- **Phục hồi trạng thái máy**: Đã thực hiện `adb uninstall com.galaxyjoy.hexviewer` và cài đặt lại bản [`app-production-release.apk`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build/outputs/apk/production/release/app-production-release.apk) sạch lên thiết bị để bảo toàn trạng thái trước khi kết thúc task.

---

## Kết luận

1. **Chất lượng Code Tích Hợp Ad SDK**: Toàn bộ các finding CAO từ vòng 1 (F1-F4, F8, F9, F12-F15, F18, F20, F24) đã được triển khai chính xác trên mã nguồn thực tế. Việc loại bỏ cấp VIP ở fallback interstitial (F12) và tách `vipKeySecret` độc lập khỏi VIP redeem key (F2/F13) hoạt động đúng như thiết kế.
2. **Trạng thái Build**: Cả 2 target `./gradlew assembleDevDebug` và `./gradlew assembleProductionRelease` đều **PASS 100%**. Hai bug trước đó (compile error release BuildConfig và hiển thị sai số ngày VIP) đã được sửa đúng và đủ.
3. **Các điểm cần xử lý trước khi release chính thức**:
   - Refactor bộ Unit Test / AndroidTest suites theo các API mới của SDK 1.6.16 (Bug 1).
   - Đổi `inputType` ô nhập VIP sang `textNoSuggestions|textVisiblePassword` để tránh lỗi viết hoa phím ảo (Bug 2).
   - Thêm dismiss dialog trong `onDestroy()` để tránh `WindowLeaked` (Bug 3).
   - Kiểm tra AdMob Console cho Step 8 UMP regulations (QC-1) cho đúng App ID mới.
