# NF-006 — Bàn phím hex/ASCII riêng khi edit

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Đỡ phải chuyển qua lại bàn phím QWERTY hệ thống khi edit byte. Bàn phím riêng 0-9 A-F + macro chèn nhanh (0x00, 0xFF...).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActLineUpdate.java:126-133`

## Acceptance Criteria

- [ ] Bàn phím hex 16 phím (0-9, A-F) khi edit trong ActLineUpdate, không cần gọi bàn phím hệ thống
- [ ] Có tối thiểu 2-3 macro chèn nhanh giá trị byte thường dùng
