# Audit cuối + chấm điểm — claude2 — 2026-08-16

## Build status

```
./gradlew assembleDevDebug assembleProductionRelease testDevDebugUnitTest compileDevDebugAndroidTestSources
BUILD SUCCESSFUL in 965ms — 110 actionable tasks (1 executed, 109 up-to-date)
```
PASS toàn bộ 4 target. Không lỗi biên dịch, không test fail.

## Phương pháp

Đọc toàn bộ diff thật (`git diff` 27 file, +1069/-241). Đối chiếu API SDK dùng trong app (`AdSetup.kt`,
`ActVipManagement.kt`, `VipKeys.kt`, `VipPrefs.kt`) với **source thật** tại tag `1.6.16` kéo qua
`gh api repos/royt93/AdmobApplovinWrapper/contents/...` — không suy đoán qua doc. Verify field-by-field
`AdSdkConfig.kt`, hàm `activateVipByKey`/`grantVipDays`/`isVipByKeyActive`/`getVipGrantedAtMs` trong
`AdManager.kt`, và write-path `saveVipGrantedAtMs` trong `AppPreferences.kt`. Kiểm tra `local.properties`
không bị track/commit. Không chạy live device test (build + code audit đã đủ tin cậy; live device đã
được làm kỹ ở QC mục 6 doc/AD.MD phiên trước — không lặp lại).

## Breakdown điểm

| Tiêu chí | Điểm /10 | Lý do ngắn |
|---|---|---|
| Correctness | 9 | API SDK dùng đúng 100% so với source thật tag 1.6.16 (verify từng field/hàm). Business logic VIP (resolve order token→redeem→legacy-off, atomic accumulate, grantedAtMs semantics) khớp chính xác với `AppPreferences.kt` thật — kể cả comment giải thích "cộng dồn ghi đè grantedAtMs" trong `ActVipManagement.kt:505-511` đúng 100% so với `accumulateVipByKeyUntil`/`markRedeemAndSaveVipAtomic` (dòng 701-727 `AppPreferences.kt`). Trừ điểm nhẹ vì `ADMOB_NATIVE_ID_UNUSED` là dead code (tự thừa nhận trong comment, SDK không có API native). |
| Security | 7 | `local.properties` gitignored + không có trong git log (xác nhận `git ls-files`/`git log --all`). `VIP_KEY_SECRET` không phải default SDK (`TPLdev0402dev@`), đủ ≥16 ký tự. `vipTokenPublicKey` **không phải** cặp khoá mẫu công khai của SDK (đã diff so với `DEFAULT_VIP_TOKEN_PUBLIC_KEY` trong source thật) — quan trọng, vì dùng khoá mẫu sẽ cho phép forge token bằng private key mẫu công khai trong repo SDK. Trừ điểm vì `settings.gradle` thêm `mavenLocal()` ưu tiên TRƯỚC `jitpack` — xác nhận máy dev hiện có sẵn `~/.m2/repository/com/github/royt93/AdmobApplovinWrapper/1.6.16/` nên **build thật đang resolve từ cache local, không phải JitPack** — không checksum nào ràng buộc 2 nguồn này khớp nhau, rủi ro provenance nếu cache local từng bị ghi đè bởi build khác. Xem finding bên dưới. |
| Policy compliance (F12 đặc biệt) | 9 | Đọc kỹ `ActVipManagement.kt:187-208`: nhánh `earned=false` CHỈ gọi `showInterstitial` fallback, `onDoneFlow` không có đường nào gọi `grantVipFromAd()`/`AdManager.grantVipDays` — không còn cách nào lách cấp VIP qua ad non-rewarded. Khớp với live-device QC đã PASS ở phiên trước (doc/AD.MD mục 6.8). Trừ 1 điểm vì finding QC-1 (mục 6.2 doc/AD.MD) — AdMob Console chưa xác nhận Published European regulations message cho App ID mới — vẫn CHƯA đóng, dù đây là item vận hành (console), không phải code; code đã fail-closed đúng thiết kế (`blockLoadUntilConsent=true`, không bị override ở đâu trong `AdSetup.kt`) nên rủi ro thực tế là mất doanh thu EEA chứ không phải vi phạm GDPR thật. |
| Code quality | 8 | Comment tiếng Việt dày đặc nhưng có giá trị thật (trace ngược audit F-number, giải thích WHY không phải WHAT). Đặt tên rõ (`grantVipFromAd`, `showNoRewardDialog`). Cấu trúc tách `AdSetup.kt` khỏi `MyApplication.java` hợp lý (giải quyết đúng vấn đề Java gọi Kotlin data class 30+ field không `@JvmOverloads`). Nit nhỏ: `ADMOB_NATIVE_ID_UNUSED` là field chết ngay từ lúc thêm — nên bỏ khỏi `build.gradle` cho tới khi thật sự cần, thay vì giữ "phòng khi SDK thêm hỗ trợ" (YAGNI). |
| Test coverage | 7 | Test tích hợp THẬT với SDK thật (không mock `AdManager`) — `AdIntegrationTest`, `ActVipManagementTest`, instrumentation test đều gọi `activateVipByKey`/`grantVipDays` thật rồi assert state qua `isVipByKeyActive()`/`getVipByKeyExpiry()` thật, không phải test biên dịch suông. `TestNetworkUtils` xử lý đúng gotcha Robolectric (V-03 network gate). Trừ điểm: **không có test tự động nào click `btnWatchAd` rồi verify nhánh `earned=false → showInterstitial → KHÔNG gọi grantVipDays`** — đây chính là logic F12-critical nằm trong `ActVipManagement.kt`, chỉ được xác minh bằng đọc code + 1 lần live-device QC thủ công (không có regression net). Đã kiểm tra `AdManagerTestHooks.kt` thật: `setProviderForTest`/`AdProvider` đều `internal` (module-private của SDK) — `:app` không cách nào fake `AdProvider` để mock `showRewarded`/`showInterstitial` callback, nên đây là giới hạn SDK áp đặt, không phải lười viết test. |

