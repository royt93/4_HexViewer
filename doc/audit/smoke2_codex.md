# Smoke-test vòng 2 — CODEX — 2026-08-16

## Build status

- `assembleDevDebug`: **PASS**. Chạy lại bằng `./gradlew assembleDevDebug --rerun-tasks`; 37 task thực thi, `BUILD SUCCESSFUL in 6s`.
- `assembleProductionRelease`: **FAIL** tại `:app:compileProductionReleaseKotlin`. `AdSetup.kt:48-49` tham chiếu `BuildConfig.UMP_DEBUG_GEOGRAPHY` và `BuildConfig.UMP_TEST_DEVICE_HASH`, nhưng hai field chỉ được khai báo trong build type `debug` (`app/build.gradle:137-141`), nên release không sinh các symbol này. Các lỗi type-mismatch còn lại tại `AdSetup.kt:24-50` là lỗi dây chuyền của Kotlin compiler sau hai `Unresolved reference`. Đây **không phải** lỗi resolve SDK/JitPack; artifact 1.6.16 tồn tại trong `~/.m2` và dependency đã qua bước resolve.

## Xác nhận claim cũ (doc/AD.MD mục 5.3, 6.2, 6.4)

| Claim | Kết quả | Bằng chứng |
|---|---|---|
| F1 — SDK 1.6.16 | **ĐÚNG** | `app/build.gradle:223-224` |
| F2/F13/F22 — secret riêng, reward dùng API tin cậy | **ĐÚNG** | `app/build.gradle:37-43`; `AdSetup.kt:36-42`; `ActVipManagement.kt:232-239` gọi `grantVipDays(this, 3)` |
| F3 — public key ECDSA riêng, private key không đóng gói | **ĐÚNG** với phần code có thể kiểm tra | `app/build.gradle:40-43`; `AdSetup.kt:38-39`. Public key là X.509 EC P-256 hợp lệ theo API 1.6.16; không thấy private key trong diff/tracked source. |
| F3 — excluded activity/privacy URL | **ĐÚNG** | `AdSetup.kt:43-45` |
| F3 — paid/error callbacks | **ĐÚNG** | `AdSetup.kt:55-66`; được gọi từ `MyApplication.java:597-599` |
| F4/F6/F9 — production IDs, AdMob bật, manifest App ID | **ĐÚNG** | `app/build.gradle:103-114,131-135`; `AndroidManifest.xml:42-45` |
| F5 — scaffold test-device IDs | **ĐÚNG** nhưng hiện rỗng | `app/build.gradle:45-49`; `AdSetup.kt:71-78` |
| F8 — attribution | **ĐÚNG** | `AndroidManifest.xml:19-23`; `res/values/strings.xml:242` |
| F12 — fallback interstitial không cấp VIP | **ĐÚNG** | `ActVipManagement.kt:178-190`; chỉ nhánh `earned=true` gọi `grantVipFromAd()` |
| F14 — timestamp dùng SDK làm source of truth | **ĐÚNG** | `VipPrefs.kt:5-14`; `ActVipManagement.kt:245-250` |
| F15 — reward không đánh dấu redeem | **ĐÚNG** | `ActVipManagement.kt:232-239`; `markUserRedeemed()` chỉ còn ở nhánh nhập key `:144-148` |
| F18 — SDK tự resolve token/redeem code | **ĐÚNG** | `ActVipManagement.kt:141-145`; `VipKeys.kt:21-26`; source 1.6.16 `AdManager.kt:3550+` verify ECDSA trước rồi `tryActivateRedeemCode` |
| F20/F24 — cleanup | **ĐÚNG** | diff xoá `AdSize` khỏi `ActMain.java`/`ActRecentlyOpen.java` và TODO khỏi `MyApplication.java` |
| Claim mục 5.1 release build xanh | **SAI ở working tree hiện tại** | Build release mới FAIL như mục Build status; nguyên nhân là QC hook UMP thêm sau claim 5.1. |
| QC-1 — UMP publisher misconfiguration | **KHÔNG XÁC ĐỊNH trạng thái console hiện tại; diễn giải cũ hợp lý** | Code dùng đúng App ID mới tại `AndroidManifest.xml:43-45`. Lỗi `no form(s) configured for the input app ID` trong log mục 6.2 phù hợp với message chưa publish/propagate, nhưng audit read-only này không có bằng chứng mới từ AdMob Console để xác nhận nó vẫn còn. Không thấy code fix nào có thể tự sửa cấu hình console. |
| QC-2 — watchdog tạo exception không crash | **ĐÚNG** | Source SDK 1.6.16 `AdManager.kt:1469,1484-1498` arm timer 30s, dựng `IllegalStateException`, route qua `reportError`, rồi flush callback false và giữ provider fail-closed; `AdSetup.kt:55-57` chỉ `Log.e`, không rethrow. Nếu sau này forward mọi throwable sang Crashlytics thì sẽ tạo non-fatal noise. Báo cáo cũ không bỏ sót tác động code đáng kể. |

