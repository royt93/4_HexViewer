# TD-006 — Proguard keep toàn bộ ui/** và util/** vô hiệu hoá obfuscate

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 — liên quan trực tiếp BUG-003 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

-keep class ...ui/** { *; } và ...util/** { *; } giữ nguyên gần như toàn bộ code, vô hiệu hoá obfuscate/shrink của R8. Đây cũng là lý do BUG-003 (VIP key) dễ bị decompile — nên cân nhắc làm cùng lúc với BUG-003/PI-001 dù hiện đang deprioritized.

## File liên quan

- `app/proguard-rules.pro:145-150`

## Acceptance Criteria

- [ ] Thu hẹp -keep rule về đúng những class thực sự cần reflection (đã có 1 số rule scoped sẵn cho Fragment/Preference ngay bên dưới, dùng làm mẫu)
- [ ] Build release vẫn chạy đúng chức năng sau khi thu hẹp keep rules (test đầy đủ regression)
- [ ] APK sau khi build có size nhỏ hơn và code bị obfuscate thực sự (verify bằng decompile thử)
