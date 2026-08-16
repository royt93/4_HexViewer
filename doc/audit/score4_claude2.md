# Audit vòng 4 (trước push) — claude2 — 2026-08-16

## Build status (bao gồm kết quả 2-3 lần chạy testDevDebugUnitTest liên tiếp)

- `assembleDevDebug` / `assembleProductionRelease`: PASS (UP-TO-DATE, đã build sẵn từ trước).
- `compileDevDebugAndroidTestSources`: PASS.
- `testDevDebugUnitTest`, chạy `--rerun` **4 lần liên tiếp** (mặc định `maxParallelForks` = số core máy = 10,
  kết hợp `forkEvery = 1`):
  1. **FAIL** — `Could not write XML test results for ... (20 test class)` — `KryoException: Buffer
     underflow` / `EOFException` khi Gradle ghi kết quả XML. KHÔNG phải test logic fail — không có
     assertion nào fail, lỗi nằm ở tầng Gradle tự serialize/deserialize kết quả binary.
  2. PASS.
  3. **FAIL** — cùng lỗi y hệt, class khác bị nêu tên (`FileDataTest`) nhưng cùng root cause.
  4. Chạy lại với `--max-workers=1` (giảm số worker thread của chính Gradle, không đổi code): PASS.
  5. Chạy lại lần nữa với `--max-workers=1`: PASS.

  → **Tỷ lệ fail mặc định: 2/3 (66%)**. Root cause xác định được: `forkEvery = 1` (audit vòng 3, fix C1)
  buộc Gradle liên tục fork/kill JVM test mới cho MỖI class; kết hợp `maxParallelForks` không được set
  (mặc định = số core CPU, ở máy audit này = 10) khiến nhiều JVM ghi đồng thời vào cùng 1 file kết quả
  nhị phân nội bộ của Gradle (`TestOutputStore`) → race → corrupt → Gradle không đọc lại được để xuất
  XML. Khi ép Gradle chạy tuần tự (`--max-workers=1`, tương đương giảm số fork đồng thời) thì 2/2 lần
  đều pass sạch.

  **Kết luận: full-suite test VẪN FLAKY sau vòng 3 — chỉ đổi loại flaky, không hết.** Vòng 3 sửa đúng
  bug logic (state leak `vipActivationBackoff` giữa test class), nhưng cách sửa (`forkEvery = 1`) tạo
  ra fork-churn cao, phơi ra 1 race condition khác trong chính Gradle 8.7 khi nhiều JVM con ghi kết quả
  song song. Đây là lỗi ở tầng build infra, không liên quan gì tới code app — nhưng vẫn khiến CI hoặc
  máy dev nhiều core (CI runner thường 4-16 core) có xác suất fail ngẫu nhiên không nhỏ mỗi lần chạy
  full suite, y hệt triệu chứng ban đầu mà C1 định giải quyết.

  **Đề xuất patch** (không tự apply): thêm `maxParallelForks` cố định (vd `1` hoặc `2`) cùng chỗ với
  `forkEvery = 1` trong `app/src/main/../app/build.gradle` (`testOptions.unitTests.all`):
  ```groovy
  all {
      it.forkEvery = 1
      it.maxParallelForks = 1   // hoặc số nhỏ cố định, KHÔNG để mặc định theo core CPU máy chạy
  }
  ```
  Đánh đổi: test chạy chậm hơn (mất song song), nhưng đổi lại là quyết định đúng — CI xanh giả (do
  race) còn nguy hiểm hơn CI chậm.

## Breakdown điểm

