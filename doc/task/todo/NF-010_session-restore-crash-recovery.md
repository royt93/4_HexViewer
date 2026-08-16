# NF-010 — Session restore / khôi phục sau crash

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 8 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Lưu lại URI, vị trí xem, selection, edit đang dang dở để phục hồi sau khi app bị kill (OOM kill trên thiết bị RAM thấp là rủi ro thật của app này, xem lịch sử OOM fix).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/models/FileData.java:176`

## Acceptance Criteria

- [ ] App bị kill giữa lúc đang edit dở, mở lại app phải hỏi khôi phục và khôi phục đúng vị trí + edit chưa lưu
- [ ] Không tăng đáng kể overhead ghi state liên tục trong lúc edit bình thường
