# BUG-013 — Sửa dòng hex lớn (>50KB) có thể bị mất âm thầm

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P1 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 — cụ thể nhưng cần verify sớm vì severity cao (mất data âm thầm) |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

ActLineUpdate.onDestroy() clear 2 static field sBridgeResultReferenceString/sBridgeResultNewString (dùng để truyền data lớn thay vì Intent extras). Nếu field này bị clear trước/trong lúc ActivityResultLauncher callback đọc kết quả, callback đọc null/rỗng → edit của user bị mất mà không có thông báo lỗi nào.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActLineUpdate.java:267-271`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/launcher/LauncherLineUpdate.java:77-92`

## Acceptance Criteria

- [ ] Verify: sửa 1 dòng hex có payload >50KB, xác nhận edit được lưu đúng 100% các lần, kể cả khi hệ thống có áp lực bộ nhớ/Activity bị recreate
- [ ] Nếu confirm đúng: đảm bảo static bridge chỉ bị clear SAU KHI callback đã đọc xong dữ liệu, không phải trong onDestroy() vô điều kiện
