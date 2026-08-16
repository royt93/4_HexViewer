# BUG-011 — onTrimMemory clear data nhưng không clear Undo/Redo stack

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 — cần verify lại trước khi fix |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Sau khi OS trim memory (TRIM_MEMORY_COMPLETE), mFileData=null và adapter bị clear, nhưng mUnDoRedo không gọi .clear() — nếu user undo/redo sau đó, command có thể tham chiếu index/data không còn tồn tại.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java:998-1026`

## Acceptance Criteria

- [ ] Verify lại: tái hiện được onTrimMemory(COMPLETE) rồi thử Undo — có thực sự crash/corrupt hay không
- [ ] Nếu confirm đúng: onTrimMemory phải gọi mUnDoRedo.clear() cùng lúc với clear adapter/data
