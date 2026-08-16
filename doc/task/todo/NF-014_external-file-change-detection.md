# NF-014 — Phát hiện file bị thay đổi từ bên ngoài

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P3 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

So mtime/size trước khi ghi đè, tránh mất thay đổi của app khác đã sửa file trong lúc HexViewer đang mở nó.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/util/io/FileHelper.java:218`

## Acceptance Criteria

- [ ] Trước khi Save, so sánh mtime/size hiện tại với lúc mở file; nếu khác, cảnh báo user trước khi ghi đè