| Tiêu chí | Điểm /10 | Lý do ngắn |
|---|---|---|
| Correctness | 8.5 | Logic app (VIP activation, redeem code, grantVipDays, bindUi single-source-of-truth) đối chiếu đúng với API SDK thật tag 1.6.16 (đọc trực tiếp `AdManager.kt` qua `gh api`) — `activateVipByKey(ctx, key, 0)`, `grantVipDays`, `getVipGrantedAtMs` đều dùng đúng contract. Trừ điểm vì `forkEvery=1` (fix C1 vòng 3) chưa giải quyết triệt để flaky — đổi sang 1 loại flaky khác (xem trên), tự thân là 1 correctness bug ở build script chưa qua audit hết. |
| Security | 2.0 | **CRITICAL, không liên quan trực tiếp tới diff hiện tại nhưng ảnh hưởng quyết định push**: repo `github.com/royt93/4_HexViewer` đang **PUBLIC**, và keystore ký release thật (`app/key/keystore.jks`, alias `mckimquyen`, cert 2024-2049, xác nhận bằng `keytool -list`) CÙNG mật khẩu plaintext của nó (`keystore.properties`: `storePassword=27072000`, khớp với hardcoded fallback `"27072000"` trong `app/build.gradle`) đã **push lên `origin/dev` từ commit `5f0c7af`** — đang lộ công khai ngay lúc này, ai cũng clone lấy được khoá ký app Play Store thật. Đây không phải rủi ro "nếu push" mà là sự cố ĐANG XẢY RA. Riêng phần diff/secret mới trong vòng này (VIP_KEY_SECRET đọc từ `local.properties`, không hardcode) làm đúng, cộng thêm điểm nhưng không đủ bù. |
| Policy compliance (F12) | 9.0 | Đọc kỹ `ActVipManagement.kt`: nhánh Interstitial fallback (`showInterstitial { shown -> if (shown) showNoRewardDialog() else showNoAdDialog() }`) xác nhận KHÔNG gọi `grantVipFromAd()` dù `shown=true` — đúng yêu cầu chính sách Rewarded. `grantVipFromAd()` chỉ được gọi từ nhánh `earned=true` của `showRewarded`. Đối chiếu `AdManager.showRewarded` (SDK thật) — callback `onRewardEarned` phản ánh đúng reward thật (không phải chỉ "đã show"). Trừ nhẹ vì thiếu test tự động cho đúng path này (xem Test coverage). |
| Code quality | 8.0 | Refactor `VipKeys`/`VipPrefs` sạch, xoá đúng dead state (`grantedAtMs` tự lưu trùng SDK), comment giải thích rõ audit trail (F13/F14/F18/F22). `settings.gradle` gate `mavenLocal()` đúng điều kiện. Vẫn còn nợ: hardcoded fallback password `"27072000"` trong `signingConfigs.release` (pre-existing, không thuộc diff này nhưng đọc thấy khi audit — nên dọn luôn nhân tiện vì cùng chủ đề bảo mật). |
| Test coverage | 6.5 | `ActVipManagementTest`/`AdIntegrationTest` cover activate-by-key, redeem code, `grantVipDays` path, revoke. **Thiếu**: không có test nào bấm `btnWatchAd`, giả lập `earned=false` + interstitial `shown=true`, rồi assert VIP KHÔNG được cấp (`showNoRewardDialog` hiện, không phải `showActivationSuccess`) — tức là chính path F12 (path policy-critical nhất) không có regression test tự động, dù logic đã tự đọc-verify đúng bằng tay. |

## ĐIỂM TỔNG: 5.5/10

(Trung bình có trọng số nghiêng về Security do đây là audit cuối trước push — 1 CRITICAL blocker đủ để
kéo điểm tổng xuống dưới ngưỡng an toàn, dù 4/5 tiêu chí còn lại đều khá tốt.)

## Finding còn sót (nếu có)

