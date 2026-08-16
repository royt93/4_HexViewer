# BUG-009 — ANR khi chuyển tab Plain Text với file lớn

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

refresh() post refreshPlain() vào Handler chạy trên MAIN-THREAD looper, duyệt tới 50.000 dòng với List<Byte> autobox từng byte → giật/đứng UI. Cùng nhóm lỗi mà TaskOpen/TaskSave đã né bằng cách chạy background, nhưng path Plain Text bị bỏ sót.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/payload/PayloadPlainSwipe.java:126-140, 149-206`

## Acceptance Criteria

- [ ] refreshPlain() phải chạy off main-thread cho file có số dòng lớn (dùng ngưỡng tương tự MAX_PLAIN_TEXT_LINES hiện có)
- [ ] Chuyển tab Plain Text trên file lớn không được gây giật frame/ANR
- [ ] Không cần global fix hiệu năng boxing ở đây — chỉ cần đưa việc nặng ra khỏi main thread (fix triệt để hơn xem TD-002 style)
