# BUG-003 — VIP key hardcode trong APK, crack 1 lần dùng mãi mãi

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P0 |
| Điểm | 8 |
| Trạng thái | todo |
| Độ tin cậy | 4/4 — đồng thuận tuyệt đối |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

2 key VIP master chỉ encode Base64 (không phải mã hoá), check bằng so sánh chuỗi local trong lookupDays(), không có xác thực server nào. Decompile APK 1 lần (jadx/apktool) là lấy được key, share công khai — mọi user cài app đều free VIP vĩnh viễn.

Trầm trọng hơn vì proguard-rules.pro có -keep class ...ui/** { *; } và ...util/** { *; } gần như không obfuscate gì, khiến việc tìm ra key càng dễ.

LƯU Ý: user đã quyết định CHƯA xử lý bug này ngay, ưu tiên việc khác trước — giữ task ở đây làm backlog nhưng không đưa vào sprint hiện tại.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipKeys.kt:5-6, 24-25`
- `app/proguard-rules.pro:145-150 (liên quan TD-006)`

## Acceptance Criteria

- [ ] Không còn secret cố định nào trong APK có thể unlock VIP vĩnh viễn cho mọi thiết bị
- [ ] Có cơ chế xác thực có yếu tố server-side hoặc theo thiết bị (không phải string-match thuần local)
- [ ] Xem PI-001 để có hướng giải pháp đầy đủ (Play Billing) — đây là fix tối thiểu, PI-001 là fix triệt để