1. **[CRITICAL — bảo mật, KHÔNG thuộc diff hiện tại nhưng phải xử lý trước/song song push]**
   `app/key/keystore.jks` (khoá ký release thật, alias `mckimquyen`) và `keystore.properties`
   (password plaintext `27072000`, khớp fallback hardcode trong `app/build.gradle` dòng 116-118) đang
   được track trong git và đã có mặt trên `origin/dev` (repo **public**). Bất kỳ ai cũng tải về được
   khoá ký ứng dụng thật trên Play Store. Đề xuất xử lý (ngoài phạm vi code diff, cần quyết định của
   chủ repo):
   - Coi khoá này là ĐÃ LỘ (compromised) — không thể chỉ xoá khỏi git history mà an toàn lại, vì đã
     public. Nếu app đã publish lên Play Store bằng khoá này, cân nhắc Play App Signing (Google giữ
     khoá ký thật, khoá upload có thể thay) nếu chưa dùng.
   - Xoá `app/key/keystore.jks`, `keystore.properties` khỏi working tree, thêm vào `.gitignore`.
   - Rotate khoá (nếu dùng Play App Signing) hoặc ít nhất đổi mọi mật khẩu liên quan.
   - Xoá khỏi git history (`git filter-repo`/BFG) — biết rằng dữ liệu đã có thể bị cache/fork/index bởi
     bên thứ 3, xoá history chỉ ngăn rò thêm chứ không thu hồi bản đã lộ.
   - Xoá luôn hardcoded fallback `"27072000"` trong `signingConfigs.release` — comment ngay phía trên
     nó ("Security: Use environment variables instead of hardcoded passwords") đã tự mâu thuẫn với chính
     dòng code bên dưới.

2. **[HIGH — build infra]** `forkEvery = 1` (fix C1, vòng 3) không kèm `maxParallelForks` cố định →
   full-suite `testDevDebugUnitTest` flaky lại theo kiểu khác (2/3 lần fail trong audit này). Xem chi
   tiết + patch đề xuất ở mục Build status.

3. **[MEDIUM — test coverage]** Thiếu test tự động cho path F12 (Interstitial fallback KHÔNG cấp VIP).
   Đề xuất thêm 1 test trong `ActVipManagementTest` hoặc `AdIntegrationTest`: simulate rewarded
   `earned=false`, interstitial callback `shown=true`, assert `AdManager.isVipByKeyActive()` vẫn false
   sau đó (cần test hook cho `showRewarded`/`showInterstitial` — kiểm tra `AdManagerTestHooks.kt` của
   SDK có sẵn seam phù hợp không).

4. **[LOW]** `doc/AD.MD` không hề nhắc tới finding #1 (keystore/password lộ) dù đã 3 vòng audit trước —
   có thể do phạm vi 3 vòng trước chỉ nhìn vào diff (đúng phạm vi được giao), không chủ động rà toàn bộ
   repo. Từ vòng này nên coi rà bảo mật ở mức "toàn repo tracked files", không chỉ diff, mỗi khi audit
   trước-push.

## Live device test (nếu có làm)

Không thực hiện — ưu tiên thời gian cho build/test verification lặp lại (yêu cầu bắt buộc của vòng 4)
và audit bảo mật; live device test không cần thiết để phát hiện 2 finding chính (build infra flaky +
keystore lộ), cả hai đều xác nhận được qua CLI/git/keytool.

## Kết luận

**KHÔNG PUSH thêm gì lên `origin` cho tới khi xử lý finding #1** — khoá ký release thật + mật khẩu của
nó đang công khai trên GitHub public ngay lúc này (đã lộ từ trước, không phải rủi ro của riêng đợt push
này, nhưng "trước khi push" là đúng thời điểm bắt buộc phải dừng lại xử lý, không thể tặc lưỡi cho qua).
Về phần diff đang xét (VIP activation flow, F12 compliance, settings.gradle gate, VipKeys/VipPrefs
refactor): chất lượng tốt, đúng đắn, khớp API SDK thật — nếu tách riêng phần này thì đáng ~8/10 và có
thể push. Nhưng câu hỏi "PUSH hay KHÔNG PUSH" phải xét toàn bộ trạng thái repo sắp đẩy lên, và với
secret production đang lộ công khai cộng thêm test suite chưa thật sự hết flaky (2/3 fail khi chạy đúng
như lệnh audit yêu cầu), câu trả lời trung thực là: xử lý xong 2 finding CRITICAL/HIGH ở trên rồi mới
push, đừng chấm điểm cao vì lịch sự khi rủi ro thật đang treo lơ lửng.
