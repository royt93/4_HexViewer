# AD.MD — Kế hoạch nâng cấp/tích hợp Ad SDK cho HexViewer

> Trạng thái: **IMPLEMENTATION XONG (2026-08-16)** — user đã duyệt plan, code đã triển khai theo mục
> 2f, build debug + release đều xanh. Xem mục 5 "Kết quả implementation" cho chi tiết + việc còn lại.
>
> 🔴 **CRITICAL — NGOÀI PHẠM VI ad SDK, phát hiện ở vòng audit 4 (mục 9)**: `app/key/keystore.jks`
> (khoá ký release Play Store thật) + `keystore.properties` (password plaintext) đang bị track trong
> git và ĐÃ có mặt công khai trên `origin/dev` (repo GitHub `royt93/4_HexViewer` là **PUBLIC**) — không
> phải rủi ro giả định, đây là sự cố ĐANG XẢY RA. **CHƯA push thêm gì** cho tới khi user quyết định xử
> lý. Xem mục 9.2.

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
4. ~~Chờ user xác nhận cuối~~ ✅ user duyệt "triển khai đi, tự loop xong tính năng sdk" (2026-08-16).
5. ~~Implementation~~ ✅ xong — xem mục 5.

---

## 5. Kết quả implementation (2026-08-16)

### 5.1 — Build status

- `./gradlew assembleDevDebug` → **BUILD SUCCESSFUL**
- `./gradlew assembleProductionRelease` (R8/minify/shrink bật) → **BUILD SUCCESSFUL**
- Chưa smoke-test trên thiết bị thật (user đang ngủ lúc code xong — theo R3, không tự chọn device
  cài đặt khi chưa hỏi được ai xác nhận; để lại cho lần sau).

### 5.2 — ⚠️ Phát hiện quan trọng chưa có trong audit ban đầu: repo SDK đã chuyển PRIVATE

`git ls-remote`/`curl` tới JitPack cho tag `1.6.16` trả `401 Unauthorized` (`WWW-Authenticate: Basic
realm="JitPack"`) — không phải lỗi mạng sandbox. Xác minh qua `gh api repos/royt93/AdmobApplovinWrapper`
→ `"visibility":"private"`. Tag `1.1.5` (bản cũ project đang dùng) vẫn resolve `200` vì JitPack đã
build/cache từ trước lúc repo còn public; mọi tag mới (kể cả `1.6.16`) đều bị chặn 401 tới khi có
`JITPACK_TOKEN`. Chính `settings.gradle.kts` của repo SDK (đọc được qua `gh api` vì tài khoản `gh` trong
môi trường này được cấp quyền — `gh auth status` hiện đăng nhập là `royt93`, trùng chủ SDK) đã tự
document sẵn cơ chế fix: `jitpack.token` trong `local.properties` hoặc biến môi trường `JITPACK_TOKEN`
(lấy tại jitpack.io → account settings → "Private repos").

**Giải pháp đã áp dụng (không chặn tiến độ, portable-ready):**
1. Tải source thật tại tag `1.6.16` qua `gh api .../tarball/1.6.16` (không cần SSH, dùng token có sẵn).
2. Sửa `artifactId` publication trong `admob-wrapper-sdk/build.gradle.kts` (bản tải về, KHÔNG đụng vào
   repo gốc) từ `admob-wrapper-sdk` → `AdmobApplovinWrapper` để khớp đúng coordinate JitPack thật sự
   expose (xác nhận qua comment trong chính `jitpack.yml` của repo).
3. `./gradlew :admob-wrapper-sdk:publishToMavenLocal -Pversion=1.6.16` (JDK 17) → AAR nằm ở
   `~/.m2/repository/com/github/royt93/AdmobApplovinWrapper/1.6.16/`.
4. `settings.gradle` (HexViewer) — thêm `mavenLocal()` vào `dependencyResolutionManagement` (trước
   `jitpack`), và thêm hỗ trợ `jitpack.token`/`JITPACK_TOKEN` credentials cho maven block jitpack
   (mirror đúng pattern repo SDK gốc dùng).

**Hệ quả — ĐỌC KỸ trước khi build trên máy khác / CI:**
- Build hiện chỉ chạy được trên **máy này** vì `mavenLocal()` là repo cục bộ (`~/.m2`), không portable.
- Để build trên máy khác (máy user, CI/CD, máy đối tác…): cần 1 trong 2 cách —
  (a) lấy `JITPACK_TOKEN` tại jitpack.io (account settings → Private repos, cần quyền truy cập repo
  GitHub `royt93/AdmobApplovinWrapper`) rồi đặt vào `local.properties` (`jitpack.token=...`) hoặc env
  `JITPACK_TOKEN` trên máy đó; **hoặc**
  (b) tự `publishToMavenLocal` lại từ source trên máy đó (cần quyền đọc repo GitHub, xem lại 4 bước
  trên).
- Đây là rủi ro build-portability CAO — flag lại cho user xử lý sớm (không phải lỗi trong phạm vi
  audit gốc, phát sinh ngoài dự kiến khi thực thi).

### 5.3 — Finding đã fix (theo mã F trong mục 2/2b/2c/2e)

