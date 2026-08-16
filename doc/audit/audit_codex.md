# Audit độc lập — codex — 2026-08-16

Phạm vi: audit read-only theo `doc/AD_PROMPT_AOS.MD`; không build và không xác nhận thay các bước dashboard/device ngoài source.

## Xác nhận đầu mối nghi vấn đã cho

1. **ĐÚNG — CAO: SDK quá cũ.** `app/build.gradle:198` pin `1.1.5`, thấp hơn minimum `1.6.2` (`AD_PROMPT_AOS.MD:23-30`). `git ls-remote --tags` ngày audit cho tag semver mới nhất **1.6.16**; endpoint artifact JitPack không xác minh được vì trả HTTP 401, nên cần kiểm tra build-status 1.6.16 trên JitPack trước khi pin. Khi nâng từ ≤1.5.5: `grantRewardOnEarn` đổi `false→true`, callback reward chạy khi ad còn phủ màn hình; phải chỉ cấp thưởng ở một trong 3 kênh và dedup bằng `rewardTransactionId`; `AdSafetyStatus.totalImpressions/fullscreenClicks` đổi thành `ctrWindowImpressions/ctrWindowClicks`; session reset sau background ≥30 phút nên ad/user/ngày có thể tăng (`AD_PROMPT_AOS.MD:171-220`). Ngoài ra 1.6.2 sửa mất callback khi token được nhập lúc SDK chưa init (`AD_PROMPT_AOS.MD:23-25`).

2. **ĐÚNG — CAO: `vipKeySecret` dùng sai mục đích.** `MyApplication.java:614` lấy `VipKeys.VIP_30D_KEY`; key đó nằm trong whitelist redeem 30 ngày (`VipKeys.kt:5,8-20`). Spec yêu cầu secret ngẫu nhiên ≥16 ký tự, riêng biệt, inject qua BuildConfig để ký chống-tamper prefs; nó không phải redeem key/token verifier (`AD_PROMPT_AOS.MD:589-591,1468-1474,1746`). APK có thể decode Base64, nên người trích key vừa biết redeem credential vừa biết HMAC secret; làm suy yếu integrity prefs và chia sẻ một credential cho hai trust-domain.

3. **ĐÚNG — CAO/TRUNG: constructor thiếu các field mới.** Danh sách đối số kết thúc tại `applovinSdkKey` (`MyApplication.java:602-616`):
   - `vipTokenPublicKey` (**CAO**): với token ECDSA, default/sample key khiến release từ chối im lặng mọi token (`activateVipByToken=false`); code hiện còn dùng legacy plain key (`ActVipManagement.kt:127-156`).
   - `appOpenExcludedActivities` (**TRUNG**): không khai class reference cho splash; ở bản cũ tên `SplashActivity` có thể còn được default string deprecated chặn, nhưng không được dựa vào default sẽ bị gỡ (`AD_PROMPT_AOS.MD:359-367,1500-1511`).
   - `applovinPrivacyPolicyUrl` (**TRUNG**): URL đã có ở `build.gradle:35` nhưng không truyền vào config; không thể bật native MAX CMP và consent UI/config không nhận URL theo spec.
   - `applovinHasUserConsent` (**KHÔNG PHẢI LỖI cho gms hiện tại**): để `null` là đúng khi Splash chạy UMP; nếu build `nongms` thì `null` chặn toàn bộ ad cho tới khi CMP trả true/false. Project dùng JitPack gms và Splash gọi UMP (`SplashActivity.java:148-158`).
   - `paidEventListener` (**TRUNG**): không có revenue callback trong `Application`, nên mất tracking doanh thu/ROAS; listener phải set ở Application để không bị auto-clear theo Activity (`AD_PROMPT_AOS.MD:342-357`).
   - `errorReporter` (**TRUNG**): lỗi SDK không được forward Crashlytics/Sentry, giảm khả năng phát hiện lỗi production (`AD_PROMPT_AOS.MD:485-496`). Đây là property của `AdManager`, không phải field A.7 của constructor.

4. **ĐÚNG — CAO trước QA bằng production IDs.** Callback init tại `MyApplication.java:618-622` không gọi `AdManager.setTestDeviceIds(...)`; repo-wide search không có call nào. Trong khi release chứa ad units thật ở `build.gradle:90-92`, QA không đăng ký device có thể tạo invalid traffic/account risk (`AD_PROMPT_AOS.MD:292-315,1780`).

5. **ĐÚNG — CAO: Google demo rewarded ID nằm trong release.** `app/build.gradle:93` dùng `ca-app-pub-3940256099942544/5224354917`; Google liệt kê chính xác đây là demo Rewarded Ads ID và yêu cầu thay trước publish. Hậu quả chắc chắn: rewarded release không ghi doanh thu/report vào account; vi phạm release gate “release không dùng test inventory” (`AD_PROMPT_AOS.MD:621-622,1030`).

6. **ĐÚNG — cần đổi theo yêu cầu chủ project.** `IS_ENABLE_ADMOB=false` ở cả release/debug (`build.gradle:94,115`) nên effective provider là AppLovin. User đã yêu cầu ưu tiên AdMob, đủ thẩm quyền để đổi thành `true` khi migrate; khi đổi, bốn AdMob IDs và App ID/UMP production phải hợp lệ. Hiện rewarded release chưa hợp lệ nên chưa được flip rồi ship.

7. **KHÔNG XÁC ĐỊNH — App ID cần owner xác nhận.** Manifest khai `ca-app-pub-3612191981543807~2249113565` tại `AndroidManifest.xml:36-39`, đúng cú pháp App ID nhưng comment gọi “Sample”. Không có bằng chứng source chứng minh ownership/mapping với package; phải xác nhận trong AdMob Console.

