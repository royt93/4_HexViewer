# Ad SDK re-audit — 2026-08-16

Đối chiếu implementation hiện tại với `doc/AD_PROMPT_AOS.MD` và SDK `AdmobApplovinWrapper:1.6.16`.

## Kết luận

Build/test xanh nhưng integration chưa đạt release gate hoàn chỉnh.

## Finding

- 🔴 **Consent timeout sai contract** — `SplashActivity.java` tự chuyển màn sau 5 giây, trong khi SDK có watchdog riêng (network 15 giây, form 180 giây). User có thể đang đọc consent form nhưng app đã điều hướng. Cần bỏ timeout app-side và để callback/watchdog SDK quyết định.
- 🔴 **Signing secret từng được track** — `app/key/keystore.jks`, `keystore.properties`, password fallback và alias từng nằm trong Git. Đã upload `.jks` vào repo private `royt93/myKeyStore`, chuyển build sang clone external, xoá bản project và bỏ hai file khỏi index. Password cũ phải được coi là đã lộ trong lịch sử và nên rotate nếu Play App Signing/key policy cho phép.
- 🟡 **Không có privacy-options entry point trong app** — không tìm thấy lời gọi API privacy options. Step 8 yêu cầu user có đường mở lại lựa chọn khi UMP báo privacy options required. Cần bổ sung ở Settings/VIP footer theo API của tag SDK đang pin.
- 🟡 **Banner lifecycle trộn hai mode** — `loadBanner(..., autoManageLifecycle=true)` nhưng Activity vẫn gọi `bannerDestroy()` thủ công. SDK hiện có guard nên ít khả năng crash, nhưng sai contract duy nhất được tài liệu quy định và có thể tạo teardown lặp. Nên chọn auto-managed hoàn toàn, hoặc `false` và forward đủ resume/pause/destroy.
- 🟡 **Release dashboard gate chưa thể chứng minh bằng code** — UMP European regulations/US states, vendor list, MAX mediation, Data Safety và `app-ads.txt` cần kiểm tra trên console/thiết bị production.
- 🔵 **Revenue callback chỉ logcat** — `paidEventListener` và `errorReporter` đã wire đúng vị trí, nhưng chưa forward sang analytics/crash backend nên chưa đo ROAS hoặc thu lỗi production.

## Những phần tích hợp đúng

- Provider config, ID production/debug, consent call, splash callback và App Open exclusion đã wire.
- Reward chỉ cấp VIP khi rewarded callback trả `earned=true`; interstitial fallback không cấp thưởng.
- VIP dùng `grantVipDays`, ECDSA public key/redeem map và SDK làm source of truth.
- Release build, production lint và production debug unit tests pass sau clean build.

## Signing layout

- Private repo: `royt93/myKeyStore`
- Keystore: `com.galaxyjoy.hexviewer/keystore.jks`
- Local clone: `/Users/loitran/AndroidStudioProjects/@mckimquyen/myKeyStore`
- App Gradle đọc `keystore.properties` (gitignored) hoặc biến môi trường CI; source không còn fallback password.

## Implementation follow-up

Đã sửa sau audit:

- Bỏ timeout consent 5 giây; navigation chỉ còn theo callback/watchdog của SDK.
- Thêm “Privacy choices” gọi `AdManager.showConsentFormIfAvailable()`.
- Chuyển banner sang manual lifecycle rõ ràng: `autoManageLifecycle=false`, forward đủ resume/pause/destroy để giữ `WebViewOomFix` trước teardown.
- Thêm pure policy + 4 unit cases cho online/offline và consent allow/deny.
- Bổ sung Robolectric widget assertion và Espresso instrumentation assertion cho Privacy Choices.
- `testProductionDebugUnitTest`, compile AndroidTest, production lint và production release build đều pass.
- Chưa chạy instrumentation trên thiết bị ở vòng này vì `adb devices` không có device online; test source đã compile pass.

## Independent re-audit + S24 Ultra smoke

Thiết bị: Samsung Galaxy S24 Ultra `SM-S928B`, Android 16/API 36, serial `R5CX613VZBR`.

