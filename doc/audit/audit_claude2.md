# Audit độc lập — claude2 — 2026-08-16

Nguồn đối chiếu: `doc/AD_PROMPT_AOS.MD` (đọc full 2067 dòng) + code thật trong project + source thật
của `AdmobApplovinWrapper` tag `1.1.5` lấy trực tiếp từ GitHub API (`gh api repos/royt93/...git/blobs`,
không đoán) để xác nhận hành vi SDK version đang dùng, không chỉ dựa vào mô tả trong doc (doc mô tả
hành vi bản MỚI NHẤT, không phải 1.1.5).

## Xác nhận đầu mối nghi vấn đã cho

**1. SDK version 1.1.5 vs tối thiểu 1.6.2 — ĐÚNG, và hậu quả NẶNG HƠN doc liệt kê.**
- `app/build.gradle:198` → `implementation("com.github.royt93:AdmobApplovinWrapper:1.1.5")`.
- Tag mới nhất thật trên GitHub (verify qua `gh api repos/royt93/AdmobApplovinWrapper/tags`):
  **1.6.16** (1.6.0 → 1.6.16 đều tồn tại, doc nói tag `1.6.0` không có artifact JitPack — không verify
  được JitPack trực tiếp vì bị chặn 403, nhưng tag git có thật).
- Đọc thẳng source thật tại tag `1.1.5`
  (`admob-wrapper-sdk/src/main/java/com/roy/sdkadbmob/AdManager.kt`, hàm `initialize()` dòng 360-395):
  **`setConfig()` tạo `provider` NGAY LẬP TỨC (đồng bộ), và `initialize()` gọi thẳng
  `MobileAds.initialize()` / `AppLovinSdk...initialize()` KHÔNG hề có bước chờ consent nào** — kiến
  trúc "consent-first / WAITING_FOR_CONSENT / fail-closed GDPR" mà doc mô tả (dòng 236-238, 810-825
  của `AD_PROMPT_AOS.MD`) **KHÔNG TỒN TẠI ở bản 1.1.5**, đây là tính năng thêm ở bản mới hơn. Do
  `MyApplication.setupAd()` (`MyApplication.java:601-623`) gọi `initialize()` ngay trong
  `Application.onCreate()`, TRƯỚC khi `SplashActivity` kịp gọi `requestConsentInfoUpdate()` — nghĩa là
  AppLovin SDK (đang là provider active vì `IS_ENABLE_ADMOB=false`) init và có thể fetch
  GAID/App Set ID/network call **trước khi có bất kỳ quyết định consent nào của user**. Đây là rủi ro
  GDPR thật, không phải suy diễn — verify bằng source code thật, không phải bằng mô tả doc.
- Các fix bảo mật S1-S4 (`AD_PROMPT_AOS.MD:1466-1488`: crash khi vipKeySecret mặc định, chặn token
  replay, CAS guard chống double-show rewarded) **đều không có trong 1.1.5** — verify bằng cách đọc
  `activateVipByKey` thật tại tag 1.1.5 (xem mục VIP bên dưới), không thấy check `IllegalStateException`
  nào cho vipKeySecret yếu.
- **Kết luận**: SAI, mức CAO — không chỉ "thiếu vá lỗi VIP cấp thầm lặng" như đầu mối gợi ý, mà còn
  thiếu toàn bộ tầng consent-gating ở SDK, tức là **app hiện tại có nguy cơ vi phạm GDPR thật** khi
  build lên Play Store phục vụ EEA/UK/CH.

**2. `vipKeySecret = VipKeys.VIP_30D_KEY` — ĐÚNG nhưng bản chất khác đầu mối nghi vấn mô tả.**
- `MyApplication.java:614` → `vipKeySecret = com.galaxyjoy.hexviewer.feature.vip.VipKeys.INSTANCE.getVIP_30D_KEY()`.
- Đọc `AdSdkConfig.kt` thật tại tag 1.1.5: KDoc field `vipKeySecret` ghi rõ **"Key secret user nhập để
  unlock VIP"** — ở bản 1.1.5, `vipKeySecret` ĐÚNG LÀ thiết kế để làm "1 mã VIP duy nhất người dùng
  gõ vào", KHÔNG PHẢI cơ chế "chống-tamper prefs" tách biệt khỏi VIP key thật như doc mới mô tả (đó là
  kiến trúc từ bản có `vipTokenPublicKey`/token ECDSA, ≥1.2.0). Vậy so với **thiết kế của chính bản
  1.1.5**, việc set `vipKeySecret = VIP_30D_KEY` không sai cú pháp.
