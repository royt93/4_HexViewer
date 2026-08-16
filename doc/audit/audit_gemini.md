# Audit độc lập — gemini — 2026-08-16

## Xác nhận đầu mối nghi vấn đã cho

### 1. SDK version đang dùng `1.1.5` vs yêu cầu tối thiểu `1.6.2`
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** [`app/build.gradle:198`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L198) — `implementation("com.github.royt93:AdmobApplovinWrapper:1.1.5")`.
- **Hậu quả thực tế:**
  - `1.1.5` là phiên bản rất cũ (< 1.2.0, < 1.5.5, < 1.6.1, < 1.6.2), chưa có bản vá lỗi VIP được cấp thầm lặng khi khởi động app (đã fix tại 1.6.2).
  - Chưa hỗ trợ chữ ký bất đối xứng ECDSA P-256 cho VIP token (`activateVipByToken`).
  - Thiếu cờ `grantRewardOnEarn` mặc định `true` (1.6.1+) khiến user xem xong video nhưng nếu app bị kill trước khi đóng ad thì mất phần thưởng.
  - Bộ đếm session không tự reset sau 30 phút background (1.6.1+).
  - Thiếu hỗ trợ chuẩn IAB GPP cho traffic tại các bang của Mỹ.
  - Khi nâng lên ≥ 1.6.2 cần lưu ý: `AdSafetyStatus` đổi tên 2 field (`totalImpressions` → `ctrWindowImpressions`, `fullscreenClicks` → `ctrWindowClicks`), `grantRewardOnEarn` đổi mặc định `false` → `true`.

---

