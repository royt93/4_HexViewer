# NF-007 — Công cụ toán bit trên vùng chọn

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P3 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Hữu ích cho phân tích file bị obfuscate/XOR. XOR/AND/OR/NOT/shift/đảo endian trên vùng byte đã chọn.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActLineUpdate.java:325-339`
- `app/src/main/java/com/galaxyjoy/hexviewer/util/SysHelper.java:107`

## Acceptance Criteria

- [ ] Áp dụng được tối thiểu XOR (single-byte key) và đảo endian trên selection, có Undo
- [ ] Kết quả ghi trực tiếp vào buffer đang edit, không cần export ra ngoài rồi import lại
