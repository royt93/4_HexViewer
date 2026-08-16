# HexViewer — Product Backlog (Scrum)

> **Cập nhật:** 2026-08-16 | **Nguồn:** đọc toàn bộ source (~16.3k LOC) + 4 review độc lập song song (Claude nội bộ, `codex exec`, `agy`/Gemini, `claude --dangerously-skip-permissions` fresh instance).
> **Cách đọc "Nguồn":** số nguồn đồng ý càng cao = độ tin cậy càng cao. 3-4/4 = gần như chắc chắn đúng, nên ưu tiên verify+fix trước.
> Roadmap 4 feature đã lên plan trước đó (Data Inspector, File Diff, Bookmarks, Pattern Search) vẫn giữ nguyên ở [TASK_OVERVIEW.md](TASK_OVERVIEW.md) — backlog này bổ sung phần **fix bug**, **trả nợ kỹ thuật**, **feature mới chưa có trong plan**, **ý tưởng sản phẩm**, và **tính năng độc quyền**.
>
> **Mỗi item bên dưới đã có task card riêng** trong [`todo/`](todo/) (file `<ID>_<slug>.md`, vd `todo/BUG-001_cancel-save-deletes-file.md`) — bảng ở đây là index tổng quan, mở file card để xem mô tả đầy đủ + acceptance criteria. Khi bắt đầu code 1 task, di chuyển file card từ `todo/` → `inprogress/`; khi merge xong, chuyển tiếp sang `done/`. **Chưa code gì ở giai đoạn này — toàn bộ 62 item hiện đang nằm trong `todo/`.**
>
> **Quyết định của chủ dự án (2026-08-16):** BUG-003 / PI-001 (VIP key bị crack) **tạm chưa xử lý**, ưu tiên các việc khác trước. Task card vẫn giữ nguyên trong backlog để không quên, nhưng không đưa vào sprint hiện tại.

---

## 0. Sprint 0 đề xuất — "Stop the bleeding" (làm TRƯỚC mọi feature mới)

Lý do: 2 bug P0 gây **mất dữ liệu thật của user** (BUG-001, BUG-002). Build feature mới lên trên nền này là xây nhà trên cát.

| ID | Việc | Điểm |
|----|------|------|
| BUG-001 | Fix mất file khi cancel Save (gộp chung NF-002 Safe Save) | 5 |
| BUG-002 | Fix corrupt index khi xoá dòng lúc đang filter/search | 5 |
| BUG-004 | Fix race onException/onCancel trong TaskRunner (dialog treo) | 3 |

**Tổng Sprint 0: 13 điểm**, ước lượng 2-3 ngày 1 dev senior.

> BUG-003 (VIP key bị crack, 8 điểm) đã được lấy ra khỏi Sprint 0 theo quyết định của chủ dự án — xem ghi chú ở đầu file.

---

## 1. BUGS_TO_FIX

