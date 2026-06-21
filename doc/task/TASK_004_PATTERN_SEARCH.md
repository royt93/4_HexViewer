# T-004 — Advanced Pattern Search

**Status:** `todo`
**Effort:** M — 4–6 ngày
**Priority:** P1
**VIP Gate:** Basic search (Free) / Wildcard + Regex + History (VIP)

---

## Mô tả

Nâng cấp toàn diện tính năng search hiện tại, bổ sung:
- **Hex pattern với wildcard** (`FF ?? 00 ??` — `??` match bất kỳ byte nào)
- **Text search** với case-insensitive option
- **Highlight TẤT CẢ matches** cùng lúc (không chỉ scroll tới 1 kết quả)
- **Next/Prev match navigation** với count badge (`3 / 17`)
- **Search history** — lưu các pattern đã tìm
- **[VIP] Regex search** trên hex data

**Hiện tại:** Search chỉ filter list theo text prefix — rất hạn chế.

---

## UI/UX

### Search bar mới (thay thế SearchView hiện tại)

```
┌───────────────────────────────────────────────────┐
│ [🔍 FF ?? 00 ??              ] [×] [HEX] [TXT]   │
│ ← 3 / 17 →                          [▼ History]  │
└───────────────────────────────────────────────────┘
```

### Hex view với highlights

```
Offset   Hex Bytes                          ASCII
0000: 48 65 6C 6C 6F [FF 00 00 FF] ...  Hello....   ← match 1 (vàng)
0010: 00 FF [FF 00 00 FF] 48 65 ...     ..ÿ.Hello   ← match 2 (vàng)
0020: [FF 00 00 FF] 49 46 00 01 ...     ÿ...IF..    ← match hiện tại (cam)
```

### History dropdown

```
┌─────────────────────────────┐
│ Recent searches:            │
│   FF ?? 00 ??               │
│   4D 5A                     │
│   [VIP] regex: ^FF[0-9A-F]+ │
│   [Clear history]           │
└─────────────────────────────┘
```

---

## Ưu điểm

- ✅ **Power user retention** — developer/security researcher dùng mỗi ngày
- ✅ **Natural VIP upsell** — regex và history là features người dùng sẵn sàng trả
- ✅ **Complement với Data Inspector** — tìm pattern → inspect bytes tại match
- ✅ **Không thay đổi main layout** — chỉ upgrade search bar

## Nhược điểm

- ⚠️ **Wildcard parser** phức tạp hơn tưởng — cần handle `??`, ranges `[0-9]`, `*`
- ⚠️ **Highlight all matches** trong ListView lớn → performance cần optimize (chỉ highlight visible rows)
- ⚠️ **Regex trên binary** — cần convert từng chunk sang string đúng encoding
- ⚠️ **Search state** phải survive orientation change (savedInstanceState)

---

## Plan chi tiết

### Phase 1: Pattern Engine (ngày 1–2)

**File mới:** `app/src/main/java/com/galaxyjoy/hexviewer/feature/search/PatternMatcher.java`

```java
public class PatternMatcher {

    // Parse "FF ?? 00 ??" → byte[] pattern với -1 cho wildcard
    public static int[] parseHexPattern(String pattern) {
        // "FF" → 0xFF, "??" → -1 (wildcard)
        // Validate: chỉ chấp nhận hex pairs và ??
    }

    // Tìm tất cả vị trí match trong byte array
    // Trả về List<Long> offsets
    public static List<Long> findAll(byte[] data, int[] pattern) {
        List<Long> results = new ArrayList<>();
        for (int i = 0; i <= data.length - pattern.length; i++) {
            if (matches(data, i, pattern)) results.add((long) i);
        }
        return results;
    }

    private static boolean matches(byte[] data, int offset, int[] pattern) {
        for (int j = 0; j < pattern.length; j++) {
            if (pattern[j] == -1) continue; // wildcard
            if ((data[offset + j] & 0xFF) != pattern[j]) return false;
        }
        return true;
    }

    // [VIP] Regex search: convert data chunk to ISO-8859-1 string, apply regex
    public static List<Long> findAllRegex(byte[] data, Pattern regex) { ... }
}
```

**File test:** `PatternMatcherTest.java`
- `FF 00` trong `[FF, 00, FF, 00]` → offsets [0, 2]
- `FF ?? FF` với `[FF, AA, FF, BB]` → [0]
- Empty pattern → empty list
- Pattern dài hơn data → empty list

### Phase 2: Search Results Model (ngày 2)

**File mới:** `SearchState.java`