## Bug/regression MỚI tự tìm (nếu có)

1. **CAO — release không compile** — `AdSetup.kt:48-50`, `app/build.gradle:137-141`. QC hook được đọc từ source dùng chung nhưng BuildConfig chỉ tồn tại ở debug. Patch đề xuất: khai báo hai field rỗng trong `defaultConfig` (và chỉ override ở `debug`), hoặc khai báo giá trị rỗng tương ứng trong `release`. Sau đó chạy lại cả hai assemble, đặc biệt R8 release.

2. **TRUNG — double-tap Activate để lại dialog không thể đóng** — `ActVipManagement.kt:127-154`. Mỗi tap tạo/show `progressDialog` mới; tap sau chỉ remove runnable cũ (`:136`) nhưng không dismiss dialog cũ. Runnable mới chỉ dismiss dialog mới (`:139`), nên dialog cũ `setCancelable(false)` có thể che UI vô hạn tới khi Activity bị recreate/destroy. Patch đề xuất: disable nút ngay khi bắt đầu; hoặc giữ `progressDialog` thành field và dismiss dialog/runnable cũ trước khi tạo request mới, đồng thời cleanup trong `onDestroy`.

3. **THẤP — dialog thành công có thể báo sai số ngày vừa kích hoạt** — `ActVipManagement.kt:487-502`. Sau rewrite, số ngày được suy từ `expiry - grantedAt`; SDK 1.6.16 cộng dồn redeem code/reward vào hạn hiện có nhưng ghi `grantedAt=now`, nên khi gia hạn một VIP đang active, dialog báo tổng số ngày còn lại thay vì số ngày vừa cấp (trái nghĩa chuỗi `vip_success_message`). Patch đề xuất: với redeem code lấy duration từ kết quả resolve/API mới nếu SDK cung cấp; với rewarded truyền rõ `3`; với token dùng wording “VIP active until …” thay vì suy ra số ngày cấp.

## Live device test (nếu có làm)

- Không chạy lại ADB. Debug APK đã được rebuild sạch; không cài lên máy để tránh thay đổi trạng thái release hiện có. Các claim live ở mục 6 chỉ được kiểm tra chéo bằng code/source SDK, không nhận là bằng chứng live mới.

## Kết luận

Debug build và các fix policy/VIP chính khớp code thật, bao gồm việc fallback interstitial không cấp VIP và rewarded dùng `grantVipDays`. Tuy nhiên working tree hiện **chưa release-ready** vì `assembleProductionRelease` fail do QC-only BuildConfig fields. Ngoài blocker build, có một regression dialog do double-tap và một sai lệch thông điệp số ngày khi cộng dồn VIP. QC-2 được mô tả đúng; QC-1 vẫn là trạng thái ngoài code cần kiểm tra lại trên AdMob Console.
