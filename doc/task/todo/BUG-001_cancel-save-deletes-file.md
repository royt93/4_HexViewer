# BUG-001 — Cancel Save làm mất file gốc

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P0 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

TaskSave ghi đè trực tiếp lên file gốc (truncate in-place), không dùng temp-file + atomic rename. Nếu user bấm Cancel giữa lúc save (hoặc có lỗi), code còn chủ động gọi DocumentFile.delete() để "dọn dẹp" — xoá luôn file gốc của user. Đây là mất dữ liệu thật, ưu tiên cao nhất toàn backlog.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskSave.java:91-100, 150-154`

## Acceptance Criteria

- [ ] Save phải ghi ra file/URI tạm trước, chỉ thay thế file gốc sau khi ghi + verify thành công (atomic rename hoặc tương đương qua SAF)
- [ ] Cancel giữa chừng KHÔNG được xoá hay làm hỏng file gốc — file gốc phải nguyên vẹn như trước khi bấm Save
- [ ] Có unit/instrumentation test mô phỏng cancel giữa lúc save và xác nhận file gốc không đổi
- [ ] Ghép chung với NF-002 (Safe Save) — nên fix 1 lần, không tách 2 PR riêng
