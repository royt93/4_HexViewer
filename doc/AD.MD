# AD.MD — Kế hoạch nâng cấp/tích hợp Ad SDK cho HexViewer

> Trạng thái: **AUDIT XONG (3/3 agent độc lập: codex, gemini, claude2) — chờ user cung cấp thông tin
> (mục 3) + duyệt plan trước khi code.** KHÔNG code cho tới khi user duyệt.

## 0. Bối cảnh

Project **ĐÃ tích hợp** `AdmobApplovinWrapper` từ trước (không phải tích hợp mới). Yêu cầu lần này:
1. Audit toàn bộ theo chuẩn `doc/AD_PROMPT_AOS.MD`.
2. Đổi ưu tiên provider sang **AdMob** (`IS_ENABLE_ADMOB=true`) — hiện đang `false` (AppLovin).
3. Debug dùng ad ID test (Google demo ID) — **debug hiện đã đúng**, giữ nguyên.
4. Release dùng ad ID thật do user cung cấp sau — **release hiện SAI 1 ID** (xem finding F4).

## 1. Touchpoint table (hiện trạng)

| Slot | File hiện tại | Trigger | API dùng | Trạng thái |
|---|---|---|---|---|
| Banner | `ActMain.java` (`refreshBannerState()` ~L749) | onResume + VIP state change | `AdManager.loadBanner`/`bannerDestroy` | ✅ đã dùng đúng API mới |
| Interstitial | `ActMain.java` (nhiều call site: L132,141,559,575) | các action trong app (giữ nguyên, không thêm mới) | `AdManager.showInterstitial`/`loadInterstitial` | ✅ đã dùng đúng API mới |
| App Open | Không có call site thủ công | tự động qua `ProcessLifecycleOwner` | (SDK tự quản) | ✅ đúng theo doc (không tự gọi tay) |
| Splash consent + App Open | `SplashActivity.java` | `onCreate` | `AdManager.requestConsentInfoUpdate` → `AdManager.initSplashScreen` | ✅ đúng theo doc (bản callback, không cần `@OptIn` experimental) |
| Rewarded / VIP | `ActVipManagement.kt`, `VipKeys.kt`, `VipPrefs.kt` | user bấm nút VIP screen | (cần agent audit xác nhận chi tiết dùng `activateVipByToken`/`activateVipByKey`/`grantVipDays`) | 🟡 cần audit sâu (xem việc giao cho codex/gemini/claude2) |

## 2. Finding đã xác nhận (tự audit, có bằng chứng file:line)

