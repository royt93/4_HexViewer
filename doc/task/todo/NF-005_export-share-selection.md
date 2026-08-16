# NF-005 — Export/Share vùng byte đã chọn

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P1 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Hiện tại HexMultiChoiceCallback.actionCopy() chỉ copy phần render ASCII vào clipboard. Hoàn toàn chưa có workflow export selection ra raw binary/hex string/C-Kotlin array/Base64 — đây là tính năng cơ bản mà mọi hex editor desktop đều có.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/multiChoice/HexMultiChoiceCallback.java`

## Acceptance Criteria

- [ ] Menu "Export/Share selection" xuất hiện khi có vùng byte được chọn
- [ ] Hỗ trợ tối thiểu: copy as hex string, copy as C array, export selection ra file mới, share qua system share sheet
- [ ] Hoạt động đúng với selection lớn (không OOM) — tái dùng nguyên tắc từ TD-002