| ID | Ưu tiên | Nguồn | Điểm | Vấn đề |
|----|---------|-------|------|--------|
| BUG-001 | P0 | 3/4 | 5 | **Cancel Save = mất file gốc.** `TaskSave.java` ghi đè trực tiếp lên file (truncate in-place), không dùng temp-file + atomic rename. Nếu user bấm Cancel giữa lúc save (hoặc lỗi), code còn chủ động gọi `DocumentFile.delete()` để "dọn dẹp" → xoá luôn file gốc của user. `TaskSave.java:91-100, 150-154`. |
| BUG-002 | P0 | 3/4 | 5 | **Xoá dòng sai khi đang filter.** Vị trí (position) chọn để xoá là index trong danh sách đã lọc, nhưng `LineEntries.removeItem()` lại `remove((Integer) position)` trên danh sách gốc (remove theo *value* không phải *index thực*) → xoá nhầm dòng, hỏng mapping index. `HexMultiChoiceCallback.java:77-82`, `DeleteCommand.java:40-48`, `LineEntries.java:107-110`. |
| BUG-003 | P0 | 4/4 | 8 | **VIP key hardcode, crack 1 lần dùng mãi mãi.** 2 key VIP master chỉ encode Base64 (không mã hoá), check bằng so sánh chuỗi local, không có xác thực server. Decompile APK 1 lần (`jadx`/`apktool`) là lấy được key, share công khai → mọi user cài app đều free VIP vĩnh viễn. `VipKeys.kt:5-6, 24-25`. Trầm trọng hơn vì `proguard-rules.pro` có `-keep class ...ui/** { *; }` và `...util/** { *; }` gần như không obfuscate gì (xem TD-006). |
| BUG-004 | P1 | 4/4 | 3 | **Race cancel/exception trong TaskRunner → dialog progress treo mãi.** `onException()` chạy trên background thread (có thể crash `CalledFromWrongThreadException` khi listener update UI trực tiếp — case cụ thể: `TaskHash.onException` → `ActHashCalculator`). Khi exception xảy ra, `onPostExecute`/`cleanup()` không được gọi → `ProgressTask` đứng hình. Ngoài ra `cancel()` post `onCancelled()` trong khi `finally` của background thread có thể đang post `onPostExecute()` — có thể fire cả 2, hoặc fire nhầm cái. `TaskRunner.java:45-53, 75-97`, `ProgressTask.java:116-127`, `TaskHash.java:159-165`, `ActHashCalculator.java:134-155`. |
| BUG-005 | P1 | 3/4 | 3 | **Undo/Redo vẫn leak Activity dù đã "fix".** `UnDoRedo` dùng `WeakReference<ActMain>` để chống leak, nhưng `UpdateCommand`/`DeleteCommand` bên trong 2 deque `mUndo`/`mRedo` lại giữ **strong reference** tới `ActMain`. Chống leak thực tế phụ thuộc 100% vào `UnDoRedo.cleanup()` được gọi đúng lúc trong `onDestroy()` — nếu lỡ 1 exception path bỏ qua cleanup, leak quay lại. `UpdateCommand.java:24-26`, `DeleteCommand.java:22-24`. |
| BUG-006 | P1 | 3/4 | 3 | **`RandomAccessFileChannel` mode RW leak fd/stream.** `close()` chỉ đóng output stream khi `mMode == Mode.WO`; mode RW không bao giờ đóng `mFileOutputStream`/`FileChannel` tường minh. Hiện đang "ngủ yên" vì `openForReadWrite()` chưa được dùng ở đâu (`@SuppressWarnings("unused")`) — nhưng sẽ nổ ngay khi NF-001 (in-place edit) được implement. `RandomAccessFileChannel.java:174-224`. |
| BUG-007 | P1 | 2/4 | 2 | **Nhập offset dạng số thập phân (vd "1.5" MB) trong Partial Open bị crash/mất input.** `ActPartialOpen.convertValueTo()` tính đúng giá trị qua `convert(val, null)` nhưng sau đó gọi `Long.parseLong(val)` trên chuỗi gốc "1.5" → NumberFormatException bị nuốt, trả về rỗng, input biến mất không báo lỗi. `ActPartialOpen.java:462-477`. |
| BUG-008 | P2 | 3/4 | 2 | **NPE khi mở file từ URI lạ/malformed (intent từ app khác).** `FileHelper.getFileName()` gọi `uri.getScheme().equals("content")` không check null; `getParentUri()` gọi `.length()` trên `getEncodedPath()` có thể null. Trigger được qua `ActMain.processIntentUri` khi app khác share file qua VIEW/EDIT intent với URI bất thường. `FileHelper.java:256-260, 360-364`. |
| BUG-009 | P2 | 2/4 | 3 | **ANR khi chuyển tab Plain Text với file lớn.** `PayloadPlainSwipe.refresh()` post `refreshPlain()` vào Handler chạy trên **main-thread looper**, duyệt tới 50.000 dòng với `List<Byte>` autobox từng byte → giật/đứng UI. Cùng nhóm lỗi mà `TaskOpen`/`TaskSave` đã né (chạy background) nhưng path Plain Text bị bỏ sót. `PayloadPlainSwipe.java:126-140, 149-206`. |
| BUG-010 | P2 | 2/4 | 1 | **VIP key phân biệt hoa/thường trái với doc.** Comment `lookupDays()` ghi "trim + uppercase" nhưng code chỉ `.trim()`, không uppercase — user nhập key khác case bị báo "invalid" sai. `VipKeys.kt:15, 24-25`. |
| BUG-011 | P2 | 1/4 | 2 | **`onTrimMemory(COMPLETE)` clear data nhưng không clear Undo/Redo stack.** Sau khi bị OS trim memory, `mFileData=null` và adapter bị clear, nhưng `mUnDoRedo` không gọi `.clear()` → nếu user undo/redo sau đó, command tham chiếu index/data không còn tồn tại → có thể `IndexOutOfBoundsException` hoặc corrupt state. `ActMain.java:998-1026`. *(1 nguồn — cần verify lại trước khi fix).* |
| BUG-012 | P2 | 1/4 | 2 | **`validateIntent()` chạy I/O đồng bộ trên main thread lúc `onCreate`.** Gọi `FileHelper.getFileSize()` (mở file descriptor đồng bộ) ngay trong luồng xử lý intent khi mở app từ "Open with…" — với provider chậm (Drive, Dropbox…) có thể ANR lúc khởi động. `ActMain.java:319-324`. *(1 nguồn — cần verify).* |
| BUG-013 | P1 | 1/4 | 3 | **Sửa dòng hex >50KB bị mất âm thầm.** `ActLineUpdate.onDestroy()` clear 2 static field `sBridgeResultReferenceString`/`sBridgeResultNewString` — nếu field này chạy trước/trong lúc `ActivityResultLauncher` callback đọc kết quả (case data lớn >50KB dùng static bridge thay vì Intent extras), callback đọc null/rỗng → edit bị mất không báo lỗi. `ActLineUpdate.java:267-271`, `LauncherLineUpdate.java:77-92`. *(1 nguồn nhưng rất cụ thể — ưu tiên verify sớm vì severity cao: mất data âm thầm).* |
| BUG-014 | P2 | 1/4 | 1 | **Off-by-one trong `AdtRecentlyOpenRecycler`.** Check bound dùng `idx > size()` thay vì `>=`; race với swipe-to-delete có thể trúng đúng `idx == size()` → `IndexOutOfBoundsException`. `AdtRecentlyOpenRecycler.java:99-101, ~203-205`. |
| BUG-015 | P2 | 1/4 | 1 | **`getSnapshot()` nuốt `OutOfMemoryError` thành list rỗng khi search.** User không phân biệt được "không tìm thấy" với "search bị fail do thiếu RAM" — dễ gây kết luận sai khi làm forensic. `LineEntries.java:42` (khu vực `getSnapshot`). |