| # | Mức độ | File:line | Vấn đề | Theo doc |
|---|---|---|---|---|
| F1 | 🔴 CAO | `app/build.gradle:159` | SDK version `1.1.5`, doc yêu cầu tối thiểu `1.6.2` (bug VIP cấp thầm lặng đã fix ở 1.6.2, `AdSafetyStatus` field rename, `grantRewardOnEarn` đổi default). | Phần A, mục "Nâng từ ≤1.5.5 lên 1.6.1" |
| F2 | 🔴 CAO | `MyApplication.java` `setupAd()` (~L440-460) | `vipKeySecret` set = `VipKeys.VIP_30D_KEY` (chính là VIP redeem key 30 ngày) — SAI mục đích. `vipKeySecret` phải là secret ngẫu nhiên ≥16 ký tự riêng cho chống-tamper prefs, KHÔNG phải VIP key thật. | Step 0 rule 5, A.7 |
| F3 | 🔴 CAO | `MyApplication.java` `setupAd()` | `AdSdkConfig` KHÔNG truyền `vipTokenPublicKey` → dùng public key MẪU mặc định → release **từ chối im lặng mọi VIP token** (`activateVipByToken` luôn `false`). | Step 0 rule 5, A.7 |
| F4 | 🔴 CAO | `app/build.gradle:96` (buildType `release`) | `ADMOB_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"` — đây là ID demo/test chính thức Google, đang gán cho build **release**. Ship production sẽ chạy test ad → vi phạm policy AdMob + zero revenue rewarded. | Bước 3 cảnh báo |
| F5 | 🟡 TRUNG | Toàn bộ `MyApplication.java`/`ActMain.java` | Không tìm thấy `AdManager.setTestDeviceIds(...)` ở đâu — QA/dev đang test bằng banner AppLovin ID thật (không phải test ID, AppLovin không có universal test ID) → risk invalid traffic nếu chưa bật MAX Test Mode + khai test device. | Bước 3b |
| F6 | 🟡 TRUNG | `MyApplication.java` `setupAd()` | `appOpenExcludedActivities` không truyền tường minh — đang dựa vào default deprecated string-match `"SplashActivity"` (activity thật đúng tên `SplashActivity` nên tình cờ vẫn đúng, nhưng doc khuyến cáo không dựa default ngầm). | Bước 4, S6 |
| F7 | 🟡 TRUNG | `MyApplication.java` `setupAd()` | `applovinPrivacyPolicyUrl` không truyền dù đã có `BuildConfig.PRIVACY_POLICY_URL` sẵn — cần nếu bật `applovinUseNativeCmp`. | A.7, 8.4b |
| F8 | 🟡 TRUNG | `AndroidManifest.xml` | Thiếu `<attribution android:tag="ads" .../>` cho targetSdk 36 (≥31). | Step 6 |
| F9 | 🟢 THẤP | `AndroidManifest.xml` | `com.google.android.gms.ads.APPLICATION_ID` comment ghi "Sample AdMob App ID" — cần user xác nhận đây là App ID thật hay còn placeholder. | Bước 3, 8.3 |
| F10 | 🟢 THẤP | `MyApplication.java` `setupAd()` | Không set `paidEventListener`/`errorReporter` → mất revenue tracking + error reporting. | Bước 4 |
| F11 | ⚪ Quyết định | `app/build.gradle` (defaultConfig + cả 2 buildType) | `IS_ENABLE_ADMOB=false` hiện tại (provider = AppLovin). User yêu cầu ưu tiên AdMob → cần đổi `true`. | Yêu cầu user, ghi đè "Cấm đổi flag" mặc định vì có chỉ thị rõ |

## 2b. Finding từ agent độc lập CODEX (xác nhận qua code thật, đã verify lại)

| # | Mức độ | File:line | Vấn đề | Fix |
|---|---|---|---|---|
| F12 | 🔴 CAO | `ActVipManagement.kt:190-198` | Fallback interstitial (`shown==true`) vẫn gọi `grantVipFromAd()` — **cấp VIP cho non-rewarded ad**. Vi phạm thẳng Step 7 rule 6 (cấm tuyệt đối), rủi ro ban account AdMob/AppLovin. | Nhánh `shown` chỉ gọi `showNoRewardDialog()`/thông báo, KHÔNG gọi bất kỳ grant API nào. |
| F13 | 🔴 CAO | `ActVipManagement.kt:144-145,242-243` | `grantVipFromAd()` và nút Activate đều gọi `AdManager.activateVipByKey(this, AdManager.adConfig.vipKeySecret, days)` — dùng sai API + sai tham số (`vipKeySecret` không phải VIP key hợp lệ để verify; legacy plaintext path mặc định TẮT ở 1.2.0+ nên có thể fail im lặng). | Reward path → `AdManager.grantVipDays(ctx, 3)`. Redeem path (nút nhập mã) → chuyển sang `activateVipByToken(input)` (ECDSA) hoặc giữ `VipKeys.lookupDays` + gọi đúng `activateVipByKey(ctx, input, days)` (truyền `input` user nhập, không phải `vipKeySecret`). |
| F14 | 🟡 TRUNG | `VipPrefs.kt` + `ActVipManagement.kt:148,245,256,170` | App tự lưu/đọc `grantedAtMs` riêng (`saveGrantedAtMs`/`getGrantedAtMs`/`clearGrantedAtMs`) — trái nguyên tắc single-source-of-truth, sai lệch khi VIP cấp qua auto-trial/token/`grantVipDays`. | Bỏ 3 hàm timestamp trong `VipPrefs`, dùng thẳng `AdManager.getVipGrantedAtMs()`. `VipPrefs` chỉ giữ `userRedeemedAtLeastOnce()`. |
| F15 | 🟡 TRUNG | `ActVipManagement.kt:245-246` | `grantVipFromAd()` gọi `markUserRedeemed()` — nhầm lẫn "xem ad nhận VIP" thành "user tự redeem key", sai phân loại grace/redeem entry ở Step 9.3. | Bỏ `markUserRedeemed()` khỏi nhánh reward-ad. |
| F16 | 🟡 TRUNG | `ActMain.java:764-770` (`autoManageLifecycle=true`) vs `ActMain.java:225-231,755-760` (`bannerDestroy` gọi tay) | Vừa để SDK auto-manage lifecycle vừa tự gọi `bannerDestroy` tay — trái contract "không phối hợp cả 2". Không crash (idempotent-guard) nhưng sai pattern. | Chọn 1: giữ `autoManageLifecycle=true` và bỏ toàn bộ gọi tay `bannerDestroy`, HOẶC đổi `false` và tự forward đủ resume/pause/destroy. Cân nhắc kỹ vì code có `WebViewOomFix` workaround gắn theo `bannerDestroy` thủ công — cần giữ đúng 1 mode rõ ràng, test kỹ leak. |
| F17 | 🟡 TRUNG | `SplashActivity.java:145-146` | Timeout tự chế 5s (`postDelayed(consentTimeoutRunnable, 5000)`) chạy song song với watchdog nội bộ của SDK — có thể cắt ngang trước khi `initSplashScreen`/App Open kịp chạy, và bỏ lỡ callback consent về muộn. | Cân nhắc bỏ timeout app-side, tin tưởng watchdog/hard-cap của wrapper (đã có sẵn theo Step 8.2). Nếu giữ (do lo ngại hang), tăng threshold hoặc chỉ dùng làm safety-net cực trễ (>watchdog SDK). |