8. **ĐÚNG — TRUNG: thiếu attribution tag.** `targetSdk=36` (`build.gradle:14`), nhưng phần manifest trước `<application>` (`AndroidManifest.xml:1-19`) không có `<attribution>`; vi phạm Step 6 và gây warning `attributionTag not declared` (`AD_PROMPT_AOS.MD:759-770`).

9. **KHÔNG XÁC ĐỊNH — consent/dashboard và keypair chưa có bằng chứng.** Code có UMP call (`SplashActivity.java:148-158`) nhưng source không chứng minh European/US message đã Publish, privacy URL public, app-ads.txt, accept/deny/revoke/offline-retry, MAX Privacy States/Test Mode. Không có `vipTokenPublicKey` hay `activateVipByToken` (`MyApplication.java:602-616`, `ActVipManagement.kt:127-156`), nên chưa có bằng chứng keypair ECDSA production.

## Finding mới tự tìm thêm

1. **CAO — cấp VIP sau interstitial, vi phạm policy bắt buộc.** Khi rewarded không earned, fallback interstitial nếu `shown=true` vẫn gọi `grantVipFromAd()` (`ActVipManagement.kt:184-200`). Step 7 rule 6 cấm tuyệt đối cấp reward từ non-rewarded (`AD_PROMPT_AOS.MD:773-784`). Fix: fallback chỉ hiển thị thông báo không nhận thưởng, không gọi bất kỳ grant API nào.

2. **CAO — đường cấp VIP từ rewarded/key sai API và có thể fail im lặng sau upgrade.** `grantVipFromAd()` gọi `activateVipByKey(adConfig.vipKeySecret, 3)` (`ActVipManagement.kt:241-250`); nút nhập key cũng validate input nhưng lại activate bằng secret (`ActVipManagement.kt:127-156`). Spec yêu cầu earned reward dùng `AdManager.grantVipDays(ctx, 3)`; legacy plaintext default tắt ở 1.2.0+ nên cách hiện tại có thể fail im lặng (`AD_PROMPT_AOS.MD:448-453,1156-1157`). Fix: reward → `grantVipDays`; redeem production → ECDSA `activateVipByToken(input)` và public key riêng.

3. **TRUNG — app tự lưu `grantedAtMs`, trái single source of truth.** `VipPrefs.kt:8-10`, ghi tại `ActVipManagement.kt:148,245`, đọc tại `ActVipManagement.kt:256`. Dữ liệu lệch khi auto-trial/token/`grantVipDays` cấp VIP. Fix: bỏ ba API timestamp app-side và dùng `AdManager.getVipGrantedAtMs()`; `VipPrefs` chỉ giữ `user_redeemed_once` (`AD_PROMPT_AOS.MD:1295-1316`).

4. **TRUNG — đánh dấu “user redeemed” cho VIP từ quảng cáo.** `grantVipFromAd()` gọi `markUserRedeemed()` (`ActVipManagement.kt:245-246`), làm sai phân loại grace/redeem. Flag chỉ dành cho lần user tự nhập key/token (`AD_PROMPT_AOS.MD:1312-1316`). Fix: không set flag ở reward path.

5. **TRUNG — banner auto-managed nhưng vẫn destroy tay.** `loadBanner(..., true)` ở `ActMain.java:764-770`, sau đó gọi `bannerDestroy` tay ở `ActMain.java:223-231,755-760`. Contract cấm phối hợp `autoManageLifecycle=true` với forward lifecycle thủ công (`AD_PROMPT_AOS.MD:729-747,1641-1644`). Fix: để SDK auto-manage hoàn toàn, hoặc chuyển `false` và forward đủ resume/pause/destroy; cần giữ workaround WebView thì chọn một chế độ rõ ràng và test leak/impression.

6. **TRUNG — timeout Splash tự chế ngắn hơn watchdog SDK và bỏ qua ad-flow.** Sau 5 giây, app gọi thẳng Main (`SplashActivity.java:145-175`) thay vì chờ callback/`initSplashScreen`; spec yêu cầu wrapper tự quản watchdog/await flow và offline/reconnect retry (`AD_PROMPT_AOS.MD:725-728,810-825`). Hậu quả: cold-start chậm có thể luôn mất App Open window, callback consent về sau bị bỏ do Activity destroy. Fix: bỏ timeout 5s app-side, dùng đúng watchdog/hard-cap của wrapper.

7. **THẤP — callback revenue/analytics toàn cục chưa cấu hình.** Ngoài `paidEventListener` thiếu nêu trên, `analyticsReporter` A.8.1 cũng không được cấu hình trong `MyApplication.java:602-622`; nếu app cần ROAS qua MMP thì hiện không có đường forward. Fix theo nhu cầu analytics, không phải gate hiển thị ad.

## Kết luận GO/NO-GO cho việc build lại migrate plan

**NO-GO để build/publish production hiện trạng.** Blocker tối thiểu: nâng SDK (xác minh artifact/tag), sửa interstitial cấp VIP, chuyển reward sang `grantVipDays`, tách secret + ECDSA public key, thay rewarded release ID, bật AdMob theo yêu cầu, đăng ký test devices, khai attribution/exclusion, và xác nhận App ID + UMP/MAX dashboard/device gates. **GO để lập và thực hiện migrate plan**, nhưng chỉ GO release sau khi build debug/release xanh và checklist Step 8/11 có bằng chứng.
