# Audit vòng 4 (trước push) — CODEX — 2026-08-16

## Build status (bao gồm kết quả 2-3 lần chạy testDevDebugUnitTest liên tiếp)

- Full gate `./gradlew assembleDevDebug assembleProductionRelease testDevDebugUnitTest compileDevDebugAndroidTestSources --rerun-tasks`: **PASS** khi chạy với build output cô lập; `BUILD SUCCESSFUL`, 110 tasks thực thi. Cả debug APK, production release (R8/resource shrink), unit tests và androidTest sources đều hoàn tất.
- `testDevDebugUnitTest --rerun-tasks` lần 1 (trong full gate): **PASS**, 320 tests, 0 failures, 0 errors.
- `testDevDebugUnitTest --rerun-tasks` lần 2: **PASS**, 320 tests, 0 failures, 0 errors (`BUILD SUCCESSFUL in 1m17s`).
- `testDevDebugUnitTest --rerun-tasks` lần 3: **PASS**, 320 tests, 0 failures, 0 errors (`BUILD SUCCESSFUL in 1m36s`).
- Kết luận về fix flaky: **đã xác nhận hết flaky trong 3/3 lần liên tiếp**. `forkEvery = 1` thực sự tạo JVM mới theo test class, đúng với nguyên nhân `vipActivationBackoff` static sống hết process của SDK.
- Ghi chú môi trường: invocation đầu tiên trên output mặc định bị lỗi ghi đồng loạt XML test-result vì hai auditor độc lập khác đang chạy Gradle cùng lúc trên chính workspace/output directory. Kiểm tra process xác nhận có nhiều `GradleWrapperMain testDevDebugUnitTest` đồng thời; disk còn 72 GiB và permission bình thường. Chạy lại với build directory/project cache riêng cho CODEX cho kết quả 3/3 PASS ở trên. Đây là collision hạ tầng audit, không phải test assertion hay flaky của code.

Đã đọc toàn bộ diff, đối chiếu trực tiếp source GitHub tag SDK `1.6.16` bằng `gh api`, gồm `AdSdkConfig.kt`, `AdManager.kt`, `AppPreferences.kt` và test hooks. Các API/semantics app đang dùng (`activateVipByKey`, redeem map, `grantVipDays`, `getVipGrantedAtMs`, rewarded callback và cooldown) khớp source thật.

## Breakdown điểm

| Tiêu chí | Điểm /10 | Lý do ngắn |
|---|---:|---|
| Correctness | 9.5 | Full gate xanh và unit suite xanh 3/3; API SDK 1.6.16 dùng đúng; token/redeem/grant/expiry có single source of truth. Trừ nhẹ vì chưa live-test ad network trong vòng này. |
| Security | 7.0 | Private signing key/JitPack token không thấy trong tracked files; `local.properties` được ignore và không tracked; public ECDSA verify key ship trong APK là đúng. Tuy nhiên HMAC integrity secret production vẫn hardcode trong tracked Gradle source. |
| Policy compliance (F12) | 9.5 | Chỉ callback Rewarded `earned == true` gọi `grantVipFromAd`; nhánh `earned == false` chỉ fallback Interstitial và hiển thị no-reward/no-ad, tuyệt đối không grant VIP. Trừ nhẹ vì invariant này chưa có regression test trực tiếp tại callback boundary. |
| Code quality | 8.8 | `AdSetup` tách cấu hình hợp lý, lifecycle cleanup rõ, dead field native đã xóa, repository fallback được gate đúng. Comment audit khá dày và `forkEvery=1` làm suite chậm nhưng có rationale kỹ thuật chính đáng. |
| Test coverage | 8.2 | 320 unit tests xanh ổn định, có test SDK thật cho redeem/grant và androidTest compile. Thiếu automated test cho chuỗi policy-critical Rewarded(false) → Interstitial(true) → VIP expiry không đổi; pill-animation tests vẫn chủ yếu mirror helper. |

## ĐIỂM TỔNG: 8.6/10

## Finding còn sót (nếu có)

