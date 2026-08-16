# NF-008 — Chọn encoding cho Plain Text pane

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Text decoder hiện hardcode ASCII printable [0x20, 0x7E], mọi byte ngoài range hiện dấu chấm. Cần hỗ trợ UTF-8/16 LE/BE, Shift-JIS, Windows-1252 để đọc đúng string trong binary ngoại ngữ, ROM game, log.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/util/SysHelper.java:389`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/AdtPlainTextListArray.java:38`

## Acceptance Criteria

- [ ] Có bộ chọn encoding trong Plain Text view, áp dụng ngay không cần mở lại file
- [ ] Tối thiểu hỗ trợ: ASCII, UTF-8, UTF-16 LE/BE, Windows-1252
- [ ] Byte không decode được vẫn hiện placeholder rõ ràng (không silent-wrong)
