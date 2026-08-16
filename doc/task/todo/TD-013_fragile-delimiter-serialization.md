# TD-013 — Serialize state bằng string ghép delimiter dễ vỡ

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P3 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

RecentlyOpened/FileData serialize bằng string ghép delimiter "|"/"^" — dễ vỡ khi thêm field mới, khó parse an toàn.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/models/RecentlyOpened.java:51`
- `app/src/main/java/com/galaxyjoy/hexviewer/models/FileData.java:176`

## Acceptance Criteria

- [ ] Chuyển sang JSON có version hoặc Room table nhỏ cho recent-files state
- [ ] Migration path cho data cũ đã lưu theo format delimiter (không mất recent-list của user hiện tại)