**Nâng version SDK (F1)**: codex xác nhận qua `git ls-remote --tags` → tag semver mới nhất hiện có là **1.6.16** (JitPack build-status cần kiểm tra riêng trước khi pin, artifact endpoint lúc audit trả 401 không verify được).

## 2c. Finding từ agent độc lập GEMINI (đã verify qua code thật)

| # | Mức độ | File:line | Vấn đề | Fix |
|---|---|---|---|---|
| F18 | 🔴 CAO | `ActVipManagement.kt:127-159` | Màn VIP KHÔNG hỗ trợ nhập VIP Token ECDSA — `AdManager.activateVipByToken(...)` **0 lần được gọi** trong toàn project (grep xác nhận). Nút "Kích hoạt" chỉ lookup 2 mã Base64 tĩnh trong `VipKeys.kt`. Nếu nâng SDK lên bản khuyến nghị dùng token (đường chính theo doc), màn hình hiện tại không dùng được. | Thêm nhánh `AdManager.activateVipByToken(this, inputKey)` (thử trước), fallback `VipKeys.lookupDays` cho mã thẻ cào cũ nếu vẫn muốn giữ. |
| F19 | 🟡 TRUNG | `ActRecentlyOpen.java:171-193` | Cũng có banner riêng (`loadBanner`/`bannerDestroy`), **bị bỏ sót khỏi bảng Touchpoint mục 1** — cùng lỗi F16 (auto-manage + destroy tay) lặp lại ở đây. | Bổ sung vào Touchpoint table; áp cùng fix F16 cho cả 2 Activity. |
| F20 | ⚪ THẤP | `ActMain.java:39`, `ActRecentlyOpen.java:37` | Import thừa `com.google.android.gms.ads.AdSize` — không dùng (SDK dùng `AdBannerSize` riêng, không phải AdMob `AdSize`). | Xóa import thừa. |

## 2d. Bảng tổng hợp thống nhất (codex + gemini xác nhận trùng khớp 100% trên các finding chung F1-F11, F12-F17)

Cả 2 agent cho cùng kết luận trên mọi finding gốc + finding mới trùng (F12=Finding1, F13=Finding2, F14=Finding4, F16=Finding6). Độ tin cậy cao.

## 2e. Finding từ agent độc lập CLAUDE2 (đào sâu nhất — kéo thẳng source thật tag `1.1.5` từ GitHub API để verify, không chỉ suy theo mô tả doc)