### 2. `MyApplication.setupAd()`: `vipKeySecret` bị gán bằng `VipKeys.VIP_30D_KEY`
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** 
  - [`app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java:614`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java#L614) — `/* vipKeySecret */ com.galaxyjoy.hexviewer.feature.vip.VipKeys.INSTANCE.getVIP_30D_KEY()`.
  - [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipKeys.kt:8-9`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipKeys.kt#L8-L9) — `VIP_30D_KEY` là chuỗi decode từ Base64 của plain redeem key 30 ngày (`9fA0q7eN!27cLx04@21993Y2u0I7#Q0`).
  - [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:144-145, 242-243`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L144-L145) — lấy `AdManager.adConfig.vipKeySecret` truyền vào `activateVipByKey`.
- **Hậu quả thực tế:**
  - `vipKeySecret` trong `AdSdkConfig` là secret HMAC dùng để ký chống-tamper SharedPreferences nội bộ (bảo vệ hạn VIP, AdSafety counters). Doc yêu cầu đây phải là chuỗi ngẫu nhiên bí mật ≥16 ký tự độc lập, **KHÔNG ĐƯỢC trùng với VIP redeem key**.
  - Việc gán `vipKeySecret` bằng redeem key khiến bất kỳ ai có mã redeem hoặc decompile app đều nắm được HMAC secret để bypass tính toàn vẹn prefs.
  - Trên SDK ≥ 1.2.0, nhánh `allowLegacyPlaintextVipKey` mặc định tắt (`false`), việc gọi `activateVipByKey` với secret key sẽ **thất bại trong im lặng** trên bản release.

---

### 3. `AdSdkConfig` constructor trong `setupAd()` thiếu các field quan trọng
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** [`app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java:602-616`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/MyApplication.java#L602-L616).
- **Hậu quả thực tế của từng field thiếu:**
  - `vipTokenPublicKey` (thiếu): SDK fallback về public key mẫu. Ở bản release, SDK sẽ **từ chối im lặng** mọi VIP token ECDSA (`activateVipByToken` luôn trả về `false`).
  - `appOpenExcludedActivities` (thiếu): Mặc định `emptyList()`. App đang dựa vào fallback deprecated string-match `"SplashActivity"`, dễ bị vỡ khi R8/obfuscation đổi tên class.
  - `applovinPrivacyPolicyUrl` (thiếu): Mặc định `null`. Dù project đã có [`app/build.gradle:35`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L35) `PRIVACY_POLICY_URL`, việc không truyền vào config khiến tính năng AppLovin Native CMP (nếu bật) không đủ điều kiện hoạt động.
  - `applovinHasUserConsent` (thiếu): Mặc định `null`. Chấp nhận được trên `gms` (do UMP xử lý), nhưng sẽ gây zero-ad nếu chạy trên môi trường không có Google Play Services (`nongms`).
  - `paidEventListener` (thiếu): Không được gán tại `MyApplication.onCreate()` → mất hoàn toàn dữ liệu tracking doanh thu quảng cáo (Ad Revenue / ROAS / LTV).
  - `errorReporter` (thiếu): Không được gán → không thể forward exception nội bộ của SDK lên Crashlytics/Sentry.

---

### 4. Không tìm thấy `AdManager.setTestDeviceIds(...)` trong code
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** Grep toàn bộ codebase `app/src/main` không có bất kỳ lệnh gọi `setTestDeviceIds`.
- **Hậu quả thực tế:**
  - Vi phạm Bước 3b của spec.
  - Khi test trên thiết bị thật, các click/impression từ dev và QA sẽ bị xem là **Invalid Traffic (IVT)** bởi AdMob và AppLovin, dẫn đến nguy cơ cao bị giới hạn quảng cáo (ad serving limit) hoặc đình chỉ tài khoản kiếm tiền.

---

### 5. `app/build.gradle` buildType `release`: `ADMOB_REWARDED_ID` là Google test ID
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** 
  - [`app/build.gradle:93`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L93) (buildType `release`) — `buildConfigField "String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\""`.
  - [`app/build.gradle:114`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L114) (buildType `debug`) — trùng đúng ID demo trên.
- **Hậu quả thực tế:**
  - `ca-app-pub-3940256099942544/5224354917` là ID test chính thức của Google.
  - Khi build bản release đưa lên Play Store, Rewarded Ad sẽ hiển thị test ad của Google, tạo ra **$0 doanh thu** và **vi phạm trực tiếp chính sách AdMob** (cấm ship test ad unit trên production), có thể bị từ chối cập nhật hoặc khóa ad unit.

---

### 6. `IS_ENABLE_ADMOB = false` cho cả debug và release vs Yêu cầu ưu tiên AdMob
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** [`app/build.gradle:94`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L94) và [`app/build.gradle:115`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L115) đều khai báo `buildConfigField("Boolean", "IS_ENABLE_ADMOB", "false")`.
- **Hậu quả thực tế:** Provider active runtime hiện tại là AppLovin MAX, không phải AdMob. Do user (chủ project) đã yêu cầu rõ ràng **ưu tiên dùng AdMob**, việc chuyển cờ sang `true` là bắt buộc và hợp lệ theo chỉ thị từ user, nhưng cần đi kèm trọn bộ AdMob ID thật.

---

### 7. `AndroidManifest.xml`: AdMob App ID có comment "Sample AdMob App ID"
- **Kết luận:** **KHÔNG XÁC ĐỊNH (Cần user xác nhận)**
- **Bằng chứng:** [`app/src/main/AndroidManifest.xml:38-39`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/AndroidManifest.xml#L38-L39) — `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-3612191981543807~2249113565" /> <!-- Sample AdMob App ID -->`.
- **Phân tích:** Publisher ID `ca-app-pub-3612191981543807` khớp với prefix của Banner, Interstitial, App Open trong `release` buildType ([`app/build.gradle:90-92`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L90-L92)). Tuy nhiên, do có comment "Sample", cần user xác nhận chính thức đây có phải App ID thật của app trên AdMob Console hay không.

---

### 8. Thiếu thẻ `<attribution>` trong `AndroidManifest.xml` (targetSdk 36 ≥ 31)
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:** [`app/build.gradle:14`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/build.gradle#L14) có `targetSdk 36`, nhưng trong [`app/src/main/AndroidManifest.xml`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/AndroidManifest.xml) không có khai báo thẻ `<attribution android:tag="ads" android:label="Ad Networks (AdMob / AppLovin MAX)" />`.
- **Hậu quả thực tế:** Gây warning liên tục trong logcat trên thiết bị Android 12+: `attributionTag not declared`.

---

### 9. Step 8 (Consent/UMP publish) & VIP ECDSA Keypair chưa thiết lập
- **Kết luận:** **ĐÚNG**
- **Bằng chứng:**
  - Không có cấu hình `vipTokenPublicKey` trong `MyApplication.java` hay `app/build.gradle`.
  - Consent UMP message trên AdMob console là thao tác bên ngoài (release gate), cần xác nhận đã Publish trước khi release.

---

## Finding mới tự tìm thêm

### Finding 1 (Mức độ CAO / VI PHẠM CHÍNH SÁCH GOOGLE & APPLOVIN): Fallback Interstitial cấp VIP khi Rewarded Ad không sẵn sàng
- **Vị trí:** [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:189-194`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L189-L194)
- **Mô tả:** Trong luồng "Xem quảng cáo → nhận 3 ngày VIP", khi `showRewarded` trả về `earned == false`, code gọi fallback sang `showInterstitial`. Nếu interstitial hiển thị thành công (`shown == true`), code lại gọi `grantVipFromAd()` để cấp VIP 3 ngày!
  ```kotlin
  // ActVipManagement.kt:189-194
  AdManager.showInterstitial(this) { shown ->
      if (isFinishing) return@showInterstitial
      if (shown) {
          grantVipFromAd() // 🚨 VI PHẠM POLICY!
      } else {
          showNoAdDialog()
      }
  }
  ```
- **Vi phạm:** Vi phạm trực tiếp **Step 7 Rule 6** và **Chính sách Rewarded Ad của Google/AppLovin (Policy A-15)**: Tuyệt đối không được cấp phần thưởng cho bất kỳ format quảng cáo nào khác ngoài Rewarded (kể cả Interstitial fallback). Vi phạm điều này có nguy cơ bị **ban tài khoản AdMob/AppLovin ngay lập tức**.
- **Cách fix:** Nhánh fallback `showInterstitial` chỉ mang tính chất monetization bù đắp, tuyệt đối không được gọi `grantVipFromAd()`. Khi `showInterstitial` đóng lại, phải hiển thị dialog thông báo không nhận được thưởng (hoặc không cấp thưởng).

---

### Finding 2 (Mức độ CAO): `grantVipFromAd()` gọi sai API `activateVipByKey` với secret key thay vì `grantVipDays`
- **Vị trí:** [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:241-251`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L241-L251)
- **Mô tả:** Hàm `grantVipFromAd()` đang thực hiện:
  ```kotlin
  val secretKey = AdManager.adConfig.vipKeySecret
  val success = AdManager.activateVipByKey(this, secretKey, 3)
  ```
- **Hậu quả:** Theo doc (Mục "Cơ chế Trial / VIP", dòng 450–452 và Step 10.3 row 7), `activateVipByKey` là nhánh legacy và mặc định tắt trên SDK ≥ 1.2.0. Việc truyền `vipKeySecret` vào `activateVipByKey` sẽ fail im lặng, dẫn đến việc user xem xong rewarded ad nhưng luôn bị báo lỗi "Thất bại" và không được cộng ngày VIP.
- **Cách fix:** Thay thế bằng API chuẩn nội bộ: `AdManager.grantVipDays(this, 3)`.

---

### Finding 3 (Mức độ CAO): Màn hình `ActVipManagement.kt` hoàn toàn không hỗ trợ nhập VIP Token ECDSA
- **Vị trí:** [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:127-159`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L127-L159)
- **Mô tả:** Khi bấm nút "Kích hoạt", code chỉ gọi `VipKeys.lookupDays(inputKey)`. File `VipKeys.kt` chỉ chứa 2 mã Base64 tĩnh (30 ngày và 3 ngày). Nếu user/QA dán token ECDSA (~68 ký tự sinh từ `generateVipToken`), hàm `lookupDays` trả về `null` và báo lỗi "Mã không hợp lệ". Hàm `AdManager.activateVipByToken(context, token)` hoàn toàn không được gọi ở bất cứ đâu.
- **Cách fix:** Cập nhật luồng nhập key tại `ActVipManagement.kt`: Gọi `AdManager.activateVipByToken(this, inputKey)` trước, nếu không khớp token ECDSA mới kiểm tra fallback mã redeem hoặc thẻ cào.

---

### Finding 4 (Mức độ TRUNG): Quản lý `grantedAtMs` thủ công trong `VipPrefs` thay vì dùng API có sẵn của SDK
- **Vị trí:** 
  - [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefs.kt:8-10`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefs.kt#L8-L10)
  - [`app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:148, 245, 256`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt#L148)
- **Mô tả:** `VipPrefs` tự lưu `granted_at_ms` trong SharedPreferences riêng của app. Khi render UI, `ActVipManagement.kt` lấy `grantedAtMs = vipPrefs.getGrantedAtMs()`.
- **Hậu quả:** Vi phạm Step 10.1 và Step 10.3 (row 2). Khi user được nhận VIP qua First-install Grace (auto-trial 1 ngày) hoặc `grantVipDays`, `vipPrefs.getGrantedAtMs()` sẽ trả về `0L`, khiến công thức tính thanh tiến trình `computeElapsedProgress` bị sai hoàn toàn và ngày kích hoạt hiển thị `01/01/1970`.
- **Cách fix:** Đọc trực tiếp từ `AdManager.getVipGrantedAtMs()` của SDK, loại bỏ các hàm lưu `grantedAtMs` thủ công trong `VipPrefs`.

---

### Finding 5 (Mức độ TRUNG): `ActRecentlyOpen.java` có vị trí tải Banner nhưng bị bỏ sót trong bảng Touchpoint của `doc/AD.MD`
- **Vị trí:** [`app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActRecentlyOpen.java:171-193`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActRecentlyOpen.java#L171-L193)
- **Mô tả:** `ActRecentlyOpen` cũng có chứa banner ad (`AdManager.INSTANCE.loadBanner(...)`), nhưng trong bảng Touchpoint của `doc/AD.MD` (mục 1) chỉ liệt kê `ActMain.java`.
- **Cách fix:** Cập nhật bổ sung `ActRecentlyOpen.java` vào bảng Touchpoint trong `doc/AD.MD` để kiểm thử và theo dõi đầy đủ mọi vị trí đặt quảng cáo.

---

### Finding 6 (Mức độ THẤP): Gọi thủ công `bannerDestroy` khi `autoManageLifecycle = true` và import thừa `com.google.android.gms.ads.AdSize`
- **Vị trí:** 
  - [`app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java:39, 230, 759`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java#L39)
  - [`app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActRecentlyOpen.java:37, 138, 179`](file:///Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/@ad/260620_HexViewer/app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActRecentlyOpen.java#L37)
- **Mô tả:** Cả 2 Activity đều truyền `autoManageLifecycle = true` trong `loadBanner`, nhưng vẫn tự gọi `AdManager.INSTANCE.bannerDestroy(adView)` trong `onDestroy()` và `refreshBannerState()`. Đồng thời có import thừa class `com.google.android.gms.ads.AdSize`.
- **Cách fix:** Xóa import thừa và chuẩn hóa lifecycle quản lý banner theo đúng contract của SDK (Appendix A.3).

---

## Kết luận GO/NO-GO cho việc build lại migrate plan

- **Đánh giá trạng thái code hiện tại:** **🔴 NO-GO CHO PRODUCTION RELEASE**
  - Project hiện tồn tại nhiều lỗi nghiêm trọng: ID test AdMob nằm trong bản release, vi phạm policy cấp thưởng trên Interstitial fallback, cấu hình secret key sai mục đích dẫn tới fail luồng VIP, và phiên bản SDK quá cũ (`1.1.5`).
- **Đánh giá kế hoạch cập nhật Migration Plan:** **🟢 GO CHO VIỆC CẬP NHẬT `doc/AD.MD`**
  - Cần tổng hợp tất cả các finding trên để hoàn thiện kế hoạch migrate trong `doc/AD.MD`.
  - Cần yêu cầu user cung cấp:
    1. Xác nhận AdMob App ID chính thức trong manifest (`ca-app-pub-3612191981543807~2249113565`).
    2. AdMob Rewarded Ad Unit ID production thật thay thế cho ID test `ca-app-pub-3940256099942544/5224354917`.
    3. Xác nhận nâng SDK lên bản mới nhất (≥ `1.6.2`).
    4. Sinh cặp khóa ECDSA VIP Token (`AdManager.generateVipKeyPair()`) và tạo secret HMAC mới cho `vipKeySecret`.