```java
public class SearchState {
    public String query;
    public boolean isHexMode;       // true = hex pattern, false = text
    public List<Long> matchOffsets; // tất cả offset match
    public int currentIndex;        // vị trí hiện tại (cho prev/next)

    public long currentOffset() {
        return matchOffsets.isEmpty() ? -1 : matchOffsets.get(currentIndex);
    }
    public void next() { currentIndex = (currentIndex + 1) % matchOffsets.size(); }
    public void prev() { currentIndex = (currentIndex - 1 + matchOffsets.size()) % matchOffsets.size(); }
}
```

**File mới:** `SearchHistory.java`

```java
public class SearchHistory {
    private static final int MAX_FREE = 5;
    private static final int MAX_VIP = 50;

    public void add(String query, boolean isVip) { ... }
    public List<String> getHistory(boolean isVip) { ... }
    public void clear() { ... }
}
// Lưu vào SharedPreferences dưới dạng JSON array
```

### Phase 3: Search UI (ngày 3)

**File mới:** `app/src/main/res/layout/view_search_bar.xml`
- `TextInputEditText` cho query
- `MaterialButtonToggleGroup`: [HEX] [TXT]
- Navigation row: [◀ Prev] [count: X/N] [▶ Next]
- History dropdown trigger [▼]

**File mới:** `SearchBarController.java`
- Manage search bar state
- Kết nối với `PatternMatcher`
- Gọi callback `onSearchResultsChanged(SearchState)` → ActMain cập nhật UI

**File sửa:** `ActMain.java`
- Replace `SearchView` hiện tại với `SearchBarController`
- Khi search results thay đổi → pass `matchOffsets` xuống `AdtHexTextArray`
- "Search" menu item → show/hide search bar với animation

### Phase 4: Highlight trong ListView (ngày 4)

**File sửa:** `AdtHexTextArray.java`

```java
// Thêm method
public void setSearchResults(List<Long> matchOffsets, long currentOffset) {
    this.matchOffsets = matchOffsets;
    this.currentOffset = currentOffset;
    notifyDataSetChanged();
}

@Override
public View getView(int position, View convertView, ViewGroup parent) {
    // ... existing code ...
    // Sau khi build hex string: highlight các bytes tại match positions
    LineEntry entry = entries.get(position);
    long rowOffset = entry.getOffset();
    for (Long matchOffset : matchOffsets) {
        if (matchOffset >= rowOffset && matchOffset < rowOffset + bytesPerLine) {
            int byteIndex = (int)(matchOffset - rowOffset);
            // Apply SpannableString highlight tại byteIndex
        }
    }
}
```

**Performance:** Chỉ iterate qua `matchOffsets` nằm trong visible range của ListView để tránh O(n×m).

### Phase 5: Async Search + Progress (ngày 5)

- Search chạy trong background thread (AsyncTask hoặc ExecutorService)
- Progress bar hiện khi đang search (file lớn)
- Cancel search khi user đổi query
- Kết quả trả về trên main thread

### Phase 6: VIP Gate + History UI (ngày 6)

- Wildcard `??` — Free
- Regex — VIP: hiện lock icon trong search mode toggle [HEX] [TXT] [🔒 REX]
- History ≤ 5 entries — Free; ≤ 50 entries — VIP
- Tap "Clear history" — luôn available

---

## Files cần tạo mới

```
feature/search/
    PatternMatcher.java         ← Pure search engine
    SearchState.java            ← Result model
    SearchHistory.java          ← SharedPrefs history
    SearchBarController.java    ← UI controller

res/layout/
    view_search_bar.xml         ← Custom search bar view
    dropdown_search_history.xml ← History list popup

test/.../feature/search/
    PatternMatcherTest.java     ← Unit tests
    SearchHistoryTest.java
```

## Files cần sửa

```
ActMain.java            ← Replace SearchView, handle search state
AdtHexTextArray.java    ← Thêm setSearchResults() + highlight logic
AdtPlainTextListArray.java ← Text search highlight
```

---

## Definition of Done

- [ ] Hex pattern search: `FF 00` tìm đúng tất cả matches
- [ ] Wildcard: `FF ?? 00` match đúng
- [ ] Text search (UTF-8) hoạt động
- [ ] Tất cả matches được highlight trong hex view
- [ ] Prev/Next navigation với counter `X/N`
- [ ] Current match highlight khác màu với các match còn lại
- [ ] Search chạy async, có cancel
- [ ] History lưu 5 entries (FREE) / 50 entries (VIP)
- [ ] [VIP] Regex search hoạt động
- [ ] Unit test PatternMatcher pass (edge cases)
- [ ] Performance: search 10MB file < 1s trên S24 Ultra