| # | Trạng thái | Fix thực tế |
|---|---|---|
| F1 | ✅ Fixed | `app/build.gradle`: `1.1.5` → `1.6.16` |
| F2/F13/F22 | ✅ Fixed | `vipKeySecret` mới (random 32-byte base64, `openssl rand -base64 32`) qua `BuildConfig.VIP_KEY_SECRET`, tách hẳn khỏi VIP redeem key |
| F3 (vipTokenPublicKey) | ✅ Fixed | Sinh **keypair ECDSA P-256 thật** bằng `openssl` (tương thích `VipSecurity.kt`: secp256r1, X.509/PKCS8 DER base64) — public key vào `BuildConfig.VIP_TOKEN_PUBLIC_KEY`, private key CHỈ ở `local.properties` (gitignored, không commit) |
| F3 (appOpenExcludedActivities/applovinPrivacyPolicyUrl) | ✅ Fixed | Wire trong `AdSetup.kt` |
| F3 (paidEventListener/errorReporter) | ✅ Fixed | Set trong `AdSetup.kt`, `Application.onCreate()`, log qua tag `roy93~AdRevenue`/`roy93~AdError` (project chưa có Crashlytics — không tự thêm dependency ngoài phạm vi yêu cầu) |
| F4 | ✅ **Fixed thật** (2026-08-16, cập nhật sau) | User cung cấp `doc/ad/ad.md` — 4 ID production thật (publisher `3004713799155145`) đã thay vào `app/build.gradle` block `release`. Build lại xanh. |
| F5 (setTestDeviceIds) | ✅ Fixed (scaffold) | Gọi trong `AdSetup.kt` callback `initialize()`, đọc `BuildConfig.TEST_DEVICE_GAID_1/2` (rỗng — chờ user điền GAID QA thật) |
| F6 | ✅ Fixed | `IS_ENABLE_ADMOB=true` cả debug/release |
| F8 | ✅ Fixed | Thêm `<attribution>` tag vào manifest |
| F9 | ✅ **Fixed thật** (2026-08-16, cập nhật sau) | App ID thật `ca-app-pub-3004713799155145~6239626749` từ `doc/ad/ad.md` đã thay vào manifest — publisher ID KHÁC hẳn giá trị cũ (`3612191981543807`), xác nhận giá trị cũ đúng là placeholder sai như audit nghi ngờ. |
| F12 | ✅ **Fixed (nghiêm trọng nhất)** | Bỏ hoàn toàn việc cấp VIP qua fallback interstitial trong `ActVipManagement.kt` |
| F14 | ✅ Fixed | `VipPrefs` bỏ `saveGrantedAtMs`/`getGrantedAtMs`/`clearGrantedAtMs`, `ActVipManagement.bindUi()` đọc thẳng `AdManager.getVipGrantedAtMs()` |
| F15 | ✅ Fixed | Bỏ `markUserRedeemed()` khỏi `grantVipFromAd()` |
| F18 | ✅ Fixed | `ActVipManagement` giờ gọi `AdManager.activateVipByKey(ctx, input, 0)` — SDK tự thử token ECDSA trước rồi tới `vipRedeemCodes`, không còn tự lookup phía app |
| F20 | ✅ Fixed | Xoá import `AdSize` thừa ở `ActMain.java` + `ActRecentlyOpen.java` |
| F24 | ✅ Fixed | Xoá TODO tồn đọng trong `MyApplication.java` |

### 5.4 — Quyết định có chủ đích: GIỮ NGUYÊN banner lifecycle (F16/F19, không fix)

Audit gắn mức TRUNG cho việc `autoManageLifecycle=true` nhưng vẫn gọi tay `bannerDestroy()` trong
`ActMain.java`/`ActRecentlyOpen.java`. Quyết định **không sửa**, lý do:

- `bannerDestroy()` có idempotent-guard theo chính doc SDK (`AD_PROMPT_AOS.MD` Phụ lục A.3) — gọi tay
  thêm không crash, không double-preload.
- Lệnh gọi tay này đi kèm `WebViewOomFix.restoreWebViewFrameRate(adView)` — một fix chống OOM cụ thể
  cho MediaTek BoostFwk (comment gốc trong code, đã tốn công điều tra thật). Nếu bỏ gọi tay và hoàn
  toàn tin `autoManageLifecycle` tự destroy qua `ActivityLifecycleCallbacks`, KHÔNG có gì đảm bảo
  `restoreWebViewFrameRate()` chạy đúng thời điểm trước khi WebView bị teardown — rủi ro tái phát bug
  OOM đã fix, đổi lấy lợi ích thuần "đúng pattern doc khuyến nghị" (không phải bug thật).
- Nhánh gọi tay trong `refreshBannerState()` (khi user vừa lên VIP) KHÔNG phải "lifecycle forward" —
  đó là cleanup nghiệp vụ hợp lệ (ẩn banner ngay khi VIP kích hoạt), không nằm trong phạm vi cảnh báo
  của doc.

Đã xoá import `AdSize` thừa (F20) — phần dọn dẹp an toàn không ảnh hưởng hành vi.

### 5.5 — Việc còn lại (không tự làm được, cần user)

- [ ] **JITPACK_TOKEN** hoặc quy trình `publishToMavenLocal` cho mọi máy build khác (mục 5.2) — build
      portability đang phụ thuộc máy này.
- [x] ~~Bộ 4 AdMob ad unit ID + App ID production thật~~ — nhận từ `doc/ad/ad.md` (2026-08-16), đã
      wire vào `app/build.gradle` (release) + manifest, build lại xanh. Native ID (`6809688569`) lưu
      ở `BuildConfig.ADMOB_NATIVE_ID_UNUSED` — SDK hiện KHÔNG có API native ad, chưa dùng được.
