# Smoke-test vòng 2 — claude2 — 2026-08-16

## Build status

- **assembleDevDebug: ✅ PASS** — verify bằng `./gradlew clean assembleDevDebug` (clean thật, không
  cache đánh lừa) → `BUILD SUCCESSFUL`.
- **assembleProductionRelease: ❌ FAIL** — `./gradlew assembleProductionRelease` → **compile error**,
  tái hiện ổn định 2 lần liên tiếp:
  ```
  e: .../ads/AdSetup.kt:48:45 Unresolved reference 'UMP_DEBUG_GEOGRAPHY'.
  e: .../ads/AdSetup.kt:49:50 Unresolved reference 'UMP_TEST_DEVICE_HASH'.
  > Task :app:compileProductionReleaseKotlin FAILED
  ```
  Toàn bộ lỗi "Argument type mismatch: actual type X but X was expected" ở dòng 24-45 (~20 dòng) chỉ là
  hệ quả cascade của Kotlin overload-resolution fail do 2 unresolved reference trên — không phải 20 bug
  riêng biệt.

  **Root cause xác nhận qua `app/build.gradle`**: `UMP_DEBUG_GEOGRAPHY`/`UMP_TEST_DEVICE_HASH`
  (`buildConfigField`) chỉ được khai trong block `buildTypes.debug` (`app/build.gradle:140-141`), KHÔNG
  có trong `buildTypes.release` lẫn `defaultConfig`. `AdSetup.kt:48-49` lại reference 2 field này
  **không điều kiện** (biểu thức Kotlin `BuildConfig.UMP_DEBUG_GEOGRAPHY.takeIf { BuildConfig.DEBUG &&
  ... }` — `BuildConfig.DEBUG` chỉ gate runtime, KHÔNG gate compile-time; field vẫn phải tồn tại trong
  class `BuildConfig` sinh ra cho build type đó). Vì `AdSetup.kt` là source dùng chung cho mọi variant,
  variant `productionRelease` không có 2 field này trong `BuildConfig` được sinh ra → không compile
  được.

  **Đây là finding CAO NHẤT của lần audit này** — trực tiếp mâu thuẫn với claim ở `doc/AD.MD` mục 5.1
  ("`./gradlew assembleProductionRelease` (R8/minify/shrink bật) → BUILD SUCCESSFUL"). Có thể lúc ghi
  claim đó, 2 field QC-only (`UMP_DEBUG_GEOGRAPHY`/`UMP_TEST_DEVICE_HASH`, thêm ở vòng "QC Master" mục 6
  sau đó) chưa tồn tại — nhưng trạng thái working-tree HIỆN TẠI chắc chắn không build được
  `assembleProductionRelease`.

  **Patch đề xuất** (không tự áp dụng): thêm 2 dòng vào `defaultConfig` trong `app/build.gradle` (áp
  dụng cho mọi variant, release override rỗng vẫn ổn vì field chỉ dùng khi `BuildConfig.DEBUG`):
  ```groovy
  defaultConfig {
      ...
      buildConfigField "String", "UMP_DEBUG_GEOGRAPHY", "\"\""
      buildConfigField "String", "UMP_TEST_DEVICE_HASH", "\"\""
  }
  ```
  rồi xoá 2 dòng trùng trong block `debug` (hoặc giữ override, Gradle cho override an toàn). Sau patch
  cần build lại cả 2 target để xác nhận xanh.

## Xác nhận claim cũ (doc/AD.MD mục 5.3, 6.2, 6.4)

