# BUG-008 — NPE khi mở file từ URI lạ/malformed

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

getFileName() gọi uri.getScheme().equals("content") không check null; getParentUri() gọi .length() trên getEncodedPath() có thể null. Trigger được qua ActMain.processIntentUri khi app khác share file qua VIEW/EDIT intent với URI bất thường (thiếu scheme, path rỗng...).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/util/io/FileHelper.java:256-260, 360-364`

## Acceptance Criteria

- [ ] getFileName()/getParentUri() không crash với URI thiếu scheme hoặc encoded path null
- [ ] Có fallback hợp lý (vd tên file mặc định) thay vì NPE khi metadata không lấy được
- [ ] Test với URI giả lập thiếu scheme/path
