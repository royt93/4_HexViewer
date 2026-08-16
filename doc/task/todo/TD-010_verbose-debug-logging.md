# TD-010 — Log.d tràn lan trong hot loop của TaskOpen

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P3 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Log.d("roy93~", ...) per-line, mỗi 50 vòng lặp, một số log cả full file path/URI (nhạy cảm privacy cho app chuyên đọc file bất kỳ của user). Proguard có strip ở release build, nhưng debug/profiling build vẫn trả giá đầy đủ.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskOpen.java (nhiều dòng, per-line/per-50-vòng-lặp)`

## Acceptance Criteria

- [ ] Loại bỏ hoặc gate log path/URI nhạy cảm ngay cả ở debug build
- [ ] Log trong hot loop giảm tần suất hoặc chuyển sang mức Verbose có thể tắt dễ dàng