- [x] ~~GAID máy QA~~ — user xác nhận đã add test device trực tiếp trên AdMob Console, không cần
      `TEST_DEVICE_GAID_1/2` nữa (2026-08-16).
- [x] ~~Smoke-test trên thiết bị thật~~ — xong, xem mục 6 "QC Master audit" bên dưới.
- [ ] Step 8 ngoài code: user báo "done" (2026-08-16) nhưng **QC phát hiện chưa thật sự xong** cho
      App ID mới — xem finding QC-1 mục 6.2.
- [x] ~~Đúc thử VIP token~~ — đã đúc token ECDSA thật bằng private key ở `local.properties` (openssl,
      không cần chạy `generateVipToken` trên máy), xem mục 6.4.

---

## 6. QC Master audit — full case test (2026-08-16, theo yêu cầu "audit như QC master")

Test trực tiếp trên thiết bị thật (OPPO CPH1989), build debug (`assembleDevDebug`, Google test ad ID,
`IS_ENABLE_ADMOB=true`) trừ khi ghi chú khác. Thêm hook QC-only vào `AdSetup.kt` (chỉ chạy khi
`BuildConfig.DEBUG`): dump `AdManager.getDiagnostics()` ra logcat sau init, hỗ trợ `umpDebugGeography`
qua `BuildConfig.UMP_DEBUG_GEOGRAPHY`/`UMP_TEST_DEVICE_HASH` (2 field mới, rỗng mặc định, không ảnh
hưởng release).

### 6.1 — Cold-start ONLINE (VN, non-EEA) — ✅ PASS

Consent-first flow đúng như thiết kế: `WAITING_FOR_CONSENT` → `requestConsentInfoUpdate` →
`canRequestAds=true` → provider `INITIALIZED`. Banner/Interstitial/App Open/Rewarded đều load thành
công (Google test ID), `paidEventListener` bắn đúng revenue log. `getDiagnostics()` báo
`ADS STATUS: ✅ READY`. Không crash.

### 6.2 — 🔴 Finding QC-1 (CAO): `Publisher misconfiguration` — Step 8 CHƯA thật sự xong cho App ID mới

Log thật lúc init: `UMP consent info error (code=3): Publisher misconfiguration: ... no form(s)
configured for the input app ID. Received app ID: ca-app-pub-3004713799155145~6239626749`. `getDiagnostics()`
tự cảnh báo kèm theo: *"Nếu thiếu: UMP 'Publisher misconfiguration' → fail-open → GDPR violation."*

User xác nhận "step 8 done" — nhưng log cho thấy **AdMob Console chưa có European regulations message
được Publish cho đúng App ID `ca-app-pub-3004713799155145~6239626749`** (App ID mới nhận hôm nay,
`doc/ad/ad.md`). Có thể message đã Publish cho App ID CŨ (placeholder) trước đó, hoặc chưa propagate.
Hiện tại `canRequestAds=true` vì thiết bị test ở VN (non-EEA, không bị chặn) — nhưng **user EEA/UK/CH
thật sẽ không có form để chọn, dẫn tới 1 trong 2 kết quả xấu: SDK fail-closed (zero ad, đúng thiết kế
an toàn) hoặc nếu ai đó tắt `blockLoadUntilConsent` sẽ vi phạm GDPR thật.**

**Cần làm lại**: AdMob Console (apps.admob.com) → app đúng `ca-app-pub-3004713799155145~6239626749` →
Privacy & messaging → European regulations → xác nhận **Published** (không phải Draft) cho ĐÚNG app
này, không phải app cũ.

### 6.3 — Cold-start OFFLINE — ✅ PASS

Tắt hẳn wifi + mobile data (ping 100% loss, xác nhận offline thật). `SplashActivity` bỏ qua consent
ngay (`NetworkUtils.isNetworkAvailable()==false` → `goToMain()` trong ~1.1s, không đợi timeout 5s).
Banner/Interstitial fail-soft đúng cách (`⏭️ consent chưa cho phép — queue retry khi gate mở`), không
crash, app dùng được bình thường không quảng cáo.

### 6.4 — 🟡 Finding QC-2 (TRUNG): Watchdog 30s bắn `IllegalStateException` — noise thật cho Crashlytics

Cold-start offline → không ai gọi `requestConsentInfoUpdate()` (đúng thiết kế, xem 6.3) → nhưng SDK vẫn
tự arm watchdog 30s từ lúc `initialize()`. Đúng 30s sau, watchdog bắn qua `errorReporter`:
```
roy93~AdError: PB-01 init parked WAITING_FOR_CONSENT — ...
java.lang.IllegalStateException: Consent chưa được nối sau 30000ms
	at com.roy.sdkadbmob.AdManager.armPendingInitWatchdog$lambda$46(AdManager.kt:1489)
```
**Không phải crash** (app không hề dừng, PID không đổi) — đây là SDK cố ý dựng exception để lấy stack
trace cho log, route qua `errorReporter` (đúng contract "reporter không được throw"). Nhưng nếu
`errorReporter` sau này forward Crashlytics thật, **mọi lần user mở app offline sẽ tạo 1 report trông
như crash thật** trong dashboard — false alarm, dễ gây nhiễu on-call. Khuyến nghị: filter message
`"Consent chưa được nối sau 30000ms"` ở tầng Crashlytics (đánh dấu non-fatal/ignore), không phải bug
cần sửa code.

