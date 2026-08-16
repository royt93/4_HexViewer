# BUG-002 — Xoá sai dòng khi đang filter/search

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P0 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Vị trí (position) được chọn để xoá là index trong danh sách ĐÃ LỌC, nhưng LineEntries.removeItem() lại remove((Integer) position) trên danh sách GỐC (remove theo value, không phải theo index thực) — xoá nhầm dòng và làm hỏng mapping index giữa danh sách lọc và danh sách gốc.

DeleteCommand.execute() còn clear filter trước khi gọi removeItem(position), càng làm sai lệch ý nghĩa của position.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/multiChoice/HexMultiChoiceCallback.java:77-82`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/undoredo/commands/DeleteCommand.java:40-48`
- `app/src/main/java/com/galaxyjoy/hexviewer/models/LineEntries.java:107-110`

## Acceptance Criteria

- [ ] Xoá dòng khi đang có filter active phải xoá đúng dòng người dùng nhìn thấy trên màn hình, không xoá nhầm dòng khác
- [ ] LineEntries.removeItem() phải remove đúng theo index thực trong danh sách gốc (map từ filtered-index sang real-index), không remove theo value
- [ ] Undo sau khi xoá lúc đang filter phải khôi phục đúng dòng đã xoá, đúng vị trí
- [ ] Có test case: tạo filter ra danh sách con, xoá 1 dòng giữa danh sách lọc, assert đúng dòng bị xoá trong danh sách gốc