## ĐIỂM TỔNG: 8/10

## Finding còn sót

1. **[TRUNG — build provenance] `settings.gradle` dòng ~13-25**: `mavenLocal()` đặt trước `jitpack.io` trong danh sách repository. Xác nhận thực tế: `~/.m2/repository/com/github/royt93/AdmobApplovinWrapper/1.6.16/` đã tồn tại trên máy này → Gradle build vừa chạy **thực sự đang lấy artifact từ mavenLocal, không phải JitPack**, dù comment trong file nói mavenLocal chỉ là fallback "khi chưa có jitpack.token". Không có checksum nào ràng buộc 2 nguồn phải khớp nhau — nếu artifact local từng bị publish sai/cũ/tamper (vd `publishToMavenLocal` chạy nhầm version khác lúc dev), build sẽ âm thầm dùng bản đó mà không cảnh báo, kể cả khi `jitpack.token` đã có sẵn.
   **Patch đề xuất**: đảo thứ tự — đặt `mavenLocal()` SAU `jitpack` (chỉ dùng khi jitpack thật sự lỗi), hoặc gate `mavenLocal()` bằng chính điều kiện `jitpackToken == null` (mirror đúng logic comment đã viết):
   ```groovy
   repositories {
     google()
     mavenCentral()
     maven {
       url "https://jitpack.io"
       if (jitpackToken != null) { credentials { username = jitpackToken } }
     }
     if (jitpackToken == null) { mavenLocal() } // chỉ fallback khi thật sự chưa có token
   }
   ```

2. **[THẤP — test coverage gap, không tự sửa được từ `:app`]** `ActVipManagement.kt:187-208` (nhánh `earned=false → showInterstitial → showNoRewardDialog`, tim của F12) không có test tự động. Đã xác nhận `AdProvider`/`setProviderForTest` trong SDK là `internal` — `:app` module không có seam để fake callback `showRewarded`/`showInterstitial`. Không phải bug, không chặn release, nhưng đáng ghi nhận làm input cho SDK version sau: xin `@InternalAdApi` cho `setProviderForTest` + public `AdProvider` interface (hoặc 1 fake-provider chính thức) để downstream test được đúng nhánh chính sách quan trọng nhất mà không phải trông cậy QC thủ công trên thiết bị thật mỗi lần.

3. **[GHI NHẬN, không phải bug — đã biết từ phiên trước]** QC-1 (doc/AD.MD mục 6.2): AdMob Console chưa xác nhận Published European regulations message cho App ID `ca-app-pub-3004713799155145~6239626749`. Ngoài phạm vi code, không tự fix được bằng patch — cần user vào apps.admob.com xác nhận. Code hiện tại đã fail-closed đúng (`blockLoadUntilConsent=true` mặc định, không override), nên rủi ro thực tế là mất ad EEA chứ không vi phạm GDPR ngay — nhưng vẫn nên đóng trước khi ship thật để không mất doanh thu thị trường đó.

## Live device test

Không thực hiện trong phiên audit này — quyết định có chủ đích: F12 (finding chính sách quan trọng nhất)
đã được 1 agent độc lập trước đó verify trực tiếp trên thiết bị thật `FUJZIFIR7DQCNRWW` (doc/AD.MD mục
6.8, PASS, "xác nhận trực tiếp"). Audit lần này tập trung vào việc phần code MỚI đổi kể từ phiên đó (chủ
yếu string/layout/test-suite nhỏ + `settings.gradle` mavenLocal) không phá vỡ finding đã đóng — đọc diff
xác nhận không file nào trong `ActVipManagement.kt`/`AdSetup.kt`/`VipKeys.kt` có thay đổi hành vi F12 kể
từ lần QC live cuối. Không cài lại APK lên máy nên không cần khôi phục bản release.

## Kết luận

Code tích hợp Ad SDK ở trạng thái production-ready: build xanh cả 4 target, API SDK dùng đúng 100% so
với source thật 1.6.16 (verify trực tiếp qua GitHub, không suy đoán), đường VIP đã đóng đúng mọi lỗ hổng
mà 2 vòng audit trước tìm ra (F12/F13/F14/F18/F22 đều đối chiếu lại và xác nhận fix đúng), secret/key
không lộ trong git. Vấn đề còn lại là 1 finding TRUNG thật sự mới (mavenLocal ưu tiên sai thứ tự, ảnh
hưởng build provenance chứ không phải app runtime) đáng sửa trước khi giao build cho người khác build
lại từ máy sạch, cộng 1 gap test coverage bị giới hạn bởi chính SDK chứ không phải do team lười. Không có
gì chặn release ngay lập tức; khuyến nghị xử lý finding #1 (mavenLocal ordering) và đóng QC-1 (AdMob
Console) trước khi public release thật ra Play Store.
