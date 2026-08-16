# BUG-015 — getSnapshot nuốt OutOfMemoryError thành list rỗng khi search

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 1 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

User không phân biệt được "không tìm thấy kết quả" với "search bị fail do thiếu RAM" — dễ gây kết luận sai khi làm forensic trên file lớn.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/models/LineEntries.java:42`

## Acceptance Criteria

- [ ] Khi search fail do OutOfMemoryError, phải báo lỗi rõ ràng cho user (khác với "0 kết quả")
- [ ] Không để OOM bị nuốt thành trạng thái thành công giả
