# TD-005 — Model whole-file-in-memory là trần kiến trúc

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P1 |
| Điểm | 21 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 — khuyến nghị làm trước T-002 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Đây là root cause của hàng loạt OOM fix chắp vá đã ghi trong doc/MEMORY_LEAK_FIX_REPORT.md / PERFORMANCE_OPTIMIZATIONS.md. T-002 (File Diff) tự doc của nó đã ghi cần "partial-diff mode cho file >10MB" — nếu không paging hoá model trước, T-002 sẽ đâm thẳng vào trần này.

KHUYẾN NGHỊ: làm task này (ít nhất bản rút gọn/MVP paging) TRƯỚC KHI bắt đầu T-002 File Diff.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskOpen.java`
- `app/src/main/java/com/galaxyjoy/hexviewer/models/LineEntries.java`

## Acceptance Criteria

- [ ] File lớn không cần load toàn bộ vào RAM để xem/edit vùng nhỏ
- [ ] Có cơ chế paging/windowed loading rõ ràng thay cho các guard/GC thủ công rải rác hiện tại
- [ ] T-002 File Diff có thể build trên nền model này mà không cần thiết kế lại giữa chừng
