# NF-001 — In-place random-access edit thật sự cho file lớn

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 13 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 — cần fix BUG-006 trước, liên quan TD-005 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Dùng lại RandomAccessFileChannel RW mode đang có sẵn nhưng chưa được wire vào bất kỳ đâu (openForReadWrite() hiện unused) để bỏ trần kích thước file hiện tại.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/util/io/RandomAccessFileChannel.java:75`

## Acceptance Criteria

- [ ] Edit file lớn không cần load toàn bộ vào RAM
- [ ] BUG-006 (fd/stream leak ở mode RW) phải fix trước khi feature này đi vào production
- [ ] Tương thích với model paging từ TD-005 nếu TD-005 đã làm trước đó
