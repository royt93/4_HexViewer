# T-002 — File Diff / Compare 2 File Binary

**Status:** `todo`
**Effort:** L — 7–10 ngày
**Priority:** P1
**VIP Gate:** Full VIP (toàn bộ feature chỉ dành cho VIP)

---

## Mô tả

Cho phép user mở 2 file binary cùng lúc và xem các byte khác nhau được highlight theo màu. Đây là **killer feature** phân biệt HexViewer với 90% các hex viewer khác trên Play Store.

**Use case chính:**
- Developer so sánh 2 version của binary (firmware update, APK patch)
- Security researcher tìm thay đổi giữa 2 version malware
- Gamer compare save file trước/sau khi chỉnh sửa
- Kiểm tra kết quả encrypt/decrypt

---

## UI/UX

```
┌──────────────────────────────────────────────────┐
│  File Diff          [File A] [File B]   [×] [⋮]  │
│  Diff: 3 bytes khác nhau                         │
├────────────────────┬─────────────────────────────┤
│  FILE A (v1.bin)   │  FILE B (v2.bin)             │
│  0000: 48 65 6C 6C │  0000: 48 65 6C 6C           │  ← Giống (không highlight)
│  0010: 6F 20 57 6F │  0010: 6F 20 57 6F           │
│  0020:[FF]41 42 43 │  0020:[00]41 42 43           │  ← Khác (FF vs 00, đỏ)
│  0030: 44 45 46 47 │  0030: 44 45 46 [AA]         │  ← Khác (47 vs AA, đỏ)
├────────────────────┴─────────────────────────────┤
│  [◀ Prev diff]  [▶ Next diff]   1/3              │
└──────────────────────────────────────────────────┘
```

**Màu sắc:**
- 🟢 Xanh lá: byte chỉ có ở file này (insert)
- 🔴 Đỏ: byte khác nhau (modify)
- ⚪ Bình thường: byte giống nhau
- 🟡 Vàng: currently focused diff

---

## Ưu điểm

- ✅ **Unique differentiator** — ít app trên Play Store có tính năng này
- ✅ **Natural VIP upsell** — user cần feature quan trọng → sẵn sàng trả
- ✅ **High App Store rating** — "Compare files" được nhiều user request
- ✅ **Viral potential** — developer/security researcher chia sẻ

## Nhược điểm

- ⚠️ **Effort cao nhất** (7–10 ngày) — cần diff algorithm, split layout
- ⚠️ **Memory intensive** — 2 file cùng load, cần streaming approach cho file lớn
- ⚠️ **UX phức tạp** — scroll sync giữa 2 panels, handle file size khác nhau
- ⚠️ **Edge cases nhiều** — file kích thước khác nhau, encoding, binary vs text

---

## Plan chi tiết

### Phase 1: Diff Algorithm Engine (ngày 1–2)

**File mới:** `app/src/main/java/com/galaxyjoy/hexviewer/feature/diff/BinaryDiff.java`

```java
public class BinaryDiff {
    public enum DiffType { SAME, MODIFIED, INSERTED, DELETED }

    public static class DiffRegion {
        public long offsetA;   // offset trong file A
        public long offsetB;   // offset trong file B
        public int length;     // số byte trong region
        public DiffType type;
    }

    // So sánh 2 byte arrays, trả về danh sách regions
    // Dùng simple byte-by-byte diff (đủ cho binary, không cần LCS)
    public static List<DiffRegion> diff(byte[] a, byte[] b) { ... }

    // Streaming diff cho file lớn (đọc từng chunk)
    public static List<DiffRegion> diffStreaming(InputStream a,
                                                  InputStream b,
                                                  int chunkSize) { ... }
}
```

**File test:** `BinaryDiffTest.java`
- Identical files → 0 diff regions
- 1 byte changed at offset 10 → 1 MODIFIED region
- A longer than B → tail is DELETED
- Mixed changes

### Phase 2: Activity + Layout (ngày 3–4)

**File mới:** `ActFileDiff.java`
- Extends `BaseActivity`
- 2 `ListView` side by side (horizontal `LinearLayout` với `weightSum=2`)
- Sync scroll: khi scroll list A → list B scroll cùng row
- Toolbar: `[File A] [File B]` buttons, diff count badge
- Bottom bar: `[◀ Prev] [diff X/N] [▶ Next]`

