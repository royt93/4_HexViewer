# Audit cuối + chấm điểm — GEMINI — 2026-08-16

## Build status
- `./gradlew assembleDevDebug`: **PASS** (APK dev debug build thành công)
- `./gradlew assembleProductionRelease`: **PASS** (R8 minification, ProGuard shrink, lintVital check toàn bộ PASS, APK production release sạch)
- `./gradlew compileDevDebugAndroidTestSources`: **PASS** (Biên dịch toàn bộ AndroidTest sources thành công)
- `./gradlew testDevDebugUnitTest`:
  - Chạy standalone `AdIntegrationTest`: **PASS** (5/5 tests pass)
  - Chạy trọn bộ suite với `--rerun-tasks`: **FAIL (318/320 pass, 2 fail do test-order pollution)**
    - `AdIntegrationTest.testVipActivationWorkflow_viaRedeemCode` (line 84)
    - `AdIntegrationTest.testVipActivation_with3DayRedeemCode` (line 98)
    - *Nguyên nhân chi tiết & patch đề xuất được phân tích ở mục Finding còn sót bên dưới.*

## Breakdown điểm
| Tiêu chí | Điểm /10 | Lý do ngắn |
|---|---|---|
| Correctness | 9.0/10 | Logic nghiệp vụ, vòng đời Activity, quản lý bộ nhớ/animator/dialog và tích hợp API SDK 1.6.16 (`AdManager`, `AdSdkConfig`, `grantVipDays`, `activateVipByKey`, `WebViewOomFix`) hoàn toàn chính xác. Trừ 1 điểm do cross-test state pollution khi chạy full test suite. |
| Security | 9.5/10 | Tách biệt hoàn toàn `VIP_KEY_SECRET` (HMAC anti-tamper) và `VIP_TOKEN_PUBLIC_KEY` (ECDSA P-256). Private key và JitPack token giữ trong `local.properties` (đã verify gitignored). Redeem codes obfuscated Base64 chống grep APK. |
| Policy compliance (F12 đặc biệt) | 10.0/10 | Tuân thủ tuyệt đối chính sách Google Play & AppLovin Rewarded Ads: Nhánh fallback Interstitial trong `ActVipManagement.kt` chỉ hiện thông báo `showNoRewardDialog()`, tuyệt đối KHÔNG cấp VIP khi xem non-rewarded ad (F12 đã bít 100%). SplashActivity được exclude khỏi App Open ad. |
| Code quality | 9.5/10 | Cấu trúc code sạch, phân tách rõ ràng Kotlin/Java bridge (`AdSetup.kt`, `TestAdSdkConfigFactory.kt`), tài liệu KDoc chi tiết, quản lý lifecycle triệt để trong `onDestroy()` chống `WindowLeaked` và memory leak. |
| Test coverage | 8.5/10 | Bộ test bao phủ đầy đủ unit test & androidTest (UI states, VIP flows, countdown, edge cases, pill animation). Tuy nhiên bộ test unit cần cấu hình `forkEvery = 1` hoặc reset backoff để tránh false failure do cross-test JVM sharing. |

## ĐIỂM TỔNG: 9.3/10

---

## Finding còn sót (nếu có)

### Finding 1: Cross-test backoff cooldown pollution giữa `ActVipManagementTest` và `AdIntegrationTest`
- **Vị trí**: `app/src/test/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagementTest.kt:117` và `app/build.gradle:182`
- **Mức độ**: TRUNG BÌNH (Chỉ ảnh hưởng test suite khi chạy full batch, không ảnh hưởng production code).
- **Mô tả chi tiết**:
  1. Trong `ActVipManagementTest.kt` (dòng 111-131), test case `testActivateButton_whenInvalidKey_showsFailureDialog` nhập mã sai `"wrong_key_123"` và nhấn Activate.
  2. Việc này gọi `AdManager.activateVipByKey(context, "wrong_key_123", 0)`, kích hoạt cơ chế chống brute-force `vipActivationBackoff.onFailure()` trong singleton `AdManager`.
  3. `vipActivationBackoff` là static state tồn tại suốt vòng đời của JVM worker Gradle.
  4. Khi Gradle chạy tiếp `AdIntegrationTest.kt` trong cùng JVM worker, lệnh `AdManager.activateVipByKey(context, VipKeys.VIP_30D_KEY, 0)` rơi vào nhánh `if (vipActivationBackoff.isInCooldown())` và trả về `false`, làm cho 2 test case (`testVipActivationWorkflow_viaRedeemCode`, `testVipActivation_with3DayRedeemCode`) bị FAIL oan.
  5. Khi chạy `AdIntegrationTest` độc lập (`./gradlew testDevDebugUnitTest --tests "...AdIntegrationTest"`), 100% test case PASS xanh.

- **Patch đề xuất (chọn 1 trong 2 giải pháp)**:
  - *Giải pháp 1 (Gradle level)*: Thêm cấu hình fork test worker cho unit test trong `app/build.gradle`:
    ```groovy
    testOptions {
        unitTests {
            includeAndroidResources = true
            all {
                forkEvery = 1
            }
        }
    }
    ```
  - *Giải pháp 2 (Test level)*: Trong `ActVipManagementTest.kt`, tránh trigger activation thực tế của SDK với mã sai hoặc giả lập riêng biệt để không kích hoạt backoff tĩnh.

---

## Live device test
- **Thiết bị**: OPPO CPH1989 (Android 11, ID `FUJZIFIR7DQCNRWW`).
- **Trạng thái thực hiện**:
  1. Đã cài đặt và khởi chạy `app-dev-debug.apk`: `SplashActivity` và `ActMain` khởi động mượt mà, logcat ghi nhận log khởi tạo hệ thống và ngôn ngữ chuẩn (`roy93~: [Application] Default system locale: 'vi-VN'`, `[Main] Application started with language: 'vi-VN'`).
  2. Đã xác nhận `ActVipManagement` cấu hình `android:exported="false"` an toàn (chặn triệt để bên thứ 3 khởi chạy trực tiếp qua Intent).
  3. **Dọn dẹp hoàn tất**: Đã gỡ bỏ bản debug và cài đặt lại bản **`app-production-release.apk`** sạch lên thiết bị, sẵn sàng cho các đợt kiểm thử tiếp theo.

---

## Kết luận 1 đoạn
Toàn bộ mã nguồn tích hợp Ad SDK và VIP Management trong đợt thay đổi này đạt chất lượng cao, cấu trúc chặt chẽ, bảo mật nghiêm ngặt và tuân thủ hoàn toàn các chính sách quảng cáo (đặc biệt là F12). Các lỗi build, memory leak và vòng lặp recreate trước đó đều đã được xử lý triệt để. Code đã hoàn toàn sẵn sàng để phát hành lên production (Production-Ready) sau khi áp dụng tinh chỉnh nhỏ về cấu hình cách ly worker của Unit Test suite.