| Claim | Kết luận | Bằng chứng |
|---|---|---|
| F1: SDK 1.1.5→1.6.16 | ✅ ĐÚNG | `app/build.gradle:224` `implementation("com.github.royt93:AdmobApplovinWrapper:1.6.16")`, `~/.m2/repository/.../1.6.16/` tồn tại thật |
| F2/F13/F22: `vipKeySecret` tách khỏi VIP key, random | ✅ ĐÚNG | `app/build.gradle:39` `VIP_KEY_SECRET` random base64 riêng biệt, `AdSetup.kt:37` dùng đúng field, không còn `VipKeys.VIP_30D_KEY` làm secret |
| F3 (vipTokenPublicKey, appOpenExcludedActivities, applovinPrivacyPolicyUrl, paidEventListener, errorReporter) | ✅ ĐÚNG | `AdSetup.kt:39,44,45,55-66` đều wire đủ, đúng field |
| F4/F9: ID production thật thay demo ID | ✅ ĐÚNG | `app/build.gradle:104-107` publisher `3004713799155145`, `AndroidManifest.xml:44` App ID khớp |
| F6: `IS_ENABLE_ADMOB=true` cả 2 build type | ✅ ĐÚNG | `app/build.gradle:110,137` |
| F8: `<attribution>` tag | ✅ ĐÚNG | `AndroidManifest.xml:19-21` |
| F12: bỏ cấp VIP qua fallback interstitial | ✅ ĐÚNG | `ActVipManagement.kt:183-186` nhánh `shown` giờ chỉ gọi `showNoRewardDialog()`, không còn `grantVipFromAd()` |
| F13/F22: reward-earn → `grantVipDays` | ✅ ĐÚNG | `ActVipManagement.kt:236` `AdManager.grantVipDays(this, 3)`, không còn `activateVipByKey(this, vipKeySecret, 3)` |
| F14: bỏ `VipPrefs.saveGrantedAtMs`, dùng `AdManager.getVipGrantedAtMs()` | ✅ ĐÚNG | `VipPrefs.kt` không còn 3 hàm timestamp, `ActVipManagement.kt:248` gọi thẳng SDK |
| F15: bỏ `markUserRedeemed()` khỏi reward path | ✅ ĐÚNG | `grantVipFromAd()` (dòng 232-241) không còn gọi `markUserRedeemed()` |
| F18: dùng `activateVipByKey(ctx, input, 0)` để SDK tự resolve token/redeem code | ✅ ĐÚNG | `ActVipManagement.kt:141` gọi đúng vậy, `VipKeys.kt` đổi `lookupDays` → `REDEEM_CODES` truyền vào `AdSdkConfig.vipRedeemCodes` (`AdSetup.kt:42`); đối chiếu source thật `AdManager.kt:3550-3730` tag 1.6.16 xác nhận `activateVipByKey` tự thử token ECDSA rồi tới `vipRedeemCodes` đúng như comment mô tả |
| F20: xoá import `AdSize` thừa | ✅ ĐÚNG | `ActMain.java`, `ActRecentlyOpen.java` diff xác nhận |
| F24: xoá TODO | ✅ ĐÚNG | `MyApplication.java` không còn dòng TODO |
| Mục 6.2 QC-1 (Publisher misconfiguration) | ⚪ KHÔNG XÁC ĐỊNH qua code tĩnh | Đây là trạng thái AdMob Console (ngoài code), không verify được bằng đọc source/build. Code-side: app không set `blockLoadUntilConsent` tường minh ở `SplashActivity.java:148` (dùng default SDK) — nhất quán với mô tả finding, không phát hiện gì mâu thuẫn. |
| Mục 6.4 QC-2 (watchdog 30s IllegalStateException) | ✅ ĐÚNG, verify qua source thật | Đọc `AdManager.kt:1484-1490` (tag 1.6.16) tag `1.6.16` xác nhận đúng y chang: `armPendingInitWatchdog()` bắn `IllegalStateException("Consent chưa được nối sau ${INIT_CONSENT_WATCHDOG_MS}ms")` qua `reportError`/`errorReporter`, đúng message log trong report. Đây là hành vi SDK, không phải bug code app — nhận định "filter Crashlytics, không cần sửa code" là hợp lý. |

## Bug/regression MỚI tự tìm

### 1. 🔴 CAO — `assembleProductionRelease` không build được (xem mục Build status ở trên)

File: `app/build.gradle:140-141` (thiếu field ở release/defaultConfig) +
`app/src/main/java/com/galaxyjoy/hexviewer/ads/AdSetup.kt:48-49` (reference field chỉ tồn tại ở debug
BuildConfig). Patch đề xuất đã ghi ở trên.

### 2. 🟡 TRUNG — Dialog "Successfully activated VIP for N days!" hiện SAI số ngày khi user đã có VIP còn hạn

File: `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:490-505`
(`showActivationSuccess()`), gọi từ dòng 148 (redeem code/token) và dòng 239 (`grantVipFromAd()`).

```kotlin
val days = Math.ceil(
    (AdManager.getVipByKeyExpiry() - AdManager.getVipGrantedAtMs()).toDouble() / (24 * 3600 * 1000)
).toInt().coerceAtLeast(0)
```

Code cũ (trước fix) truyền thẳng số ngày đã biết trước (`VipKeys.lookupDays()` trả `3`/`30`, hoặc hằng
số `3` cho reward). Code mới back-calculate `days` từ `expiry - grantedAtMs`. Vấn đề: đối chiếu source
thật SDK 1.6.16 (`AppPreferences.kt:619-727`, hàm `markTokenAndSaveVipAtomic`/`markRedeemAndSaveVipAtomic`/
`addVipDaysAtomic`) — mọi đường cấp VIP đều **cộng dồn** (`newExpiry = max(now, oldExpiry) + days`) và
`keyVipGrantedAtMs` bị **ghi đè bằng thời điểm activate hiện tại** mỗi lần expiry đổi (không phải giữ
nguyên timestamp lần đầu).