**Tổng BUGS_TO_FIX: 15 items, 46 điểm.**

---

## 2. TECH_DEBT_ENHANCEMENTS

| ID | Nguồn | Điểm | Vấn đề |
|----|-------|------|--------|
| TD-001 | 3/4 | 13 | Migrate `ListView`/`ArrayAdapter` (hex + plain text view) → `RecyclerView` + `DiffUtil`. Là điều kiện tiên quyết để làm mượt highlight/diff cho T-002, T-004. `AdtHexTextArray.java`, `AdtSearchableListArray.java`, `AdtPlainTextListArray.java`. |
| TD-002 | 4/4 | 5 | `TaskSave` box toàn bộ file thành `List<Byte>` trước khi ghi (`bytes.addAll(entry.getRaw())` per byte) → hàng triệu object boxed với file lớn, không có `MemoryMonitor` guard nào (khác với `TaskOpen`). Ghi thẳng `byte[]`/NIO `FileChannel` theo chunk. `TaskSave.java:151-154`. |
| TD-003 | 3/4 | 13 | `ActMain.java` (~1030 dòng) là "God Activity": file I/O, undo/redo, search, popup, ad lifecycle, VIP badge đều nằm chung 1 class. Tách `EditorViewModel` (MVVM) — lợi ích phụ: giữ được buffer/undo history qua config change thay vì load lại từ disk. |
| TD-004 | 2/4 | 8 | `TaskRunner` là AsyncTask-tự-chế, quản lý lifecycle thủ công dễ vỡ (chính là root cause BUG-004, BUG-005). Migrate sang Kotlin Coroutines + `lifecycleScope`/structured concurrency. |
| TD-005 | 3/4 | 21 | **Model whole-file-in-memory là trần kiến trúc.** Đây là root cause của hàng loạt OOM fix chắp vá đã ghi trong `doc/MEMORY_LEAK_FIX_REPORT.md`/`PERFORMANCE_OPTIMIZATIONS.md`. T-002 (File Diff) tự doc của nó đã ghi cần "partial-diff mode cho file >10MB" — nếu không paging hoá model trước, T-002 sẽ đâm thẳng vào trần này. **Khuyến nghị: làm TD-005 (ít nhất bản rút gọn) trước khi bắt đầu T-002.** |
| TD-006 | 1/4 | 3 | `proguard-rules.pro:145-150` có `-keep class ...ui/** { *; }` và `...util/** { *; }` — giữ nguyên gần như toàn bộ code, vô hiệu hoá obfuscate/shrink của R8. Đây cũng là lý do BUG-003 (VIP key) dễ bị decompile. Thu hẹp lại còn đúng class cần reflection. |
| TD-007 | 1/4 | 5 | VIP state nằm rải rác 3 nơi (flag/expiry nội bộ AdManager, `VipPrefs` SharedPreferences, cờ first-init trong `AppPreferences`) phải tự đồng bộ tay — lệch 1 chỗ là hiện sai trạng thái VIP cho user. Gộp về 1 nguồn sự thật. |
| TD-008 | 2/4 | 5 | Search theo từng keystroke không debounce, clone toàn bộ list mỗi ký tự gõ (`EntryFilter.apply()`, `SearchableFilterFactory.multilineSearch`) — chạy background nên không treo UI nhưng tốn GC, và cấu trúc dữ liệu hiện tại không mở rộng tốt cho wildcard/regex (cần cho T-004). |
| TD-009 | 1/4 | 2 | Pattern "confirm unsaved changes → save → tiếp tục" bị copy-paste 4 lần trong `ActMain.java` (dòng ~373, ~669, ~866, ~967) — gom về 1 helper `runWithSaveConfirmation()`. |
| TD-010 | 3/4 | 2 | `Log.d("roy93~", ...)` tràn lan trong hot loop của `TaskOpen` (per-line, mỗi 50 vòng lặp...), một số log cả full file path/URI (nhạy cảm privacy cho 1 app chuyên đọc file bất kỳ của user). Proguard có strip ở release build, nhưng debug/profiling build vẫn trả giá đầy đủ. |
| TD-011 | 1/4 | 3 | Chưa có CI pipeline chạy `./gradlew test` dù test coverage khá ổn (23 unit test file, 9 instrumented, theo `doc/test/FULL_TEST_PLAN.md`). Thêm GitHub Actions gate trước khi merge — nên làm trước khi 4 feature lớn (T-001..T-004) đổ vào. |
| TD-012 | 1/4 | 1 | LeakCanary bị comment-out trong `build.gradle` kể cả ở dev/debug build — trái với mô tả trong `BUILD_VARIANTS_GUIDE.md`, mất luôn công cụ phát hiện chính xác class bug (leak) mà app này đã fix tới 24 lần trong lịch sử. |
| TD-013 | 1/4 | 3 | Serialize state (`RecentlyOpened`, `FileData`) bằng string ghép delimiter `"|"`/`"^"` — dễ vỡ khi thêm field mới. Chuyển sang JSON có version hoặc Room table nhỏ. |
| TD-014 | 1/4 | 2 | `android:launchMode="singleTask"` bị áp cho gần như mọi Activity (không chỉ launcher) — nguồn tiềm ẩn bug back-stack, vì `singleTask` chỉ nên dùng cho task root. |