**File mới:** `app/src/main/res/layout/act_file_diff.xml`
```xml
<ConstraintLayout>
    <Toolbar/>
    <LinearLayout  <!-- horizontal, 2 halves -->
        <ListView android:id="@+id/lvFileA" android:layout_weight="1"/>
        <View android:layout_width="1dp" ... />  <!-- divider -->
        <ListView android:id="@+id/lvFileB" android:layout_weight="1"/>
    </LinearLayout>
    <LinearLayout  <!-- bottom nav bar -->
        <Button id="btnPrevDiff"/>
        <TextView id="tvDiffCounter"/>
        <Button id="btnNextDiff"/>
    </LinearLayout>
</ConstraintLayout>
```

**File mới:** `AdtDiffArray.java` (extends `AdtHexTextArray`)
- Override `getView()` để apply màu highlight theo `DiffRegion`
- Cache highlight spans để tránh recalculate mỗi scroll

### Phase 3: Scroll Sync (ngày 5)

```java
// Sync scroll logic
lvFileA.setOnScrollListener(new AbsListView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        if (!isSyncing) {
            isSyncing = true;
            lvFileB.setSelectionFromTop(firstVisibleItem,
                view.getChildAt(0) == null ? 0
                    : view.getChildAt(0).getTop());
            isSyncing = false;
        }
    }
});
```

### Phase 4: VIP Gate + Launcher (ngày 6)

- Thêm button "So sánh File" ở `ActMain` hoặc menu overflow
- Check VIP → nếu FREE: show dialog "Tính năng VIP" → mở `ActVipManagement`
- `LauncherFileDiff.java` — tương tự `LauncherOpen`, handle 2 file picker sequentially

### Phase 5: Large File Support (ngày 7–8)

- Giới hạn diff full: ≤ 10MB mỗi file (FREE: ≤ 2MB)
- File lớn hơn: Partial Diff mode — diff từng chunk 1MB, lazy load
- Progress dialog khi đang compute diff
- Memory: không load cả 2 file vào RAM cùng lúc nếu lớn

### Phase 6: Export Diff Report (ngày 9)

- Menu → "Export Diff" → tạo text report:
  ```
  File A: v1.bin (1024 bytes)
  File B: v2.bin (1024 bytes)
  Diff: 3 bytes khác nhau

  Offset 0x0020: A=FF, B=00
  Offset 0x0031: A=47, B=AA
  Offset 0x0055: A=12, B=34
  ```
- Share via Intent.ACTION_SEND
- [VIP] Export as CSV/JSON

### Phase 7: Test + Polish (ngày 10)

- Unit test: `BinaryDiffTest`
- Instrumented test: launch ActFileDiff, verify highlight
- Performance test: diff 10MB file < 3s
- Edge cases: file trống, 1 file lớn hơn nhiều

---

## Files cần tạo mới

```
feature/diff/
    BinaryDiff.java             ← Pure diff algorithm
    ActFileDiff.java            ← Main diff activity
    AdtDiffArray.java           ← ListView adapter với highlight
    LauncherFileDiff.java       ← File picker launcher

res/layout/
    act_file_diff.xml
    item_diff_row.xml           ← 2 columns: fileA_hex | fileB_hex

test/.../feature/diff/
    BinaryDiffTest.java
```

## Files cần sửa

```
ActMain.java            ← Thêm button/menu "So sánh File" (VIP only)
AndroidManifest.xml     ← Đăng ký ActFileDiff
```

---

## Definition of Done

- [ ] Chọn 2 file → hiển thị side-by-side diff view
- [ ] Bytes khác nhau được highlight màu đỏ
- [ ] Scroll sync 2 panel
- [ ] Prev/Next diff navigation
- [ ] Diff count badge trên toolbar
- [ ] VIP gate: FREE user thấy blur/lock, tap → ActVipManagement
- [ ] File > 10MB: hiển thị warning, suggest Partial Diff
- [ ] Performance: diff 2×5MB < 2s trên S24 Ultra
- [ ] Export diff report (text)
- [ ] Unit test + Instrumented test pass
