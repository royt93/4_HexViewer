# TD-004 — Migrate TaskRunner tự chế sang Kotlin Coroutines

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 8 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

TaskRunner là AsyncTask-tự-chế, quản lý lifecycle thủ công dễ vỡ — chính là root cause của BUG-004 và góp phần vào BUG-005. Migrate sang Coroutines + lifecycleScope cho cancellation/error propagation an toàn hơn.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskRunner.java`

## Acceptance Criteria

- [ ] Toàn bộ task chạy qua Coroutines + lifecycleScope, không còn Handler thủ công cho lifecycle
- [ ] Cancellation, exception, completion không còn race condition (bug BUG-004 tự động biến mất)
- [ ] API bề mặt cho TaskOpen/TaskSave/TaskHash giữ tương thích, không phải viết lại toàn bộ business logic
