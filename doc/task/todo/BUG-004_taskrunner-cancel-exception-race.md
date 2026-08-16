# BUG-004 — Race cancel/exception trong TaskRunner làm dialog treo

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P1 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 4/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

onException() chạy trên background thread — nếu listener (vd ActHashCalculator) update UI trực tiếp trong callback này sẽ crash CalledFromWrongThreadException.

Khi exception xảy ra, onPostExecute()/cleanup() không được gọi → ProgressTask đứng hình vĩnh viễn trên màn hình.

cancel() post onCancelled() trong khi finally của background thread có thể đang post onPostExecute() cùng lúc — có thể cả 2 cùng fire, hoặc fire nhầm.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskRunner.java:45-53, 75-97`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/ProgressTask.java:116-127`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskHash.java:159-165`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActHashCalculator.java:134-155`

## Acceptance Criteria

- [ ] onException() phải luôn được dispatch về main thread trước khi listener xử lý
- [ ] Mọi nhánh lỗi (exception, cancel, OOM) đều phải dẫn tới dismiss ProgressTask, không để dialog treo
- [ ] cancel() và hoàn thành bình thường không được cùng fire callback cho cùng 1 lần chạy — chỉ 1 trong 2
- [ ] Có test cho cả 3 nhánh: thành công, exception, cancel giữa chừng
