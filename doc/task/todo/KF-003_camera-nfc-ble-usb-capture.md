# KF-003 — Thu byte trực tiếp từ Camera/NFC/BLE/USB-OTG

| Trường | Giá trị |
|---|---|
| Loại | Killer Feature |
| Ưu tiên | — |
| Điểm | — |
| Trạng thái | todo |
| Độ tin cậy | 3/4 — hội tụ mạnh |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Scan QR/barcode/chụp ảnh hex dump, chạm thẻ NFC, kết nối thiết bị BLE hoặc USB-serial/flash programmer → đổ thẳng payload vào hex view. Năng lực này desktop không thể làm nếu thiếu phần cứng rời.

## File liên quan

- `(feature mới hoàn toàn)`

## Acceptance Criteria

- [ ] Ý tưởng độc quyền — nên tách nhỏ theo từng nguồn input (QR trước, NFC sau, BLE/USB-OTG sau cùng vì cần phần cứng test)