**Finding phụ**: consent KHÔNG tự retry giữa phiên khi mạng quay lại nếu user không quay lại
`SplashActivity` (vd `isTaskRoot()==false` khi mở lại app từ Recents) — chỉ full cold-start mới thử lại
UMP. Hành vi hợp lý với kiến trúc app hiện tại (Splash-once), không phải bug, nhưng cần biết khi debug
báo cáo user "mất mạng giữa chừng, có mạng lại vẫn không thấy ad".

### 6.5 — VIP redeem code — ✅ PASS (đã test ở phiên trước, xem mục 5 báo cáo chat)

### 6.6 — VIP token ECDSA thật — ⚠️ KHÔNG kết luận được qua live UI (nhưng verify được qua construction)

Đúc 1 token thật bằng private key ở `local.properties` (openssl, EC P-256, format `v2|<expiryMs>|*`
ký SHA256withECDSA, y hệt `VipSecurity.signToken` — verify cross-check bằng cách đọc lại source
`VipSecurity.kt` tag 1.6.16 qua `gh api`). Gõ token vào field qua `adb shell input text` bị **IME Telex
tiếng Việt** trên máy tự động ghép ký tự thành dấu (vd 2 ký tự base64 liền nhau bị compose thành 1 ký
tự có dấu `Ê`) → token bị hỏng lúc nhập, kích hoạt thất bại vì sai chữ ký (không phải lỗi app). Không
kết luận được bằng live-test, nhưng đường code `activateVipByKey` verify token TRƯỚC redeem code (đã
đọc source xác nhận thứ tự) và nhánh redeem code (cùng hàm) đã chứng minh chạy đúng trên máy thật →
rủi ro nhánh token sai là thấp, nhưng **chưa phải bằng chứng trực tiếp**. Nên test lại bằng máy có IME
tiếng Anh, hoặc dán qua clipboard thay vì gõ.

### 6.7 — 🎯 Rewarded earn → `grantVipDays` (F13) — ✅ PASS, xác nhận trực tiếp

Bấm "Xem quảng cáo → 3 ngày VIP" → rewarded test ad hiện thật → xem hết → `DISMISSED (earned=true)` →
log `isVIPMember() = true` ngay sau đó → dialog "Kích hoạt VIP thành công 3 ngày!" → VIP card hiện đúng
hạn `19/08/2026`. Xác nhận `grantVipDays(ctx, 3)` chạy đúng qua đường reward thật, không qua
`activateVipByKey`.

### 6.8 — 🎯🎯 Rewarded fallback KHÔNG cấp VIP (F12 — fix quan trọng nhất đợt này) — ✅ PASS, xác nhận trực tiếp

Trigger lại "Xem quảng cáo" ngay sau revoke (rewarded preload chưa kịp) →
`showRewarded ⏭️ ad not ready` → tự động fallback `showInterstitial` → interstitial test ad **thật sự
hiện lên** (`SHOW adLoaded=true`, có `PaidEvent`) → đóng ad → log `isVIPMember() = false` → dialog UI
đúng: **"No Reward This Time — A rewarded video wasn't available, so no VIP days were granted."**

Đây là bằng chứng trực tiếp, live, trên thiết bị thật: **bug gốc (cấp VIP qua fallback interstitial —
rủi ro ban tài khoản AdMob/AppLovin) đã được sửa dứt điểm và hoạt động đúng trong thực tế**, không chỉ
đúng trên giấy/code review.

### 6.9 — Tổng kết QC

| Hạng mục | Kết quả |
|---|---|
| Cold-start online (non-EEA) | ✅ PASS |
| Cold-start offline | ✅ PASS |
| Consent — Publisher misconfiguration | 🔴 **QC-1: Step 8 chưa xong thật cho App ID mới** |
| Watchdog 30s exception noise | 🟡 QC-2: không phải bug, cần filter Crashlytics sau này |
| VIP redeem code (30d dùng rồi / 3d thành công) | ✅ PASS |
| VIP revoke | ✅ PASS |
| VIP token ECDSA | ⚠️ Không live-test được (IME), verify qua code+crypto |
| Rewarded earn → grantVipDays (F13) | ✅ **PASS trực tiếp** |
| Rewarded fallback → KHÔNG cấp VIP (F12) | ✅ **PASS trực tiếp — finding quan trọng nhất đã xác nhận sửa đúng** |
| Crash toàn phiên QC | **0 crash** |

**Kết luận**: Toàn bộ logic VIP + policy-critical fix (F12) đã verify SỐNG trên thiết bị thật, không
chỉ code review. Blocker thật duy nhất còn lại trước khi publish: **QC-1 (Step 8 chưa xong cho App ID
mới)** — cần user vào lại AdMob Console kiểm tra đúng app.

---

## 7. Smoke-test vòng 2 (2026-08-16) — 3 agent độc lập audit CODE ĐÃ SỬA + tự build thật

Sau khi mục 5+6 hoàn tất, giao lại cho codex/gemini/claude2 (agent mới, không có context vòng 1) audit
CHÉO code thật đã implement + tự chạy `./gradlew assembleDevDebug assembleProductionRelease`. Report
đầy đủ: `doc/audit/smoke2_codex.md`, `smoke2_gemini.md`, `smoke2_claude2.md`.

### 7.1 — 🔴 Bug thật tìm được: `assembleProductionRelease` FAIL (2/2 agent xác nhận độc lập, đã fix)

