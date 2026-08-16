# NF-013 — Chế độ Read-only/View lock cho file mở từ intent VIEW

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

File mở qua intent VIEW từ app khác hiện vào thẳng luồng edit bình thường, dễ sửa nhầm file hệ thống/nhạy cảm mà user chỉ định mở để xem.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java`
- `app/src/main/AndroidManifest.xml`

## Acceptance Criteria

- [ ] File mở qua ACTION_VIEW mặc định ở chế độ read-only, có toggle rõ ràng để chuyển sang edit
- [ ] File mở qua ACTION_EDIT giữ nguyên hành vi edit như hiện tại
- [ ] Có indicator UI rõ ràng cho biết đang ở chế độ read-only hay edit