Hệ quả: nếu user ĐÃ có VIP còn hạn (vd còn 10 ngày từ lần redeem/reward trước) rồi redeem thêm 1 mã 3
ngày, SDK cộng dồn đúng (`expiry += 3 ngày` — hành vi ĐÚNG), nhưng dialog sẽ tính
`ceil((oldExpiry + 3ngày − now) / ngày) ≈ 13` và hiện **"Successfully activated VIP for 13 days!"**
thay vì "3 days!" — sai lệch, đánh lừa user về số ngày mã/reward vừa cấp thêm.

Test QC ở `doc/AD.MD` mục 6.7 "PASS" vì test chạy trên trạng thái sạch (chưa có VIP trước đó), nên
`grantedAtMs ≈ now` và `expiry − grantedAtMs ≈ đúng 3 ngày` tình cờ khớp — không phơi bày được bug này.
Cần test lại case "redeem lần 2 khi VIP đang active" để tái hiện.

**Patch đề xuất**: quay lại truyền thẳng số ngày đã biết cho dialog (constant `3` ở `grantVipFromAd()`,
và với redeem/token — SDK không trả về số ngày vừa cộng qua `activateVipByKey` (trả `Boolean`), nên hoặc
đổi message thành "VIP hiện có hạn tới {date}" (dùng `getVipByKeyExpiry()` trực tiếp, không trừ đi
`grantedAtMs`) thay vì "N ngày", hoặc chấp nhận hạn chế và đổi wording string `vip_success_message` cho
đúng ngữ nghĩa "hạn VIP mới" thay vì "N ngày vừa cấp".

### 3. ⚪ THẤP — `activateVipByKey` giờ yêu cầu có mạng (V-03), khác hành vi cũ

File: `AdManager.kt:3560-3563` (tag 1.6.16, SDK-side, không phải bug app) — trước đây
`VipKeys.lookupDays()` là lookup local thuần, redeem code hoạt động cả khi offline. Sau khi chuyển sang
gọi thẳng `AdManager.activateVipByKey`, SDK chặn cứng nếu `networkChecker(context)==false`. User offline
nhập đúng mã vẫn nhận `showActivationFailed()` với message chung chung (`vip_failed_message`, không nói
rõ lý do "cần mạng"). Đây là đánh đổi có chủ đích của SDK (comment ghi rõ "YÊU CẦU SẢN PHẨM, owner
chốt"), không phải bug — nhưng UX hiện tại không giải thích rõ cho user tại sao redeem fail lúc offline.
Cân nhắc đổi message hoặc thêm check mạng trước khi cho bấm "Activate" để tránh report nhầm "mã đúng
mà không kích hoạt được".

### 4. ⚪ THẤP — String mới thiếu bản dịch ở mọi locale khác

`attribution_ads_label`, `vip_no_reward_title`, `vip_no_reward_message` chỉ thêm vào
`app/src/main/res/values/strings.xml` (default/English), không có trong `values-vi-rVN`,
`values-ar`, v.v. (21 locale dir hiện có). Android tự fallback về default nên KHÔNG crash/lỗi build —
chỉ là gap i18n (user Việt sẽ thấy "No Reward This Time" bằng tiếng Anh giữa 1 màn hình đa số đã dịch).
Không chặn release.

## Live device test

Không thực hiện — không đủ tin cậy để tự điều khiển `adb`/tương tác ad thật trong phiên audit tĩnh này,
và bug #1 (build FAIL) đã đủ để chặn tiến độ, ưu tiên báo cáo ngay hơn là cố cài APK cũ (build trước đó,
trước khi thêm `UMP_DEBUG_GEOGRAPHY`) rồi suy diễn từ trạng thái không khớp working-tree hiện tại.

## Kết luận

Toàn bộ 13 claim fix ở `doc/AD.MD` mục 5.3 (F1,F2/13/22,F3,F4/F9,F6,F8,F12,F13/22,F14,F15,F18,F20,F24)
đối chiếu code thật đều **ĐÚNG**. QC-2 verify khớp 100% qua source SDK thật. QC-1 không verify được qua
code tĩnh (cần Console).

Nhưng **`assembleProductionRelease` hiện FAIL** — mâu thuẫn trực tiếp với claim "BUILD SUCCESSFUL" của
`doc/AD.MD` mục 5.1, do thiếu 2 `buildConfigField` (`UMP_DEBUG_GEOGRAPHY`/`UMP_TEST_DEVICE_HASH`) ở
`release`/`defaultConfig`. Đây là **blocker build thật, cần fix trước khi release** — không phải finding
lý thuyết. Thêm 1 finding TRUNG thật (dialog hiện sai số ngày VIP khi cộng dồn) do quá trình viết lại
`showActivationSuccess()` back-calculate thay vì dùng số ngày đã biết trước — cần fix trước khi ship vì
ảnh hưởng trực tiếp trải nghiệm/độ tin cậy hiển thị cho user trả phí.
