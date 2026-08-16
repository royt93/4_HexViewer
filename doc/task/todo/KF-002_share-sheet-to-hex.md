# KF-002 — Share-sheet-to-hex ingestion

| Trường | Giá trị |
|---|---|
| Loại | Killer Feature |
| Ưu tiên | — |
| Điểm | — |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Đăng ký HexViewer làm Share target hệ thống — mọi app khác (mail, browser, file manager) có thể "Open in HexViewer" trực tiếp, không cần mở file manager trước.

## File liên quan

- `app/src/main/AndroidManifest.xml:41-138 (đã có MIME intent filter rộng, cần khai báo thêm Share target)`

## Acceptance Criteria

- [ ] Ý tưởng độc quyền — chi phí implement thấp nhất trong nhóm killer feature (chỉ cần khai báo intent-filter ACTION_SEND, dùng lại luồng mở file hiện có)