**Tổng TECH_DEBT: 14 items, 86 điểm.**

---

## 3. NEW_FEATURES (chưa có trong 4 task đã plan)

Xếp theo giá trị user, có ghi phụ thuộc nếu có.

| ID | Nguồn | Điểm | Feature | Phụ thuộc |
|----|-------|------|---------|-----------|
| NF-002 | — | 5 | **Safe Save**: ghi ra temp file rồi atomic-rename thay vì truncate in-place. | Fix trực tiếp BUG-001, nên làm **cùng lúc** với BUG-001, không tách riêng. |
| NF-005 | 3/4 | 5 | **Export/Share vùng byte đã chọn**: raw binary, hex string, C/Kotlin array, Base64. Hiện hoàn toàn chưa có workflow export selection nào. | — |
| NF-003 | 2/4 | 8 | **Find & Replace hàng loạt** (hex/text) — hiện chỉ có Find, chưa có Replace. Ghép cặp tự nhiên với T-004 Pattern Search. | Nên làm sau/cùng T-004 |
| NF-008 | 2/4 | 5 | **Chọn encoding cho Plain Text pane** (UTF-8/16 LE/BE, Shift-JIS, Windows-1252, auto-detect) — hiện hardcode chỉ ASCII in được, còn lại hiện dấu chấm. | — |
| NF-012 | 2/4 | 3 | **Hash theo vùng chọn**, không chỉ whole-file như hiện tại (`ActHashCalculator`/`TaskHash`). | — |
| NF-013 | 2/4 | 5 | **Chế độ Read-only/View lock** cho file mở qua intent VIEW từ app khác — tránh sửa nhầm file hệ thống/nhạy cảm. | — |
| NF-001 | 2/4 | 13 | **In-place random-access edit thật sự** cho file lớn (dùng lại `RandomAccessFileChannel` RW mode đang có sẵn nhưng chưa wire) — bỏ trần kích thước file hiện tại. | Phải fix BUG-006 trước; liên quan TD-005 |
| NF-009 | 2/4 | 13 | **Multi-tab / mở nhiều file cùng lúc** (2-5 file). | Nặng vì đụng kiến trúc ActMain — nên làm sau TD-003 |
| NF-011 | 2/4 | 8 | **Nhận diện file có cấu trúc** (magic byte: ZIP/APK/ELF/DEX/PNG/JPEG/PDF/SQLite) kèm annotate header/section cơ bản. | Nền tảng chung với T-001 Data Inspector — nên gộp chung epic |
| NF-004 | 2/4 | 13 | **Binary template parser kiểu 010 Editor** (user tự định nghĩa struct để auto-annotate). | Xây trên NF-011 + T-001 |
| NF-006 | 1/4 | 5 | **Bàn phím hex/ASCII riêng khi edit** (phím tắt 0-9 A-F, macro chèn nhanh 0x00/0xFF...) — đỡ phải chuyển qua lại bàn phím QWERTY hệ thống. | — |
| NF-007 | 1/4 | 5 | **Công cụ toán bit trên vùng chọn** (XOR/AND/OR/NOT/shift/đảo endian) — hữu ích cho phân tích file bị obfuscate. | — |
| NF-010 | 1/4 | 8 | **Session restore / khôi phục sau crash**: lưu lại URI, vị trí xem, selection, edit đang dang dở để phục hồi sau khi app bị kill. | — |
| NF-014 | 1/4 | 3 | **Phát hiện file bị thay đổi từ bên ngoài** (so mtime/size) trước khi ghi đè — tránh mất thay đổi của app khác. | — |
| NF-015 | 1/4 | 3 | **File Info/Hash Calculator dùng luôn file đang mở** thay vì bắt chọn lại file từ picker hệ thống. | — |

