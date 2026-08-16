# KF-001 — Android-native artifact inspector (APK/DEX/ELF/SQLite)

| Trường | Giá trị |
|---|---|
| Loại | Killer Feature |
| Ưu tiên | — |
| Điểm | — |
| Trạng thái | todo |
| Độ tin cậy | 3/4 — hội tụ mạnh nhất toàn bộ report |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Đọc hiểu APK/AndroidManifest binary XML, DEX header/method table, ELF .so symbol table, SQLite/WAL, logcat/tombstone — kèm live-patch (toggle debuggable, đổi version code) + re-sign/re-align ngay trên máy. Không desktop hex editor nào làm việc này native.

Đây là killer feature được cả 3/4 nguồn độc lập đề xuất biến thể của cùng 1 ý tưởng — tín hiệu mạnh nhất trong toàn bộ report.

## File liên quan

- `(feature mới hoàn toàn)`

## Acceptance Criteria

- [ ] Ý tưởng độc quyền — cần research riêng để scope MVP (vd bắt đầu từ đọc AndroidManifest binary XML trước, APK re-sign để sau)
