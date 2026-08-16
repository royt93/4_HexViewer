# TD-012 — LeakCanary bị comment-out kể cả ở dev build

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P3 |
| Điểm | 1 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Trái với mô tả trong doc/BUILD_VARIANTS_GUIDE.md, mất luôn công cụ phát hiện chính xác class bug (leak) mà app này đã fix tới 24 lần trong lịch sử theo doc/MEMORY_LEAK_FIX_REPORT.md.

## File liên quan

- `app/build.gradle:185 (dòng debugImplementation leakcanary đang bị comment — đã verify)`

## Acceptance Criteria

- [ ] Bật lại LeakCanary cho debug/dev build variant
- [ ] Cập nhật doc/BUILD_VARIANTS_GUIDE.md nếu hành vi thực tế khác mô tả
- [ ] Hữu ích trực tiếp để verify BUG-005 sau khi fix