**Tổng NEW_FEATURES: 15 items, 101 điểm.**

---

## 4. PRODUCT_IDEAS (tăng trưởng / doanh thu / giữ chân user)

| ID | Nguồn | Ý tưởng |
|----|-------|---------|
| PI-001 | **4/4 — đồng thuận tuyệt đối** | **Thay hệ thống VIP key hardcode bằng Google Play Billing (subscription/lifetime) hoặc entitlement xác thực server-side.** Đây không chỉ là fix bug (BUG-003) mà là fix lại toàn bộ mô hình kinh doanh — hiện tại `build.gradle` **chưa hề có billing library nào**. |
| PI-002 | 2/4 | VIP hiện chỉ tắt quảng cáo — cho VIP giá trị chức năng thật (mở khoá đúng những gì T-001..T-004 đã plan là VIP-gated) để người trả tiền thấy xứng đáng. |
| PI-003 | 2/4 | Redesign "xem ad đổi VIP": thay vì tặng nguyên 3 ngày VIP cho 1 lần xem ad (hiện tại), đổi thành "phiên quyền lợi" ngắn/có mục tiêu hơn (vd 2 giờ, hoặc 1 lần export) — tăng tần suất xem ad mà không làm mất giá trị gói trả phí. |
| PI-004 | 2/4 | Xuất "báo cáo file" chia sẻ được (PDF/text: hash, file info, vùng đáng chú ý) — hữu ích cho dân forensic/support ticket, kèm góc quảng bá tự nhiên ("Created with HexViewer"). |
| PI-005 | 1/4 | File mẫu sẵn có lúc mở app lần đầu (ELF, JPEG lỗi có EXIF cứu được, PNG giấu payload) — giải quyết vấn đề "màn hình trống" lúc mới cài, tăng retention D1. |
| PI-006 | 1/4 | Kho template binary cộng đồng (kiểu 010 Editor, sync cloud) — vừa tăng trưởng viral (GitHub/Reddit), vừa làm funnel bán VIP (template premium). |
| PI-007 | 1/4 | Định vị marketing "riêng tư tuyệt đối": phân tích hoàn toàn offline, không upload file, chỉ dùng SAF — điểm khác biệt cho nhóm dev/incident-responder. |
| PI-008 | 1/4 | Audit lại rủi ro khi toàn bộ doanh thu quảng cáo phụ thuộc 1 thư viện cá nhân bên thứ 3 (`com.github.royt93:AdmobApplovinWrapper`) thay vì tích hợp thẳng AdMob/AppLovin MAX SDK. |
| PI-009 | 1/4 | Lớp tài khoản/cloud để đồng bộ đa thiết bị (bookmarks, phiên diff, VIP entitlement) — bet lớn hơn, hợp lý làm chung lúc với PI-001 (đằng nào cũng cần server-side rồi). |

