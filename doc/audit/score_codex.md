# Audit cuối + chấm điểm — CODEX — 2026-08-16

## Build status

- Lệnh bắt buộc nguyên văn `./gradlew assembleDevDebug assembleProductionRelease testDevDebugUnitTest compileDevDebugAndroidTestSources`: **FAIL** tại `testDevDebugUnitTest`; 320 tests chạy, 2 tests fail. Vì Gradle dừng tại unit test nên target `compileDevDebugAndroidTestSources` không chạy trong invocation này.
- `assembleDevDebug`: **PASS**.
- `assembleProductionRelease`: **PASS** (bao gồm R8/minify, shrink resources và lintVital).
- `testDevDebugUnitTest`: **FAIL**, tái lập lần hai với `--rerun-tasks`; fail tại `AdIntegrationTest.kt:84` và `:98`.
- `compileDevDebugAndroidTestSources`: **PASS** khi chạy riêng với `--rerun-tasks`.
- Đối chứng nguyên nhân: riêng class `AdIntegrationTest` chạy **PASS** (5/5), nhưng full suite luôn fail 2 test trên. Đây là test pollution/order-dependence, không phải dependency/cache cũ.

## Breakdown điểm

| Tiêu chí | Điểm /10 | Lý do ngắn |
|---|---:|---|
| Correctness | 8.0 | Luồng init, config, token/redeem, rewarded và VIP expiry khớp API thật của SDK tag 1.6.16; cả debug/release APK assemble được. Trừ điểm vì validation gate tổng hiện đỏ. |
| Security | 7.0 | Private signing key không nằm trong tracked files; public ECDSA key ship trong APK là đúng; `local.properties` được ignore và không tracked. Tuy nhiên HMAC integrity secret mới bị hardcode/commit trong `app/build.gradle`. |
| Policy compliance (F12 đặc biệt) | 9.0 | Chỉ callback `earned=true` của Rewarded gọi `grantVipFromAd`; nhánh Rewarded=false → Interstitial tuyệt đối không cấp VIP. Trừ điểm vì chưa có regression test trực tiếp cho nhánh policy-critical này. |
| Code quality | 8.0 | `AdSetup` tách Java/Kotlin constructor hợp lý, lifecycle cleanup và comment/rationale rõ; không thấy đường VIP dead/duplicate. Một số comment quá dài và test helper mirror implementation làm tăng drift risk. |
| Test coverage | 4.0 | Có test redeem/grant/UI và androidTest compile, nhưng full unit suite fail ổn định; thiếu test trực tiếp chứng minh Interstitial fallback không cấp VIP và phần pill-animation chủ yếu test bản sao helper thay vì code production. |

## ĐIỂM TỔNG: 6.0/10

Điểm tổng bị cap ở 6 vì release gate mà user yêu cầu hiện FAIL tái lập, dù APK production tự assemble thành công và chưa thấy lỗi runtime/policy blocker trong luồng code production.

## Finding còn sót (nếu có)

### C1 — TRUNG — Full unit-test suite bị phụ thuộc thứ tự và đang FAIL

- Vị trí: `app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementTest.kt:110`; `app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/AdIntegrationTest.kt:74-106`.
- Mô tả: test invalid-key trong `ActVipManagementTest` gọi thật `AdManager.activateVipByKey`, làm `vipActivationBackoff` của SDK vào cooldown. Cooldown này cố ý sống hết process và `clearAppPreferencesForTest()`/`destroy()` không reset. Sau đó hai redeem-code tests của `AdIntegrationTest` chạy trong cùng test worker nên nhận `false`. Bằng chứng: full suite fail hai lần; chạy riêng `AdIntegrationTest` thì pass toàn bộ.
- Ảnh hưởng: CI/release verification đỏ; báo cáo “test xanh” không phản ánh working tree hiện tại. Đây là lỗi test-suite, chưa thấy bằng chứng app production redeem sai khi process không vừa bị brute-force throttle.
- Patch đề xuất (không apply): cô lập class có invalid-key trong JVM riêng bằng một Gradle `Test` task/source-set riêng, hoặc cấu hình `forkEvery = 1` cho unit-test task nếu chấp nhận chi phí chạy chậm hơn. Phương án tốt hơn dài hạn là SDK export một test-only `@InternalAdApi resetVipActivationBackoffForTest()` cross-module; gọi trong `@Before`. Không sửa bằng cách reset cooldown ở production `destroy()` vì sẽ làm yếu chống brute-force.