- **Nhưng phát hiện thật (tự tìm, xem mục Finding mới #1 bên dưới): code app gọi `activateVipByKey`
  SAI CÁCH — không truyền input user, mà truyền lại chính `adConfig.vipKeySecret`**, khiến check
  `key != adConfig.vipKeySecret` trong SDK (`AdManager.kt:1716` tại tag 1.1.5) LUÔN LUÔN pass (so sánh
  1 giá trị với chính nó). Toàn bộ việc "verify key" thật sự nằm ở `VipKeys.lookupDays()` phía app
  (client-side, dễ dịch ngược qua Base64 — xem bằng chứng decode bên dưới), không phải ở SDK.
- **Kết luận**: ĐÚNG MỘT PHẦN — giá trị dùng làm `vipKeySecret` không "sai" theo API 1.1.5, nhưng cách
  dùng ở `ActVipManagement.kt` khiến verify SDK trở thành no-op, và khi nâng SDK lên ≥1.2.0 (bắt buộc
  theo mốc tối thiểu 1.6.2 của doc), toàn bộ luồng `activateVipByKey` này sẽ **fail im lặng** vì nhánh
  legacy plaintext mặc định TẮT (`allowLegacyPlaintextVipKey=false` theo doc dòng 450-451, 594-597) —
  đây là điểm migrate-breaking thật, mức CAO.

**3. `AdSdkConfig` thiếu `vipTokenPublicKey`, `applovinPrivacyPolicyUrl`, `appOpenExcludedActivities`,
`applovinHasUserConsent`, `paidEventListener`, `errorReporter` — ĐÚNG, nhưng vì các field này KHÔNG TỒN
TẠI ở bản 1.1.5 đang dùng.**
- Đọc `AdSdkConfig.kt` thật tại tag 1.1.5 (13 field, xem transcript trên): chỉ có
  `isEnableAdmob, isDebug, admobAppOpenId, admobInterstitialId, admobBannerId, admobRewardedId,
  applovinAppOpenId, applovinInterstitialId, applovinBannerId, applovinRewardedId, safety,
  vipKeySecret, applovinSdkKey` — KHÔNG có 6 field trên. `MyApplication.java:602-616` truyền đúng
  13 field này theo đúng constructor thật của 1.1.5 — code KHÔNG "quên" set, mà đơn giản API đó chưa
  ra đời.
- Hậu quả thật (không phải suy diễn theo doc bản mới): **không có `appOpenExcludedActivities` nghĩa là
  không có cách khai trừ Splash khỏi App Open tự động** ở tầng SDK 1.1.5 — nhưng do
  `SplashActivity.checkShowAd()` (`SplashActivity.java:138-159`) tự quản lý flow qua
  `initSplashScreen`/`requestConsentInfoUpdate` thủ công (không dùng auto-resume App Open của
  `ProcessLifecycleOwner` ở tầng khác), rủi ro đè App Open lên Splash **thấp hơn** so với lo ngại ban
  đầu — cần verify thêm ở `ActMain.java` xem App Open có tự resume đè lên activity khác không.
  Không có `errorReporter`/`paidEventListener` nghĩa là **zero crash reporting cho lỗi ad, zero revenue
  tracking** — xác nhận đúng, mức TRUNG (không phải chặn release nhưng mất khả năng đo ROAS/debug).
- **Kết luận**: ĐÚNG về hiện trạng, nhưng root cause là **version quá cũ** chứ không phải thiếu sót khi
  gọi constructor. Không thể fix bằng cách thêm tham số vào constructor hiện tại — phải nâng SDK trước.

**4. Không tìm thấy `AdManager.setTestDeviceIds(...)` — ĐÚNG.**
- Grep toàn bộ `app/src/main/java` không có lời gọi `setTestDeviceIds`. Verify hàm này **có tồn tại**
  ở SDK 1.1.5 (`AdManager.kt:597` tại tag 1.1.5), nên có thể gọi ngay không cần nâng version trước.
- **Kết luận**: ĐÚNG, mức CAO — QA/dev hiện đang click ad-unit ID thật (xem mục #5) không được khai
  test device, rủi ro khoá tài khoản AdMob thật, có thể fix ngay không phụ thuộc việc nâng SDK.

**5. `ADMOB_REWARDED_ID` release = ID demo Google `ca-app-pub-3940256099942544/5224354917` — ĐÚNG.**
- `app/build.gradle:93` (block `release`). Đây đúng là **Google Test Rewarded Ad Unit ID chính thức**
  (publisher ID demo `3940256099942544` — trùng publisher ID dùng cho toàn bộ 4 ID demo ở block
  `debug` dòng 111-114) — không phải trùng hợp, là ID mẫu Google công bố công khai trong tài liệu
  AdMob quickstart.
- Rủi ro thực tế **hiện tại thấp** vì `IS_ENABLE_ADMOB=false` cả debug/release
  (`app/build.gradle:94,115`) → nhánh AdMob (bao gồm rewarded ID này) hoàn toàn không được
  `initialize()` chạm tới (xác nhận qua source 1.1.5: `initialize()` chỉ gọi `MobileAds.initialize()`
  khi `adConfig.isEnableAdmob==true`). Nhưng đây là **rủi ro tiềm ẩn (dead code hiện tại, live bomb khi
  flip flag)** — nếu user bật `IS_ENABLE_ADMOB=true` theo đúng yêu cầu "ưu tiên AdMob" (xem mục #6) mà
  quên sửa dòng 93, bản release sẽ serve test ad thật lên production ngay lập tức: vi phạm policy AdMob
  ("Test ads must not be served in production") + zero revenue rewarded.
- **Kết luận**: ĐÚNG, mức CAO — phải sửa TRƯỚC KHI flip `IS_ENABLE_ADMOB=true`, không phải sau.

**6. `IS_ENABLE_ADMOB=false` cả debug/release, user muốn ưu tiên AdMob — ĐÚNG hiện trạng.**
- `app/build.gradle:94` (release), `:115` (debug) — cả hai `false`. Toàn bộ 4 slot AdMob
  (banner/interstitial/appopen/rewarded) hiện là dead-config, AppLovin đang là provider chạy thật.
- Doc (`AD_PROMPT_AOS.MD:630`, mục "Cấm") liệt kê **"Tự ý ... đổi flag `IS_ENABLE_ADMOB`"** — nhưng
  chính spec cũng ghi rõ đây là cấm áp dụng cho **AI tự ý đổi không hỏi**, không cấm đổi khi có yêu cầu
  rõ ràng từ user/chủ project (bối cảnh nhiệm vụ audit này xác nhận user có yêu cầu). Về mặt kỹ thuật,
  đổi flag này AN TOÀN ở tầng code (constructor 1.1.5 đọc `config.isEnableAdmob` đúng nghĩa), nhưng
  **PHẢI làm sau khi**: (a) sửa ID demo ở mục #5, (b) xác nhận App ID thật ở mục #7, (c) Publish UMP
  European regulations message thật trên AdMob Console (Step 8, ngoài phạm vi code) — nếu không sẽ
  đúng kịch bản "GDPR fail-closed → zero ad" (với SDK mới) hoặc "ad chạy không consent" (với SDK 1.1.5
  hiện tại, theo mục #1).
- **Kết luận**: ĐÚNG hiện trạng + đánh giá đổi flag là hợp lý về code nhưng phải làm sau các bước dọn
  dẹp trên, không phải đổi ngay.

**7. AndroidManifest App ID `ca-app-pub-3612191981543807~2249113565`, comment "Sample AdMob App ID" —
KHÔNG XÁC ĐỊNH được 100% nhưng có bằng chứng mạnh đây LÀ App ID thật, comment sai/gây hiểu lầm.**
- `AndroidManifest.xml:38-39`.
- Publisher ID `3612191981543807` (phần trước dấu `~`) **trùng khớp** với publisher ID dùng ở toàn bộ
  4 ad-unit ID production thật trong `app/build.gradle:90-93` (block `release`:
  `ca-app-pub-3612191981543807/5282471019`, `/2777620817`, `/4547973843`) — đây KHÔNG phải publisher
  ID demo của Google (`3940256099942544`, xuất hiện riêng ở block `debug`). Google demo App ID chính
  thức là `ca-app-pub-3940256099942544~3347511713` — khác hoàn toàn số này.
- **Kết luận**: KHÔNG tự khẳng định 100% (không có quyền truy cập AdMob Console của user để verify),
  nhưng bằng chứng số publisher ID trùng khớp ID production thật là rất mạnh — **cần user xác nhận
  đây là App ID thật của chính app** (rất có khả năng comment "Sample AdMob App ID" chỉ là sao chép sót
  từ template gốc, không phản ánh giá trị thật đã điền vào).

**8. Thiếu `<attribution>` tag trong manifest — ĐÚNG.**
- Đọc toàn bộ `AndroidManifest.xml` (248 dòng): không có tag `<attribution>` nào.
- `targetSdk 36` (`app/build.gradle:14`) ≥ 31 → đúng điều kiện Step 6 (`AD_PROMPT_AOS.MD:766-770`)
  yêu cầu khai báo, nếu thiếu sẽ có warning `attributionTag not declared` liên tục trong logcat.
- **Kết luận**: ĐÚNG, mức THẤP (chỉ warning logcat, không crash/không policy risk) — fix dễ, thêm 1
  dòng trước `<application>`.

**9. Chưa rõ Step 8 (UMP publish) / VIP token ECDSA keypair đã sinh chưa — XÁC NHẬN CHƯA LÀM, và
KHÔNG THỂ làm được với SDK 1.1.5 hiện tại.**
- Không tìm thấy bất kỳ tham chiếu `generateVipKeyPair`, `vipTokenPublicKey`, `activateVipByToken`
  trong toàn bộ `app/src/main/java` — hợp lý vì các API này **không tồn tại ở SDK 1.1.5** (verify qua
  source thật, không thấy các hàm này trong `AdManager.kt` tag 1.1.5).
  → VIP token ECDSA **không thể** đã được sinh/dùng, vì API chưa tồn tại trong bản đang chạy.
- Step 8 (Publish UMP European regulations message trên AdMob Console) là thao tác ngoài code — không
  audit được bằng cách đọc file, đúng như giới hạn task đã nêu (cần user tự xác nhận qua ảnh chụp màn
  hình theo mẫu ở `AD_PROMPT_AOS.MD:1559-1591`).

## Finding mới tự tìm thêm

**F1 — [CAO] `activateVipByKey` bị gọi sai cách, tự vô hiệu hoá cơ chế verify của SDK.**
- `feature/vip/ActVipManagement.kt:144-145` (nút "Activate") và `:242-243` (grant từ rewarded ad):
  ```kotlin
  val secretKey = AdManager.adConfig.vipKeySecret
  val success = AdManager.activateVipByKey(this, secretKey, days)
  ```
  Tham số `key` truyền vào SDK luôn là `AdManager.adConfig.vipKeySecret` (hằng số cấu hình sẵn từ
  `MyApplication.java:614`) — KHÔNG PHẢI chuỗi user vừa gõ (`inputKey`, biến này chỉ được dùng để
  `VipKeys.lookupDays(inputKey)` lấy số ngày, rồi bị bỏ rơi). Vì SDK check
  `key != adConfig.vipKeySecret` (`AdManager.kt:1716` tại tag 1.1.5) so sánh giá trị với chính nó, kết
  quả LUÔN `true` — verify SDK trở thành no-op. An toàn thực tế của tính năng "nhập mã VIP" hiện **hoàn
  toàn phụ thuộc** vào `VipKeys.lookupDays()` (client-side, `VipKeys.kt:24-25`), không phải cơ chế
  SDK. Không phải bug crash, nhưng là **thiết kế sai kiến trúc**: nếu sau này SDK đổi semantics của
  `vipKeySecret` (đã đổi ở bản mới — xem xác nhận #2 ở trên) mà code app không sửa theo, tính năng sẽ
  fail toàn bộ trong im lặng (trả `false`, không throw) — dễ sót qua QA nếu QA không test kỹ nhánh
  nhập mã.
- Fix theo đúng đường doc khuyến nghị khi nâng SDK: chuyển hẳn sang token ECDSA
  (`generateVipToken`/`activateVipByToken`) cho VIP giá trị cao, giữ `VipKeys` map hiện tại làm
  "thẻ cào" qua `AdSdkConfig.vipRedeemCodes` (field có sẵn từ SDK mới, xem `AD_PROMPT_AOS.MD:1752`)
  thay vì tự chế cơ chế gọi ngược `vipKeySecret`.

**F2 — [TRUNG] `VIP_30D_KEY`/`VIP_3D_KEY` chỉ obfuscate bằng Base64, không phải mã hoá — giải mã tức thì.**
- `feature/vip/VipKeys.kt:5-6` — `VIP_30D_B64`/`VIP_3D_B64` decode ra plaintext ngay bằng
  `base64 -d` (đã tự verify: `VIP_30D_KEY = "9fA0q7eN!27cLx04@21993Y2u0I7#Q0"`,
  `VIP_3D_KEY = "eQ7@93L0f!2Y2707xN04021993u0I#2aK"`). Base64 KHÔNG phải bảo mật — bất kỳ ai
  `apktool`/`jadx` decompile APK đều lấy được 2 mã này trong vài giây, sau đó nhập vào bất kỳ máy nào
  để có VIP miễn phí vĩnh viễn (không tự hết hạn, không bind thiết bị vì đây là plaintext-key path,
  không phải token). Đây đúng loại rủi ro doc đã cảnh báo cho "thẻ cào" (`AD_PROMPT_AOS.MD:465-472`:
  "1 mã lộ lên mạng → mọi máy chưa dùng đều redeem được") — nhưng ở project này rủi ro **cao hơn thẻ
  cào thường** vì thẻ cào thường không đồng thời là `vipKeySecret` bảo vệ tầng SDK (xem F1: 2 vai trò
  bị trộn lẫn).
- Không phải bug mới cần code fix gấp (chấp nhận được cho VIP giá trị thấp theo đúng khuyến nghị doc),
  nhưng cần ghi nhận rõ trong tài liệu nội bộ: đây là "thẻ cào" thực chất, KHÔNG phải bảo mật thật.

**F3 — [TRUNG] `AdManager.initialize()` chạy trước consent ở SDK 1.1.5 — xem chi tiết ở xác nhận #1.**
- Đã trình bày đầy đủ ở trên, nhắc lại vì đây là finding tự đào sâu (không có trong đầu mối gốc), mức
  độ nghiêm trọng cao nhất trong toàn bộ audit này vì liên quan trực tiếp compliance pháp lý.

**F4 — [THẤP] `MyApplication.java:43` còn TODO nhắc nhở tồn đọng.**
- `//TODO roy93~ why you see ad` — dòng TODO không rõ nghĩa, còn sót lại từ quá trình tích hợp. Không
  ảnh hưởng chức năng, nên dọn trước khi audit/migrate xong để tránh nhiễu code review sau này.

## Kết luận GO/NO-GO cho việc build lại migrate plan

**NO-GO** cho việc build release/production ngay ở trạng thái hiện tại. Lý do xếp theo mức độ:

1. **CAO — Compliance/pháp lý**: SDK 1.1.5 không có consent-gating ở tầng provider init (F3/#1) →
   khởi tạo AppLovin trước khi có quyết định GDPR. Bắt buộc nâng SDK lên tối thiểu 1.6.2 (theo đúng
   mốc doc yêu cầu) TRƯỚC khi cân nhắc bất kỳ thay đổi nào khác.
2. **CAO — Tài khoản ads**: chưa gọi `setTestDeviceIds` (#4) + demo rewarded ID nằm trong release
   (#5) — 2 việc này sửa được ngay, không cần đợi nâng SDK, nên làm trước để bảo vệ tài khoản trong
   lúc QA các bước tiếp theo.
3. **CAO — Kiến trúc VIP sẽ vỡ khi nâng SDK**: F1 + xác nhận #2 — luồng `activateVipByKey` hiện tại
   gắn chặt với semantics cũ của `vipKeySecret` (1.1.5). Nâng SDK lên ≥1.2.0 mà không viết lại theo
   token ECDSA/`vipRedeemCodes` sẽ làm tính năng nhập mã VIP và "xem ad → VIP" **fail toàn bộ trong im
   lặng** (nhánh legacy plaintext mặc định tắt).
4. **CẦN USER XÁC NHẬN trước khi code tiếp**: App ID thật ở manifest (#7), và quyết định cuối có
   flip `IS_ENABLE_ADMOB=true` ngay hay giữ AppLovin tới khi UMP Console (Step 8) publish xong.

**Khuyến nghị thứ tự migrate plan**: (a) nâng SDK lên tag mới nhất verify được qua JitPack thật (không
tự tin dùng số trong doc vì có thể lỗi thời) → (b) viết lại toàn bộ VIP flow theo token ECDSA +
`vipRedeemCodes`, bỏ cách gọi `activateVipByKey(this, adConfig.vipKeySecret, days)` hiện tại → (c) thêm
`setTestDeviceIds`, sửa demo rewarded ID, thêm `<attribution>` tag, thêm `paidEventListener`/
`errorReporter`/`appOpenExcludedActivities` (giờ đã có field) → (d) chỉ flip `IS_ENABLE_ADMOB=true`
sau khi Step 8 (UMP publish, ảnh chụp màn hình) hoàn tất và App ID thật đã xác nhận với user.