---

## 5. EXCLUSIVE_KILLER_FEATURES (độc quyền, khó copy trên desktop)

| ID | Nguồn | Ý tưởng |
|----|-------|---------|
| KF-001 | **3/4 — hội tụ mạnh nhất trong toàn bộ report** | **Android-native artifact inspector**: đọc hiểu APK/AndroidManifest binary XML, DEX header/method table, ELF `.so` symbol table, SQLite/WAL, logcat/tombstone — kèm live-patch (toggle debuggable, đổi version code) + re-sign/re-align ngay trên máy. Không desktop hex editor nào làm việc này native. |
| KF-003 | **3/4 — hội tụ mạnh** | **Thu byte trực tiếp từ Camera/NFC/BLE/USB-OTG**: scan QR/barcode/chụp ảnh hex dump, chạm thẻ NFC, kết nối thiết bị BLE hoặc USB-serial/flash programmer → đổ thẳng payload vào hex view. Năng lực này desktop không thể làm nếu thiếu phần cứng rời. |
| KF-002 | 2/4 | **Share-sheet-to-hex**: đăng ký HexViewer làm Share target hệ thống — mọi app khác (mail, browser, file manager) có thể "Open in HexViewer" trực tiếp, không cần mở file manager trước. |
| KF-004 | 2/4 | **Trợ lý AI on-device "file này là gì?"**: entropy map, nhận diện magic byte, tách embedded object, đoán scheme nén/mã hoá (zlib/AES/RC4), tóm tắt ngôn ngữ tự nhiên — chạy hoàn toàn offline (TFLite/Gemini Nano). |
| KF-005 | 1/4 | **UX cảm ứng-first**: minimap/heatmap entropy, pinch để đổi bytes-per-row, kéo-chọn bằng handle, rung phản hồi tại ranh giới cấu trúc, menu radial 1 tay — thiết kế cho ngón tay thay vì thu nhỏ lưới desktop. |
| KF-006 | 1/4 | **Live process memory inspector** qua Root/Shizuku (`/proc/pid/maps`, `/proc/pid/mem`) — nghiên cứu bảo mật ngay trên máy, không cần PC + GDB/Frida. |
| KF-007 | 1/4 | **Wi-Fi hex server on-device**: bật HTTP/WebSocket server local để mở giao diện hex-editor desktop-class từ browser laptop, đọc/ghi trực tiếp file trên điện thoại. |
| KF-008 | 1/4 | **Diff cộng tác qua QR** (dựa trên T-002): 2 người mỗi người mở file trên máy mình, trao đổi signature nén qua QR → biết ngay "khác nhau tại offset X" mà không cần transfer file. |
| KF-009 | 1/4 | **Đọc byte bằng giọng nói / TalkBack**: đọc to vùng dữ liệu dạng decimal/ASCII, lệnh voice "nhảy tới offset 0x400" — mảng accessibility gần như trống trên mọi hex editor desktop, chi phí thấp để chiếm lĩnh. |

