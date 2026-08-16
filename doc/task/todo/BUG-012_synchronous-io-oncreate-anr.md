# BUG-012 — validateIntent chạy I/O đồng bộ trên main thread lúc onCreate

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 — cần verify lại trước khi fix |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Gọi FileHelper.getFileSize() (mở file descriptor đồng bộ) ngay trong luồng xử lý intent khi mở app từ "Open with…" — với provider chậm (Drive, Dropbox…) có thể ANR lúc khởi động.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java:319-324`

## Acceptance Criteria

- [ ] Verify lại bằng cách mở app với file từ 1 cloud provider chậm/giả lập độ trễ
- [ ] Nếu confirm đúng: đưa getFileSize() ra background, hiện loading state thay vì block main thread
