# BUG-010 — VIP key phân biệt hoa/thường trái với doc

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 1 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Comment của lookupDays() ghi "trim + uppercase" nhưng code chỉ gọi .trim(), không uppercase — user nhập key khác case với key lưu trữ bị báo "invalid key" sai.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipKeys.kt:15, 24-25`

## Acceptance Criteria

- [ ] lookupDays() xử lý đúng như doc đã ghi (chuẩn hoá case trước khi so khớp), hoặc sửa lại doc cho khớp code — chọn 1 và làm nhất quán
- [ ] Test nhập key với case khác nhau đều nhận đúng