---

## 6. Tổng hợp điểm & đề xuất roadmap

| Nhóm | Số item | Tổng điểm |
|------|---------|-----------|
| Bugs | 15 | 46 |
| Tech Debt | 14 | 86 |
| New Features | 15 | 101 |
| Product Ideas | 9 | (định hướng, không chấm điểm) |
| Killer Features | 9 | (định hướng, không chấm điểm) |

### Thứ tự đề xuất (theo giả định 1 dev, sprint 2 tuần ~20-25 điểm/sprint)

```
Sprint 0 (P0 bugs)  → Sprint 1 (P1 bugs + TD-002)  → T-003 Bookmarks (đã plan)
  → T-001 Data Inspector (đã plan, gộp NF-011)  → TD-001 RecyclerView migration
  → T-004 Pattern Search (đã plan)  → TD-005 paging model
  → T-002 File Diff (đã plan, cần TD-005 trước)  → NF-005 Export selection
  → PI-001 VIP → Play Billing  → KF-001 Android artifact inspector (differentiator lớn nhất)
```

**Lý do:** không sửa BUG-001/002/003 trước thì mọi feature mới build trên nền dữ liệu mất mát + VIP vô giá trị. TD-005 (paging) phải đi trước T-002 vì T-002 tự thừa nhận cần xử lý file >10MB. KF-001 đặt cuối vì lớn nhất nhưng cũng là điểm khác biệt rõ nhất để differentiate khỏi mọi hex editor khác trên Play Store.