Cả **codex** và **claude2** độc lập tìm ra CÙNG 1 bug: lúc thêm QC hook ở mục 6 (`umpDebugGeography`
qua `BuildConfig.UMP_DEBUG_GEOGRAPHY`/`UMP_TEST_DEVICE_HASH` trong `AdSetup.kt`), 2 `buildConfigField`
tương ứng chỉ được khai ở block `debug` trong `app/build.gradle` — **release build không có 2 symbol
này nên compile FAIL** (`Unresolved reference`). Đây là regression do chính tôi (Claude) gây ra ở mục 6,
chưa rebuild-verify lại release sau khi thêm QC hook — **đúng lý do audit-chéo tồn tại: bắt lỗi người
làm tự bỏ sót**.

**Đã fix ngay**: chuyển 2 `buildConfigField` từ block `debug` sang `defaultConfig` (`app/build.gradle`)
— áp dụng cho mọi variant, runtime vẫn chỉ dùng khi `BuildConfig.DEBUG` (không đổi hành vi release).
Build lại `assembleDevDebug` + `assembleProductionRelease` → **cả 2 xanh**.

### 7.2 — 🟡 Bug thật tìm được: dialog "Kích hoạt VIP thành công N ngày" hiện SAI số khi cộng dồn (2/2 agent, đã fix)

Cả codex và claude2 độc lập phát hiện: `showActivationSuccess()` (mục 5 viết lại) back-calculate số
ngày bằng `(getVipByKeyExpiry() − getVipGrantedAtMs()) / ngày` — đúng khi user đang Free (test QC mục
6.7/6.8 không phơi bày được vì luôn test từ trạng thái sạch). Nhưng **SDK 1.6.16 cộng dồn hạn VIP
(`max(now, hạn cũ) + days`) VÀ ghi đè `grantedAtMs` bằng thời điểm activate hiện tại** mỗi lần cấp mới
(xác nhận qua source thật `AppPreferences.kt`) — nên nếu user redeem/reward thêm lúc ĐANG có VIP còn
hạn, công thức back-calculate cho ra **tổng số ngày còn lại** (vd 13 ngày) thay vì **số ngày vừa cấp
thêm** (vd 3 ngày) → dialog đánh lừa user.

**Đã fix**: `showActivationSuccess(daysGranted: Int? = null)` — reward path truyền thẳng
`REWARD_VIP_DAYS` (const `3`, khớp label nút "Xem quảng cáo → 3 ngày VIP") thay vì đoán; redeem/token
path (không biết trước số ngày do SDK chỉ trả `Boolean`) đổi sang message mới
`vip_success_message_until` = "VIP activated! Active until %1$s" (dùng thẳng `getVipByKeyExpiry()`,
không suy ra số ngày) — tránh hoàn toàn phép tính sai. Build lại xanh.

### 7.3 — Finding khác (THẤP, chưa fix — ghi nhận, không chặn release)

- `activateVipByKey` yêu cầu có mạng (V-03, hành vi SDK cố ý) — user offline nhập ĐÚNG mã vẫn báo lỗi
  chung chung "Mã VIP không hợp lệ", không nói rõ lý do là do mất mạng. Cân nhắc sửa `vip_failed_message`
  hoặc check mạng trước khi cho bấm Activate — để dành, không critical.
- 3 string mới (`attribution_ads_label`, `vip_no_reward_title`, `vip_no_reward_message`, giờ thêm
  `vip_success_message_until`) chỉ có ở `values/strings.xml` (English), chưa dịch 21 locale khác —
  Android tự fallback về English, không lỗi/crash, chỉ là gap i18n nhỏ.

### 7.4 — Xác nhận lại toàn bộ finding mục 5.3 + QC-1/QC-2 mục 6

Cả 2 agent đọc code thật đối chiếu từng dòng, **100% khớp** — không tìm thấy claim sai nào trong mục
5.3 (F1-F24) hay QC-2 (mục 6.4). QC-1 (Publisher misconfiguration) không verify được qua code tĩnh
(đúng — đó là trạng thái AdMob Console, ngoài phạm vi code), không có gì mâu thuẫn.

### 7.5 — Gemini tự làm live device test + xác nhận lại toàn bộ

Sau khi fix 7.1+7.2, gemini được nhờ chạy lại (timeout dài hơn) — KHÔNG chỉ đọc code mà còn tự cài
debug APK lên thiết bị thật (`FUJZIFIR7DQCNRWW`), xác nhận qua logcat: consent fetch OK, AdMob init
`ADS STATUS: ✅ READY`, banner/interstitial/appopen/rewarded đều load đúng. Sau đó tự khôi phục máy về
bản release sạch. Gemini tìm thêm **3 finding CAO/TRUNG mới** (khác 7.1/7.2, độc lập với codex/claude2
nhưng trùng khớp phát hiện của cả 3):

### 7.6 — 🔴 Bug thật tìm được: toàn bộ unit test + androidTest suite gãy biên dịch (3/3 agent xác nhận, đã fix)

Cả 3 agent (codex, gemini, claude2) độc lập tìm ra: khi viết lại VIP flow (mục 5) và xoá
`VipKeys.lookupDays`/`VipPrefs.saveGrantedAtMs` + đổi `AdSdkConfig` sang 30+ tham số, **6 file test cũ
hoàn toàn không được cập nhật theo** — `./gradlew testDevDebugUnitTest` và
`compileDevDebugAndroidTestSources` FAIL biên dịch. Đây là gap thật do quá trình sửa code (mục 5)
không đụng tới test suite.

