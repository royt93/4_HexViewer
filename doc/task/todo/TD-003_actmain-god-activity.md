# TD-003 — ActMain là God Activity ~1030 dòng

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 13 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

File I/O, undo/redo, search, popup, ad lifecycle, VIP badge đều nằm chung 1 class. Tách EditorViewModel (MVVM) — lợi ích phụ: giữ được buffer/undo history qua config change thay vì load lại từ disk.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java`

## Acceptance Criteria

- [ ] Logic file I/O, undo/redo, search tách khỏi Activity vào ViewModel/repository riêng
- [ ] Xoay màn hình không làm mất undo history hoặc phải load lại file từ disk
- [ ] ActMain.java giảm đáng kể số dòng, mỗi trách nhiệm có thể test độc lập
