# TD-009 — Pattern confirm-unsaved-changes bị copy-paste 4 lần

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P3 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Pattern "confirm unsaved changes → save → tiếp tục" (UIHelper.confirmFileChanged + new TaskSave().execute()) bị lặp lại gần y hệt 4 chỗ.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java (dòng ~373, ~669, ~866, ~967)`

## Acceptance Criteria

- [ ] Gom về 1 helper runWithSaveConfirmation(Runnable) dùng chung cho cả 4 call site
- [ ] Hành vi UI không đổi so với trước khi refactor