| # | Mức độ | File:line | Vấn đề | Fix |
|---|---|---|---|---|
| F21 | 🔴 **CAO NHẤT** | `MyApplication.java:601-623` gọi `initialize()` ngay trong `Application.onCreate()`, TRƯỚC khi `SplashActivity` kịp gọi `requestConsentInfoUpdate()` | **SDK 1.1.5 không có consent-gating ở tầng provider init** (verify bằng đọc source thật `AdManager.kt` tag 1.1.5, hàm `initialize()` dòng 360-395): `setConfig()` tạo provider đồng bộ, `initialize()` gọi thẳng `MobileAds.initialize()`/`AppLovinSdk.initialize()` — KHÔNG có `WAITING_FOR_CONSENT` như doc mô tả (đó là kiến trúc của bản MỚI HƠN). Hệ quả: AppLovin (provider active hiện tại) có thể fetch GAID/App Set ID/network call **trước khi có quyết định consent GDPR của user** — rủi ro pháp lý thật, không phải suy diễn. | Đây là lý do CHÍNH đáng phải nâng SDK — không chỉ vì bug VIP như đầu mối ban đầu nêu. |
| F22 (làm rõ F13/Finding2) | 🔴 CAO | `ActVipManagement.kt:144-145,242-243` + `AdManager.kt:1716` (tag 1.1.5) | Root cause chính xác: SDK check `key != adConfig.vipKeySecret`, nhưng app truyền lại chính `adConfig.vipKeySecret` làm `key` → so sánh 1 giá trị với chính nó → **luôn pass, verify SDK thành no-op**. An toàn tính năng "nhập mã VIP" hiện hoàn toàn dựa vào `VipKeys.lookupDays()` phía client (dễ decompile), không phải cơ chế SDK. | Khi nâng SDK ≥1.2.0, legacy plaintext path mặc định TẮT (`allowLegacyPlaintextVipKey=false`) → toàn bộ nhánh này **fail im lặng**. Phải viết lại theo token ECDSA (`activateVipByToken`) + `vipRedeemCodes` (field built-in cho "thẻ cào", thay vì tự chế cơ chế gọi ngược `vipKeySecret`). |
| F23 | 🟡 TRUNG | `VipKeys.kt:5-6` | `VIP_30D_KEY`/`VIP_3D_KEY` chỉ Base64 (không phải mã hoá) — decode ra plaintext tức thì (`"9fA0q7eN!27cLx04@21993Y2u0I7#Q0"` / `"eQ7@93L0f!2Y2707xN04021993u0I#2aK"`, đã tự verify). Rủi ro CAO HƠN "thẻ cào" thường vì đồng thời đang đóng vai `vipKeySecret` bảo vệ tầng SDK (xem F22) — 2 vai trò bị trộn lẫn. | Chấp nhận được cho VIP giá trị thấp (đúng khuyến nghị doc cho thẻ cào), nhưng phải tách khỏi vai trò `vipKeySecret`. |
| F24 | ⚪ THẤP | `MyApplication.java:43` | TODO tồn đọng không rõ nghĩa: `//TODO roy93~ why you see ad`. | Dọn dẹp, không ảnh hưởng chức năng. |

