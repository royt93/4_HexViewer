# NF-011 — Nhận diện file có cấu trúc (magic byte)

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 8 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Nhận diện ZIP/APK/ELF/DEX/PNG/JPEG/PDF/SQLite qua magic byte, kèm annotate header/section cơ bản. Nền tảng chung với T-001 Data Inspector — nên gộp chung 1 epic khi lên sprint.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActFileInfo.java`

## Acceptance Criteria

- [ ] Nhận diện đúng tối thiểu 6 định dạng phổ biến kể trên qua magic byte
- [ ] Hiển thị được thông tin header cơ bản (không cần full parser) ngay trong File Info
- [ ] Kiến trúc đủ mở để NF-004 (binary template) build tiếp lên trên