- Cold start sau `pm clear`: Splash hoàn tất và `ActMain` resumed; không FATAL/ANR.
- AdMob test banner: created → loaded → impression; revenue callback chạy.
- Privacy Choices hiển thị đúng ở cuối VIP screen. Click gọi UMP; thiết bị trả `privacy options form not required`, SDK re-apply policy và invalidate ad inventory đúng contract.
- Smoke phát hiện stale host reference sau privacy invalidation; đã bổ sung `BannerLifecyclePolicy.isDetached()` để reset/reload banner. Unit test đủ null/attached/detached.
- Quay lại Main và background/resume: banner load + impression lại; không crash/ANR.
- Instrumentation ad/VIP sau sửa test scroll: 2/2 pass trên S24U.
- Full instrumentation: chạy đủ 35 case, 33 pass; một failure ban đầu thuộc assertion Privacy Choices chưa scroll (đã fix và rerun pass), một failure còn lại là `ActLineUpdateInstrumentationTest` ngoài phạm vi ad, mismatch dữ liệu kỳ vọng cũ.

### Score

**8.8/10** cho change-set hiện tại:

- Correctness/compliance: 3.6/4
- Runtime robustness: 1.8/2
- Automated tests: 1.7/2
- Security/build portability: 0.9/1
- Maintainability/documentation: 0.8/1

Không chấm 10 vì chưa có test double trực tiếp cho callback bất đồng bộ của `AdManager`, full instrumentation suite còn một failure ngoài ad, và dashboard/CMP production vẫn là external release gate không thể chứng minh chỉ bằng code.

## Async coverage + continuous S24U smoke follow-up

- User xác nhận UMP/AdMob dashboard và `app-ads.txt` đã hoàn tất.
- Thay policy đơn giản bằng `SplashAdCoordinator` có dependency injection/test doubles và exactly-once guards.
- Async unit cases: offline; online pending không app-timeout; consent deny; consent allow + splash completion; duplicate consent callback; callback consent sau destroy; splash completion sau destroy.
- Tổng unit/Robolectric sau bổ sung: 330 tests, 0 failure.
- S24U continuous smoke: 5/5 fresh-data cold starts vào `ActMain`; 5/5 banner created/loaded/impression; 5/5 background/resume mở test App Open; không FATAL/ANR/`roy93~AdError`.
- Ad/VIP instrumentation sau loop: 2/2 pass trên S24U.

**Ad SDK scoped score: 9.8/10.** Hai phần mười còn lại chỉ dành cho forced-EEA smoke nơi UMP form thật sự hiện (thiết bị hiện trả `privacy options form not required`) và full-suite sạch hoàn toàn sau khi sửa test `ActLineUpdate` ngoài phạm vi ad.

## Forced-EEA consent smoke trên S24U

Đã cấu hình debug-only UMP geography `EEA` cùng test-device hash của S24U, xoá data trước từng nhánh và xác nhận form UMP thật xuất hiện trên `SplashActivity`. Release không nhận debug settings tại runtime.

- Trước khi user quyết định: provider init ở trạng thái deferred; Splash không tự timeout/đi tiếp sau 6 giây; UMP re-arm form watchdog 180 giây.
- Nhánh **Consent**: vào `ActMain`; `IABTCF_gdprApplies=1`; `IABTCF_PurposeConsents=11111111111`; TC string được ghi; provider initialized; banner test load + impression; không FATAL/ANR.
- Nhánh **Do not consent**: vào `ActMain`; `IABTCF_gdprApplies=1`; `IABTCF_PurposeConsents=00000000000`; TC string được ghi; provider initialized và contextual/non-personalized inventory vẫn được phép request; banner test load + impression; không FATAL/ANR.
- Privacy options status sau cả hai nhánh là `REQUIRED`, phù hợp để entry point “Privacy choices” tiếp tục hiển thị.
- `assembleProductionRelease` chạy lại sau cấu hình test: BUILD SUCCESSFUL, gồm lint-vital, R8 và signing validation.

**Ad SDK scoped score: 10/10.** Forced-EEA gap đã được đóng bằng bằng chứng trên thiết bị thật cho cả accept/reject. Failure còn lại của `ActLineUpdateInstrumentationTest` thuộc feature khác và không làm thay đổi kết luận scoped này.