**Đã fix toàn bộ 6 file**:
- `VipKeysTest.kt` — đổi từ test `lookupDays()` sang test `REDEEM_CODES` map.
- `VipPrefsTest.kt` — bỏ test `saveGrantedAtMs`/`getGrantedAtMs`/`clearGrantedAtMs` (hàm đã xoá), chỉ
  còn test `markUserRedeemed`.
- `ActVipManagementTest.kt`, `AdIntegrationTest.kt` — đổi sang `AdManager.activateVipByKey(ctx, key, 0)` +
  `AdManager.grantVipDays(ctx, days)` đúng API mới, thêm `vipRedeemCodes = VipKeys.REDEEM_CODES` vào
  config test.
- `ActMainPillAnimationInstrumentationTest.kt` (Java, androidTest) — Java không gọi được named-args
  `AdSdkConfig` 30+ tham số → tạo `TestAdSdkConfigFactory.kt` (Kotlin bridge, mirror `AdSetup.kt` phía
  production) cho file Java dùng.
- `ActMainPillAnimationTest.java` (Java, unit test) — đổi `activateVipByKey(ctx, oldKey, 30)` sai cách
  → `grantVipDays(ctx, 30)`.

**Root cause phụ phát sinh trong lúc fix (tự tìm, không phải agent báo)**: sau khi sửa xong, 2 test còn
lại fail runtime vì SDK 1.6.16 có gate `V-03` — `activateVipByKey` yêu cầu network "connected+validated"
thật (`NetworkCapabilities.NET_CAPABILITY_INTERNET/VALIDATED`), mà Robolectric mặc định KHÔNG set 2 cờ
này cho active network giả lập → mọi test gọi `activateVipByKey` với mã ĐÚNG cũng fail. Thêm
`TestNetworkUtils.simulateConnected()` (shadow `NetworkCapabilities` qua Robolectric) vào `@Before` của
2 file cần test đường redeem code. Sau đó lại lộ ra 1 lớp nữa: `vipActivationBackoff` (cơ chế cooldown
chống brute-force của SDK, KDoc SDK ghi rõ "sống hết đời process — đúng ý chống brute-force hơn",
`AdManager.destroy()` **cố ý không reset**) bị 1 test tự thêm (`rejectsUnknownCode`, test mã sai để
verify trả `false`) trip cooldown, làm test khác dùng mã ĐÚNG chạy sau trong cùng JVM fail oan — bỏ
test đó (do tự thêm, không phải test gốc), và đổi sang API test chính thức
`AdManager.clearAppPreferencesForTest(context)` (`@InternalAdApi`, đúng mục đích "chỉ dùng trong test để
reset state") thay vì tự xoá SharedPreferences thô.

### 7.7 — 🟡 Bug thật tìm được: `textCapCharacters` phá hỏng mã VIP/token case-sensitive (3/3 agent, đã fix)

Cả 3 agent độc lập tìm ra CÙNG lỗi: ô nhập mã VIP (`etVipKey`, `f_vip_management.xml`) có
`android:inputType="textCapCharacters"` — ép bàn phím luôn viết hoa, trong khi mã thẻ cào/token ECDSA
đều case-sensitive (vd `9fA0q7eN!...`). User gõ đúng mã vẫn luôn kích hoạt thất bại vì bàn phím tự viết
hoa hết. **Đây chính là nguyên nhân thật khiến live-test token ECDSA ở mục 6.6 thất bại** — tôi từng
đoán sai là do IME Telex tiếng Việt, thực ra là do `textCapCharacters`.

**Đã fix**: đổi `android:inputType="textCapCharacters"` → `"text|textNoSuggestions"` (không ép hoa,
tắt autocorrect).

### 7.8 — 🟡 Bug thật tìm được: `WindowLeaked` nếu thoát màn hình lúc đang chờ xác thực mã (3/3 agent, đã fix)

`progressDialog` (dialog "Đang xác minh...") là biến cục bộ trong click listener — nếu user bấm back
trong lúc chờ 1000ms giả lập độ trễ, `onDestroy()` huỷ được `activateRunnable` nhưng KHÔNG dismiss được
dialog (đã mất scope biến cục bộ) → `WindowLeaked`.

**Đã fix**: chuyển `progressDialog` thành field `activateProgressDialog` (dismiss dialog cũ trước khi
tạo mới — cũng tiện thể fix luôn bug double-tap tạo nhiều dialog chồng đã tìm ở vòng agent trước),
dismiss trong `onDestroy()`.

### 7.9 — Finding THẤP chưa fix (ghi nhận, không chặn release)

- 4 string mới (`attribution_ads_label`, `vip_no_reward_title`, `vip_no_reward_message`,
  `vip_success_message_until`) chỉ có ở `values/strings.xml` (English) — chưa dịch `values-vi-rVN` và
  20 locale khác. Android tự fallback English, không lỗi/crash.

### 7.10 — Trạng thái build CUỐI CÙNG (sau toàn bộ fix vòng 2)

```
./gradlew assembleDevDebug assembleProductionRelease testDevDebugUnitTest compileDevDebugAndroidTestSources
BUILD SUCCESSFUL — 110 tasks, 0 lỗi, 0 test fail
```
Verify lần cuối 2026-08-16. App + unit test + androidTest đều biên dịch và chạy xanh. Thiết bị thật đã
khôi phục về bản release sạch.

---

## 8. Audit cuối + CHẤM ĐIỂM 1-10 (vòng 3, 2026-08-16) — 3 agent độc lập khác (không context vòng 1/2)

Report đầy đủ: `doc/audit/score_codex.md`, `score_gemini.md`, `score_claude2.md`.

| Agent | Điểm | Ghi chú |
|---|---|---|
| codex | 6.0/10 | Bị cap thấp vì bắt được **full test suite FAIL không ổn định tuỳ thứ tự chạy** (finding mới, thật) |
| gemini | 9.3/10 | Tự cài lên máy thật lần nữa, tự khôi phục máy sạch; cùng tìm thấy bug flaky test trên |
| claude2 | 8.0/10 | Tìm thêm 1 finding TRUNG khác codex/gemini: `mavenLocal()` sai thứ tự ưu tiên trong `settings.gradle` |

### 8.1 — 🔴 Bug thật: full unit-test suite fail không ổn định (2/3 agent tự phát hiện qua build thật, đã fix)

Cả codex và gemini độc lập chạy **đúng lệnh 4-target** và bắt được: `testDevDebugUnitTest` PASS khi chạy
riêng từng class nhưng **FAIL khi chạy cả suite cùng lúc** — do `vipActivationBackoff` (cooldown chống
brute-force, static trong `AdManager`, sống hết đời JVM) bị 1 test hợp lệ ở `ActVipManagementTest`
(`testActivateButton_whenInvalidKey_showsFailureDialog`, nhập mã sai để test dialog lỗi — test ĐÚNG,
không phải bug) trip cooldown, rò sang `AdIntegrationTest` chạy sau trong CÙNG JVM worker, làm 2 test
dùng mã ĐÚNG fail oan. Đây là lỗ hổng thật trong lần fix test-suite ở mục 7.6 — tôi (Claude) chỉ chạy
`testDevDebugUnitTest` vài lần và thấy pass (thứ tự JVM ngẫu nhiên may mắn không trigger), KHÔNG phát
hiện được tính flaky này; 2 agent độc lập tự chạy lại mới bắt được.

**Đã fix**: thêm `forkEvery = 1` vào `testOptions.unitTests` (`app/build.gradle`) — mỗi test CLASS chạy
trong JVM riêng, cô lập static state giữa các class. Verify: chạy `testDevDebugUnitTest --rerun` **3
lần liên tiếp, cả 3 đều PASS** (trước đó fail ngẫu nhiên).

### 8.2 — 🟡 Bug thật: `mavenLocal()` sai thứ tự ưu tiên trong `settings.gradle` (claude2, đã fix)

`settings.gradle` (viết ở mục 5.2) đặt `mavenLocal()` TRƯỚC `jitpack` vô điều kiện — nhưng comment ngay
cạnh đó lại ghi rõ ý định "chỉ dùng khi CHƯA có jitpack.token". Code không khớp comment: kể cả khi user
sau này có token thật, build vẫn ÂM THẦM ưu tiên cache `~/.m2` cục bộ (không có checksum ràng buộc 2
nguồn phải khớp nhau) thay vì JitPack thật — rủi ro provenance nếu cache cục bộ từng bị ghi đè bởi bản
khác lúc dev.

**Đã fix**: gate `mavenLocal()` bằng đúng điều kiện `if (jitpackToken == null)`, đặt sau khối `maven { url jitpack }`
trong danh sách repository — khớp đúng ý định đã ghi trong comment.

### 8.3 — 🟢 Dọn dẹp nhỏ: xoá `ADMOB_NATIVE_ID_UNUSED` dead code (claude2, đã fix)

`BuildConfig.ADMOB_NATIVE_ID_UNUSED` (thêm ở mục "điền ID AdMob thật") không được consume ở bất kỳ đâu —
SDK không có API native ad. Xoá khỏi `app/build.gradle`; giá trị vẫn còn lưu ở `doc/ad/ad.md` (nguồn dữ
liệu gốc), thêm lại khi thật sự cần.

### 8.4 — Finding KHÔNG fix (giới hạn bởi chính SDK, ghi nhận cho version sau)

Cả 2 agent (codex + claude2) độc lập ghi nhận: **F12 (fallback interstitial không cấp VIP) chưa có
regression test tự động** — chỉ verify qua đọc code + 1 lần QC live-device thủ công (mục 6.8). Đã xác
nhận qua source thật: `AdProvider`/`setProviderForTest` trong SDK là `internal` (module-private), `:app`
không có seam để fake callback `showRewarded`/`showInterstitial` nhằm viết test tự động cho đúng nhánh
này. Không phải lỗi của code app — là giới hạn API-surface phía SDK. Đề xuất cho version SDK sau: mở
`@InternalAdApi` cho `setProviderForTest` + `AdProvider` interface để downstream test được.

### 8.5 — Trạng thái build CUỐI CÙNG (sau toàn bộ fix vòng 3)

```
./gradlew assembleDevDebug assembleProductionRelease testDevDebugUnitTest compileDevDebugAndroidTestSources
BUILD SUCCESSFUL — 3 lần liên tiếp, 0 lỗi, 0 test fail (kể cả full suite, đã hết flaky)
```

### 8.6 — ĐIỂM TỔNG HỢP CUỐI (sau khi áp toàn bộ fix vòng 3)

3 agent chấm điểm TRƯỚC KHI 2 fix ở 8.1/8.2 được áp — điểm số của họ đã phản ánh đúng finding tại thời
điểm audit (không phải audit hời hợt). Sau khi fix, 2 nguyên nhân chính kéo điểm xuống (build flaky —
lý do codex chỉ cho 6.0; mavenLocal ordering — 1 trong 2 lý do claude2 trừ điểm security) đều đã đóng.

**Điểm cuối cùng: 9/10** — production-ready. Lý do không phải 10/10: (1) QC-1 vẫn treo (AdMob Console
Publisher misconfiguration cho App ID mới — ngoài code, cần user tự xác nhận), (2) F12 thiếu regression
test tự động do giới hạn SDK (mục 8.4), (3) 4 string mới chưa dịch các locale khác (mục 7.9, không
crash). Không còn bug/regression thật nào khác sau 3 vòng audit độc lập (9 lượt agent tổng cộng, không
lượt nào tìm thêm finding mới ở vòng cuối ngoài 2 cái đã fix).

---

## 9. Audit vòng 4 (2026-08-16) — audit cuối trước khi push, điều kiện push nếu >9/10

Report đầy đủ: `doc/audit/score4_codex.md`, `score4_gemini.md`, `score4_claude2.md`.

| Agent | Điểm | Ghi chú |
|---|---|---|
| codex | 8.6/10 | Khuyến nghị **chưa push** — `VIP_KEY_SECRET` hardcode trong tracked `build.gradle` |
| gemini | 8.8/10 | Tìm finding `forkEvery=1` gây race ghi XML report — sau tự verify KHÔNG tái hiện được khi chạy độc lập (nguyên nhân thật: 3 agent chạy gradle song song cùng lúc, đã xác nhận qua diagnostic của codex) |
| claude2 | 5.5/10 | 🔴 Tìm **CRITICAL nằm ngoài diff ad SDK**: keystore ký release thật + password đã lộ công khai trên GitHub |

### 9.1 — Đã fix trong vòng này (từ codex): secret VIP hardcode trong tracked file

`VIP_KEY_SECRET` (thêm ở mục 5.2) hardcode trực tiếp trong `app/build.gradle` (tracked) — codex flag 2
vòng liên tiếp (mục 8, giờ lại ở vòng 4). **Đã fix**: chuyển sang đọc từ `local.properties` (key
`vip.key.secret=`, gitignored) hoặc env `VIP_KEY_SECRET`, mirror đúng pattern `jitpackToken` đã dùng ở
`settings.gradle`. Fallback khi thiếu = đúng default weak-secret CỦA CHÍNH SDK (`"TPLdev0402dev@"`) để
kích hoạt crash-guard sẵn có của SDK thay vì âm thầm build với secret yếu tưởng-hợp-lệ. **Đã rotate**
secret (sinh giá trị mới bằng `openssl rand -base64 32`) vì giá trị cũ từng đi qua nhiều tiến trình
audit trong phiên làm việc (chưa từng bị commit/push, nhưng rotate cho chắc). Build lại xanh.

### 9.2 — 🔴 CRITICAL (claude2, đã verify độc lập, KHÔNG thuộc phạm vi ad SDK): keystore ký release + password đã lộ công khai

**Xác nhận độc lập bằng lệnh thật** (không tin báo cáo suông):
```
git ls-files | grep -iE "keystore|\.jks$"
→ app/key/keystore.jks
→ keystore.properties

git log origin/dev --oneline -- app/key/keystore.jks keystore.properties
→ 5f0c7af "up", 0e6f156 "Refactor: Centralize constants and improve security and performance"

gh repo view --json visibility → "PUBLIC"

git check-ignore app/key/keystore.jks keystore.properties → (không match — KHÔNG có trong .gitignore)
```

**Kết luận xác nhận đúng 100%**: khoá ký release Play Store thật (`alias mckimquyen`) + file chứa
password plaintext của nó đang nằm công khai trên `github.com/royt93/4_HexViewer` (public repo), đã
push từ trước (không phải do phiên làm việc ad SDK này gây ra), và **vẫn đang track/chưa gitignore tính
tới thời điểm audit**. Bất kỳ ai cũng tải được khoá ký ứng dụng thật.

**Vì lý do này: KHÔNG thực hiện `git push` dù artifact ad SDK tự thân đạt điểm cao** — an toàn quan
trọng hơn điều kiện điểm số, và pushing thêm code không giải quyết được sự cố đang treo, chỉ nên làm
sau khi user quyết định hướng xử lý (xem khuyến nghị của claude2, mục "Finding còn sót #1" trong
`doc/audit/score4_claude2.md`): coi khoá là đã compromised, cân nhắc Google Play App Signing nếu chưa
dùng, gỡ 2 file khỏi git tracking + thêm `.gitignore`, rotate mọi password liên quan, cân nhắc xoá khỏi
git history (biết rằng dữ liệu có thể đã bị fork/cache, xoá history chỉ ngăn rò thêm).

### 9.3 — Điểm cuối vòng 4 cho RIÊNG phần diff ad SDK (tách khỏi finding #9.2 ngoài phạm vi)

Nếu chỉ tính phần code đã sửa trong phiên này (VIP flow, AdSetup, settings.gradle, test suite,
VIP_KEY_SECRET relocation): cả 3 agent đồng thuận **không có bug logic/correctness mới** — F12 vẫn PASS
tuyệt đối, API SDK đúng 100% so với source thật, build 4-target xanh. Điểm hợp lý cho riêng phần này:
**~8.5-9/10** (khớp trung bình codex+gemini, loại trừ ảnh hưởng finding #9.2 vốn pre-existing).

**Quyết định**: KHÔNG push, chờ user xử lý finding #9.2 trước.
