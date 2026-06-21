# T-003 — Bookmarks & Jump Navigation

**Status:** `todo`
**Effort:** S — 2–3 ngày
**Priority:** P2
**VIP Gate:** Free ≤ 5 bookmarks / VIP unlimited + colors + export

---

## Mô tả

Cho phép user **đánh dấu (bookmark)** các offset quan trọng trong file đang xem, đặt tên/màu cho bookmark, và **nhảy nhanh** giữa các bookmark. Bookmark được lưu theo tên file vào SharedPreferences.

**Use case chính:**
- Đang phân tích file lớn, tìm được section quan trọng → pin lại để quay lại sau
- Đang so sánh nhiều vị trí trong cùng 1 file — nhảy qua lại không cần nhớ offset
- Làm việc lâu với 1 file — session bookmark tồn tại qua lần mở tiếp theo

---

## UI/UX

### Main screen — bookmark indicator trong hex row

```
Offset   Hex Bytes                    ASCII
🔖 0010: FF D8 FF E0 00 10 4A 46...  ÿØÿà..JF
   0020: 49 46 00 01 01 00 00 01...  IF......
🔖 0050: E0 00 10 4A 46 49 46 00...  à..JFIF.
```

### Bookmark drawer (pull từ right edge hoặc menu)

```
┌─────────────────────────────────┐
│  Bookmarks (2/5)        [+ Add] │
├─────────────────────────────────┤
│ 🔴  JPEG Header    0x0010  [✏] [🗑] │
│ 🔵  APP0 Segment   0x0050  [✏] [🗑] │
├─────────────────────────────────┤
│ [VIP] Thêm tối đa 999 bookmarks │
│       + Export bookmark list     │
└─────────────────────────────────┘
```

### Add bookmark dialog

```
┌─────────────────────────────┐
│  Add Bookmark               │
│  Offset: 0x0010 (16)        │
│  Tên: [JPEG Header       ]  │
│  Màu: ● ● ● ● ●            │
│              [Cancel] [Add] │
└─────────────────────────────┘
```

---

## Ưu điểm

- ✅ **Quick win** — ít code nhất (2–3 ngày), ship sớm nhất
- ✅ **Retention** — user quay lại app để dùng bookmarks đã lưu
- ✅ **Natural VIP limit** — giới hạn 5 bookmark FREE rất tự nhiên
- ✅ **Không ảnh hưởng main flow** — hoàn toàn additive
- ✅ **Persistent** — lưu qua các session

## Nhược điểm

- ⚠️ Ít "wow factor" khi marketing, khó screenshot để promote
- ⚠️ Bookmark bị orphan nếu file thay đổi (offset dịch chuyển sau edit)
- ⚠️ Key SharedPreferences phải escape đúng tên file (có thể có ký tự đặc biệt)

---

## Plan chi tiết

### Phase 1: Data Model + Storage (ngày 1)

**File mới:** `app/src/main/java/com/galaxyjoy/hexviewer/feature/bookmark/Bookmark.java`

```java
public class Bookmark {
    public String id;        // UUID
    public long offset;      // byte offset trong file
    public String label;     // tên user đặt
    public int colorIndex;   // 0–4 (5 màu preset)
    public long createdAt;   // timestamp
}
```

**File mới:** `BookmarkStore.java`

```java
public class BookmarkStore {
    private static final int FREE_LIMIT = 5;

    // Key: sha256 của file URI → List<Bookmark>
    public List<Bookmark> getBookmarks(String fileKey) { ... }
    public boolean add(String fileKey, Bookmark bm, boolean isVip) {
        if (!isVip && getBookmarks(fileKey).size() >= FREE_LIMIT) return false;
        // save to SharedPreferences as JSON
        return true;
    }
    public void remove(String fileKey, String bookmarkId) { ... }
    public void clear(String fileKey) { ... }
}
```

**Serialization:** Gson `List<Bookmark>` → JSON string trong SharedPreferences.
**Key:** `bookmark_${md5(fileUri)}` để tránh path quá dài.

### Phase 2: Bookmark Drawer UI (ngày 2)

**File mới:** `app/src/main/res/layout/drawer_bookmarks.xml`
- `NavigationView` hoặc `BottomSheetDialogFragment`
- RecyclerView list bookmarks
- Header: count + "Add" button
- Footer: VIP promo nếu FREE user đã đủ 5

**File mới:** `AdtBookmarkList.java` — RecyclerView adapter
- Item: color dot | label | offset hex | [edit] [delete]
- Swipe to delete
- Tap → dismiss drawer + scroll hex view tới offset đó (blink highlight)

**File mới:** `app/src/main/res/layout/dlg_add_bookmark.xml`
- `MaterialAlertDialog` với TextInputEditText (label)
- 5 MaterialButton màu: đỏ, cam, xanh lá, xanh dương, tím
- Hiển thị offset hiện tại (readonly)

### Phase 3: Integration với Hex View (ngày 3)

**File sửa:** `ActMain.java`
- Thêm menu item "Bookmarks" trong toolbar overflow
- Long press trên hex row → context menu thêm option "Bookmark this offset"
- Drawer icon (bookmark icon) trên toolbar

**File sửa:** `AdtHexTextArray.java`
- `setBookmarks(List<Bookmark> bookmarks)` — method mới
- Trong `getView()`: nếu row offset match bookmark → hiển thị màu dot ở cột đầu
- Invalidate adapter khi bookmark thêm/xóa

**Navigation:**
- Tap bookmark trong drawer → `lv.setSelectionFromTop(position, 0)` + blink (dùng lại `GoToDialog.blinkBackground()`)

---

## Files cần tạo mới

```
feature/bookmark/
    Bookmark.java               ← Data model
    BookmarkStore.java          ← SharedPrefs persistence
    AdtBookmarkList.java        ← RecyclerView adapter

res/layout/
    drawer_bookmarks.xml        ← Drawer hoặc BottomSheet
    item_bookmark.xml           ← Row trong list
    dlg_add_bookmark.xml        ← Dialog thêm bookmark

test/.../feature/bookmark/
    BookmarkStoreTest.java      ← Unit test (limit, add, remove)
```

## Files cần sửa

```
ActMain.java        ← Menu, long press listener, drawer toggle
AdtHexTextArray.java ← Hiển thị bookmark indicator trong row
```

---

## Definition of Done

- [ ] Long press hex row → option "Add Bookmark"
- [ ] Dialog thêm: nhập label, chọn màu
- [ ] Bookmark indicator (colored dot) hiện trong hex row
- [ ] Drawer bookmark list: hiển thị, tap → scroll + blink
- [ ] Xóa bookmark (swipe hoặc button)
- [ ] FREE user: tối đa 5 bookmarks, overflow → VIP dialog
- [ ] VIP user: unlimited bookmarks
- [ ] Bookmark persist qua lần mở lại file
- [ ] Unit test: BookmarkStore add/remove/limit
- [ ] Không crash khi file đổi tên (orphan bookmark)
