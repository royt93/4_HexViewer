# BUG-005 — Undo/Redo command vẫn giữ strong reference tới Activity

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P1 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

UnDoRedo dùng WeakReference<ActMain> để chống leak, nhưng UpdateCommand/DeleteCommand bên trong 2 deque mUndo/mRedo lại giữ strong reference tới ActMain. Chống leak thực tế phụ thuộc 100% vào UnDoRedo.cleanup() được gọi đúng lúc trong onDestroy() — nếu 1 exception path nào đó bỏ qua cleanup, leak Activity quay lại.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/undoredo/commands/UpdateCommand.java:24-26`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/undoredo/commands/DeleteCommand.java:22-24`

## Acceptance Criteria

- [ ] UpdateCommand/DeleteCommand không giữ strong reference tới Activity (đổi sang WeakReference hoặc bỏ hẳn, chỉ giữ dữ liệu cần cho undo/redo)
- [ ] Leak protection không còn phụ thuộc duy nhất vào việc cleanup() được gọi đúng thời điểm
- [ ] Verify bằng LeakCanary (xem TD-012 — cần bật lại trước) sau khi xoay màn hình / đóng Activity nhiều lần với undo stack còn dữ liệu