**Publisher ID App ID (F9/#7)**: claude2 bổ sung bằng chứng mạnh — publisher ID `3612191981543807` trong manifest **trùng khớp** publisher ID của 3/4 ad-unit ID production thật trong `build.gradle` release (khác hẳn publisher ID demo Google `3940256099942544`). Rất có khả năng đây LÀ App ID thật, comment "Sample" chỉ là sót lại từ template — nhưng vẫn cần user xác nhận chính thức.

## 2f. Tổng hợp cuối — thứ tự ưu tiên xử lý (đồng thuận cả 3 agent)

1. **Nâng SDK lên tag mới nhất verify được qua JitPack thật** (đừng tin số version cứng trong doc/audit vì có thể lỗi thời — check trực tiếp https://jitpack.io/#royt93/AdmobApplovinWrapper lúc thực thi). Đây là gốc rễ của phần lớn finding CAO (F1, F21, F22 đều bắt nguồn từ version quá cũ).
2. **Viết lại toàn bộ VIP flow** theo token ECDSA (`generateVipKeyPair`/`activateVipByToken`) thay cho `activateVipByKey(this, adConfig.vipKeySecret, days)` hiện tại (F13/F22), sửa 3 bug policy nghiêm trọng trong `ActVipManagement.kt`:
   - F12: fallback interstitial đang cấp VIP — PHẢI xoá (rủi ro ban account).
   - F13/F22: `grantVipFromAd()` phải đổi sang `AdManager.grantVipDays(ctx, 3)`.
   - F14: bỏ lưu `grantedAtMs` thủ công trong `VipPrefs`, dùng `AdManager.getVipGrantedAtMs()`.
   - F15: bỏ `markUserRedeemed()` khỏi nhánh reward-ad.
   - F18: thêm nhánh `activateVipByToken` cho input token ECDSA.
3. **Việc sửa ngay, không phụ thuộc nâng SDK** (hàm đã tồn tại ở 1.1.5): thêm `AdManager.setTestDeviceIds(...)` (F5), sửa `ADMOB_REWARDED_ID` release khỏi demo ID Google (F4 — **PHẢI sửa trước khi flip `IS_ENABLE_ADMOB=true`**), thêm `<attribution>` tag (F8).
4. **Sau khi nâng SDK, wire các field config mới**: `vipTokenPublicKey`, `applovinPrivacyPolicyUrl`, `appOpenExcludedActivities`, `paidEventListener`, `errorReporter` (F3/F6/F7/F10).
5. **Chuẩn hoá banner lifecycle**: chọn 1 trong 2 mode (`autoManageLifecycle=true` bỏ hết gọi tay `bannerDestroy`, HOẶC `false` forward đủ 3 hàm) — áp cho cả `ActMain.java` + `ActRecentlyOpen.java` (F16, F19). Xoá import thừa `AdSize` (F20).
6. **Chỉ flip `IS_ENABLE_ADMOB=true`** sau khi: App ID thật đã xác nhận (F9), rewarded ID release đã sửa (F4), Step 8 (UMP Console Publish) đã làm xong ngoài code.
7. Dọn TODO tồn đọng (F24).

## 3. Quyết định của user (2026-08-16, qua AskUserQuestion)

- ✅ **App ID manifest hiện tại = placeholder, KHÔNG dùng.** User sẽ cung cấp **full bộ AdMob ID release thật** (App ID + 4 ad unit ID) sau. → giữ nguyên kiến trúc đọc qua `BuildConfig`/manifest placeholder, thay giá trị khi có ID thật, KHÔNG tự đoán/tự điền.
- ✅ **Nâng SDK lên tag mới nhất** (verify qua JitPack lúc thực thi, đừng tin số cứng trong doc/audit).
- ✅ **Release AdMob ID: tạm giữ ID demo/test Google** cho tới khi user gửi bộ ID thật — áp dụng cho **cả 4 slot** (không chỉ riêng rewarded), đồng nhất bằng ID demo chính thức của Google (không giữ ID "nhìn giống thật nhưng chưa xác nhận" như hiện tại) để tránh mơ hồ. Đánh dấu rõ trong code (comment `// TODO(release-ids)`) + trong tài liệu là **CHƯA sẵn sàng ship Play Store** cho tới khi thay ID thật.
- ✅ **`IS_ENABLE_ADMOB = true`** cho cả debug và release ngay từ vòng migrate này (đúng yêu cầu gốc "ưu tiên AdMob") — vì release vẫn đang dùng ID demo (bước trên), việc flip an toàn để dev/test, KHÔNG phải tín hiệu sẵn sàng publish.
- ✅ **Viết lại toàn bộ VIP flow** theo chuẩn SDK mới: bỏ `activateVipByKey(this, adConfig.vipKeySecret, days)`, thêm `activateVipByToken`, đổi reward-grant sang `AdManager.grantVipDays`, bỏ cấp VIP qua fallback interstitial (F12), tách `vipKeySecret` khỏi VIP redeem key.
- ⏳ Còn treo, KHÔNG chặn migrate: bộ AdMob ID release thật (user gửi sau), xác nhận cuối App ID thật.

## 4. Bước tiếp theo

1. ~~Chờ 3 report độc lập~~ ✅ xong (`doc/audit/audit_codex.md`, `audit_gemini.md`, `audit_claude2.md`).
2. ~~Tổng hợp finding~~ ✅ xong (mục 2).
3. ~~Hỏi user~~ ✅ xong (mục 3).
4. **Đang chờ user xác nhận cuối để bắt đầu code** (xem tin nhắn chat) — implementation theo đúng thứ tự ưu tiên ở mục 2f.