### S1 — TRUNG — HMAC integrity secret production đang nằm trong tracked source

- Vị trí: `app/build.gradle`, field `VIP_KEY_SECRET` trong `defaultConfig`.
- Source SDK tag 1.6.16 ghi rõ `vipKeySecret` dùng HMAC chống tamper cho VIP state và khuyến nghị **không hardcode secret thật trong source/repo**, mà inject từ CI environment hoặc `local.properties`.
- Đây không phải private key ECDSA và cũng không thể là bí mật tuyệt đối khi cuối cùng phải nằm trong APK; tuy nhiên commit literal làm tăng phạm vi lộ qua repo/fork/log/history, giảm khả năng quản trị và rotation. Giá trị hiện tại chưa nằm trong commit HEAD nhưng đang ở diff chuẩn bị push, nên đây là thời điểm tốt nhất để chặn nó đi vào history.
- Patch đề xuất (không apply): đọc `vip.key.secret` từ `local.properties` hoặc `VIP_KEY_SECRET` từ environment; escape an toàn khi tạo `buildConfigField`; release build fail-fast nếu thiếu/placeholder, debug có thể dùng secret test riêng. Rotate giá trị hiện tại sau khi chuyển vì nó đã xuất hiện trong working diff/report audit cũ.

### T1 — THẤP — Chưa có regression test trực tiếp cho invariant F12

- Vị trí: `ActVipManagement.kt:186-207`.
- Code hiện tại đúng: `earned=true` là đường duy nhất tới `grantVipFromAd`; Interstitial callback không grant. Nhưng test `testGrantVipDays_rewardPath` chỉ kiểm tra primitive grant, không chứng minh callback non-rewarded không gọi grant.
- Patch đề xuất (không apply): tách decision flow ra coordinator/injectable ad facade; fake Rewarded(false), Interstitial(true), rồi assert expiry không đổi và no-reward UI được gọi. Thêm đối chứng Rewarded(true) grant đúng một lần. SDK test hooks hiện là `internal`, nên app module không fake provider trực tiếp được.

### Xác nhận các fix trọng tâm vòng 3

- `settings.gradle`: **đúng**. JitPack được khai trước; credentials chỉ set khi token khác null; `mavenLocal()` chỉ được thêm trong `if (jitpackToken == null)`. Khi có token thật, artifact local không còn tham gia resolution.
- `forkEvery = 1`: **đúng và hiệu quả**, unit suite PASS 3/3 lần liên tiếp với `--rerun-tasks`.
- `ADMOB_NATIVE_ID_UNUSED`: đã xóa; search toàn source không còn reference/field chết này.
- Secret scan: không thấy private ECDSA key, PEM private key, JitPack token literal hoặc tracked `local.properties`. Public ECDSA key là dữ liệu verify, không phải secret.

## Live device test (nếu có làm)

Không thực hiện live-device test trong vòng này. Không cài debug APK lên thiết bị `FUJZIFIR7DQCNRWW`, nên không cần khôi phục production/release và trạng thái thiết bị không bị thay đổi.

## Kết luận 1 đoạn — có khuyến nghị PUSH hay KHÔNG PUSH

Hai fix bắt buộc của vòng 3 đã được xác minh đúng: repository provenance không còn bị `mavenLocal` lấn khi có token, và full unit suite hết flaky qua 3/3 lần chạy thật; full build gate cũng xanh. Logic F12 hiện tuân thủ chính sách, API SDK 1.6.16 khớp source thật và không phát hiện bug correctness mới. Tuy vậy, **khuyến nghị CHƯA PUSH ngay** vì `VIP_KEY_SECRET` production vẫn là literal trong tracked `app/build.gradle`, trái hướng dẫn bảo mật của chính SDK và rất dễ tránh trước khi nó vào Git history. Inject secret từ environment/`local.properties`, rotate giá trị, chạy lại full gate một lần; sau đó có thể PUSH. Gap test F12 là cải thiện nên làm sớm nhưng không phải blocker correctness của patch hiện tại.
