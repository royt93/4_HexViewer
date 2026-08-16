# NF-002 — Safe Save: ghi temp file rồi atomic-rename

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P0 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | — |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Fix trực tiếp BUG-001. Ghi ra temp file trước, chỉ thay thế file gốc sau khi ghi + verify thành công. NÊN LÀM CÙNG LÚC với BUG-001, không tách PR riêng.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskSave.java`

## Acceptance Criteria

- [ ] Xem Acceptance Criteria của BUG-001 — 2 task này là 1 đơn vị công việc
