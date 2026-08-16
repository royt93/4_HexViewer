# NF-003 — Find & Replace hàng loạt (hex/text)

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 8 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

App hiện chỉ có Find, chưa có Replace. Ghép cặp tự nhiên với T-004 Pattern Search — nên làm sau/cùng T-004.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/search/`

## Acceptance Criteria

- [ ] Replace 1 match hoặc Replace All cho cả hex pattern và text pattern
- [ ] Có preview trước khi apply, và tích hợp với Undo/Redo
- [ ] Không phá vỡ index/filter mapping (liên quan trực tiếp bài học từ BUG-002)
