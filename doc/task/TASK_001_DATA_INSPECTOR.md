# T-001 — Data Inspector Panel

**Status:** `todo`
**Effort:** M — 3–5 ngày
**Priority:** P0 ⭐ (Core missing feature)
**VIP Gate:** Basic types (Free) / Extended types + Endian toggle (VIP)

---

## Mô tả

Khi user tap vào 1 hoặc nhiều bytes trong hex view, hiển thị một **bottom sheet panel** (hoặc sidebar ở landscape) với giá trị của bytes đó được interpret theo nhiều kiểu dữ liệu khác nhau. Đây là tính năng có ở mọi hex editor chuyên nghiệp (HxD, 010 Editor, ImHex, Hex Fiend).

**Trước khi có:** User tap vào dòng → vào ActLineUpdate để edit, không có cách xem giá trị decoded nhanh.

**Sau khi có:** User tap dài (long press) vào 1 byte → bottom sheet hiện ngay, xem int8=65, ASCII='A', hex=0x41, v.v. Không cần rời khỏi hex view.

---

## UI/UX

```
┌─────────────────────────────────────────┐
│  Data Inspector              [×]        │
│  Offset: 0x0041  |  4 bytes selected   │
├─────────────────────────────────────────┤
│  Endian:  [LE] [BE]                     │
├──────────────┬──────────────────────────┤
│  Int8        │  65                      │
│  UInt8       │  65                      │
│  Int16       │  16,706  (LE)            │
│  UInt16      │  16,706                  │
│  Int32       │  1,145,258,561  (LE)     │
│  UInt32      │  1,145,258,561           │
│  Float32     │  1.28e+19                │
├──────────────┴──────────────────────────┤
│  [VIP] Int64 / UInt64 / Float64         │
├─────────────────────────────────────────┤
│  ASCII       │  ABCD                    │
│  Hex         │  41 42 43 44             │
│  Binary      │  01000001 01000010...    │
│  Base64      │  QUJDRA==               │
└─────────────────────────────────────────┘
```

---

## Ưu điểm

- ✅ **Core value** — là tính năng mà user hex editor expect nhất
- ✅ **Dễ upsell VIP** — 64-bit types và Base64 gate VIP tự nhiên
- ✅ **Không thay đổi main flow** — chỉ thêm bottom sheet, code hiện tại không bị ảnh hưởng
- ✅ **Tăng rating** — user thường đánh giá thấp vì thiếu feature này
- ✅ **No network needed** — toàn bộ logic offline

## Nhược điểm

- ⚠️ Cần xử lý **endianness** (Little Endian vs Big Endian) đúng
- ⚠️ Khi select < 4 bytes: Int32/Float32 cần disabled/greyed out
- ⚠️ Bottom sheet trên landscape mode cần layout khác (side panel)
- ⚠️ Long press conflict với existing multi-select gesture — cần phân biệt

---

## Plan chi tiết

### Phase 1: Data Engine (ngày 1)

**File mới:** `app/src/main/java/com/galaxyjoy/hexviewer/ui/inspector/DataInspector.java`

```java
// Responsibility: nhận byte[], offset, endian → trả về Map<Type, String>
public class DataInspector {
    public enum Endian { LITTLE, BIG }

    public static Map<String, String> inspect(byte[] data, int offset,
                                              int length, Endian endian) {
        // Tính toán tất cả types từ bytes tại offset..offset+length
    }
    // int8, uint8, int16, uint16, int32, uint32, float32
    // [VIP] int64, uint64, float64
    // ascii, hex string, binary string, base64
}
```

**File test:** `app/src/test/.../DataInspectorTest.java`
- Test với byte `0x41` → int8=65, ASCII='A'
- Test endian: `[0x01, 0x00]` LE → int16=1, BE → int16=256
- Test overflow: 1 byte → int16 disabled

### Phase 2: Bottom Sheet UI (ngày 2)

**File mới:** `app/src/main/res/layout/bottom_sheet_data_inspector.xml`
- `BottomSheetDialogFragment` Material 3
- RecyclerView với 2 columns: Type | Value
- Toggle buttons: LE / BE (MaterialButtonToggleGroup)
- Endian toggle ở header
- VIP rows có icon lock 🔒 và blur/disabled style

**File mới:** `app/src/main/java/com/galaxyjoy/hexviewer/ui/inspector/DataInspectorSheet.kt`
- Extends `BottomSheetDialogFragment`
- Nhận `byteArray: ByteArray` + `offset: Int` + `selectedCount: Int` qua args
- Call `DataInspector.inspect()` và bind vào RecyclerView

**File mới:** `app/src/main/res/layout/item_inspector_row.xml`
- 2 columns: type label (TextView) | value (TextView với monospace font)
- VIP rows: alpha 0.4, icon lock, onClickListener → mở ActVipManagement

### Phase 3: Integration với ActMain / HexView (ngày 3)

**File sửa:** `ActMain.java` / `AdtHexTextArray.java`
- Long press trên 1 row → show DataInspectorSheet với bytes của row đó
- Select range (multi-select mode) → thêm action "Inspect" trong action bar
- Pass `byte[]` qua static bridge (như ActLineUpdate đang làm với `sBridgeTexts`)

### Phase 4: Landscape mode (ngày 4)

- Detect `Configuration.ORIENTATION_LANDSCAPE`
- Thay bottom sheet bằng side panel (SlidingPaneLayout hoặc ConstraintLayout với constraint)
- Tự động refresh khi user scroll + tap row khác

### Phase 5: Test + Polish (ngày 5)

- Unit test `DataInspectorTest` (Robolectric)
- Instrumented test: long press → bottom sheet visible
- Edge cases: 0 bytes selected, >8 bytes selected (truncate)
- Animation: sheet slide up với Material motion

---

## Files cần tạo mới

```
app/src/main/java/.../ui/inspector/
    DataInspector.java          ← Engine (pure Java, no Android deps)
    DataInspectorSheet.kt       ← BottomSheetDialogFragment
    AdtInspectorRows.java       ← RecyclerView adapter

app/src/main/res/layout/
    bottom_sheet_data_inspector.xml
    item_inspector_row.xml

app/src/test/.../ui/inspector/
    DataInspectorTest.java
```

## Files cần sửa

```
ActMain.java                    ← Thêm long press listener, show sheet
AdtHexTextArray.java            ← Thêm callback onLongPress(position, bytes)
```

---

## Definition of Done

- [ ] Long press 1 row trong hex view → DataInspector bottom sheet hiện
- [ ] Đủ 7 types (int8 → float32) cho FREE user
- [ ] Endian toggle LE/BE hoạt động
- [ ] VIP types (int64, float64, Base64) bị lock với FREE user
- [ ] Tap VIP lock → mở ActVipManagement
- [ ] Landscape: hiển thị dạng side panel thay bottom sheet
- [ ] Unit test DataInspector pass
- [ ] Instrumented test pass trên S24 Ultra
- [ ] Không memory leak (WeakReference trong sheet)
