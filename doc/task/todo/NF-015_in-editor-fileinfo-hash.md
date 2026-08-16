# NF-015 — File Info/Hash Calculator dùng file đang mở

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P3 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Hiện tại tap "File Info"/"Hash Calculator" mở activity picker mới, bắt chọn lại file dù đang có file mở sẵn trong ActMain.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActFileInfo.java:124`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActHashCalculator.java:105`

## Acceptance Criteria

- [ ] Từ màn hình editor đang mở file, vào File Info/Hash Calculator dùng thẳng file đó, không bắt chọn lại