### C2 — TRUNG — HMAC integrity secret mới bị commit trực tiếp trong source

- Vị trí: `app/build.gradle:39`.
- Mô tả: `VIP_KEY_SECRET` là chuỗi production thật hardcode trong tracked Gradle file. Source tag 1.6.16 của `AdSdkConfig` yêu cầu không hardcode secret thật trong source/repo, khuyến nghị inject từ CI environment hoặc `local.properties`. Secret tất nhiên vẫn hiện diện trong APK để client verify, nên đây không phải bí mật chống reverse-engineering tuyệt đối; nhưng commit làm tăng đáng kể phạm vi lộ (repo, fork, log, history) và khiến rotation khó hơn.
- Kiểm tra liên quan: `local.properties` đang được `.gitignore` bắt và không tracked; không thấy VIP private ECDSA key hay JitPack token literal trong tracked code. Public ECDSA verify key ở `BuildConfig` là đúng mô hình bất đối xứng.
- Patch đề xuất (không apply): đọc `vip.key.secret` từ `local.properties` hoặc `VIP_KEY_SECRET` từ environment tương tự JitPack token; fail-fast khi build release nếu thiếu/placeholder; chỉ cho debug dùng fallback test riêng. Sau khi đổi, rotate secret vì giá trị hiện tại đã nằm trong Git working history/diff.

### C3 — THẤP — F12 đúng nhưng chưa có regression test tại boundary thật

- Vị trí: `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:186-207`; các test hiện tại trong `ActVipManagementTest.kt` và `AdIntegrationTest.kt`.
- Mô tả: code hiện tại tuân thủ F12, nhưng không test case `Rewarded(false) → Interstitial(true) → VIP expiry không đổi`. `testGrantVipDays_rewardPath` chỉ chứng minh API grant hoạt động, không khóa invariant policy.
- Patch đề xuất (không apply): bọc các lệnh show ad bằng interface injectable hoặc tách decision callback thành coordinator nhỏ; dùng fake trả Rewarded=false/Interstitial=true và assert `grantVipDays` không được gọi, `getVipByKeyExpiry()` không đổi, dialog “No Reward” xuất hiện. Thêm case Rewarded=true gọi grant đúng một lần.

### C4 — THẤP — Test pill animation kiểm tra bản sao thay vì implementation thật

- Vị trí: `app/src/test/java/com/galaxyjoy/hexviewer/ui/act/ActMainPillAnimationTest.java:103+`; `app/src/androidTest/java/com/galaxyjoy/hexviewer/ui/ActMainPillAnimationInstrumentationTest.java:114+`.
- Mô tả: hai suite dùng helper tự mirror logic animation. Chúng có thể vẫn xanh khi implementation trong `ActMain` bị thay đổi/hỏng, nên coverage được mô tả mạnh hơn giá trị thực tế.
- Patch đề xuất (không apply): extract animation controller production thành class/package-visible dùng chung cho `ActMain` và tests, hoặc test trực tiếp Activity/view thật.

## Live device test (nếu có làm)

Không chạy live device test. Không cài APK debug lên thiết bị, vì vậy không cần khôi phục production/release và trạng thái máy không bị thay đổi.

## Kết luận 1 đoạn

Phần code production tích hợp SDK nhìn chung đúng API 1.6.16, F12 đã được khóa đúng ở logic và không thấy đường non-rewarded nào cấp VIP; release APK cũng build/minify thành công. Tuy nhiên chưa nên gọi toàn bộ changes là production-ready theo gate đã đặt ra: full unit suite hiện fail tái lập do static cooldown pollution, HMAC secret mới bị commit trực tiếp, và invariant F12 chưa có regression test thật. Sửa C1 và C2, bổ sung C3 rồi chạy lại nguyên lệnh bốn target là điều kiện hợp lý trước khi ship.
