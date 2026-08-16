# PI-005 — File mẫu sẵn có lúc mở app lần đầu

| Trường | Giá trị |
|---|---|
| Loại | Product Idea |
| Ưu tiên | — |
| Điểm | — |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Giải quyết vấn đề "màn hình trống" lúc mới cài — 3 file mẫu (ELF, JPEG lỗi có EXIF cứu được, PNG giấu payload) tăng retention D1.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java:119-130`

## Acceptance Criteria

- [ ] Ý tưởng sản phẩm — cần chuẩn bị asset file mẫu, kiểm tra bản quyền/nguồn gốc trước khi bundle vào APK
