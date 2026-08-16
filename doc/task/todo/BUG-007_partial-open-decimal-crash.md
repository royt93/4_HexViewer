# BUG-007 — Nhập offset dạng thập phân trong Partial Open bị mất input

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P1 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

convertValueTo() tính đúng giá trị qua convert(val, null) khi input có dấu chấm thập phân, nhưng sau đó lại gọi Long.parseLong(val) trên CHUỖI GỐC (vd "1.5") thay vì giá trị đã convert — NumberFormatException bị nuốt, trả về rỗng, input của user biến mất không có thông báo lỗi nào.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActPartialOpen.java:462-477`

## Acceptance Criteria

- [ ] Nhập offset dạng thập phân (vd "1.5" MB) phải convert đúng ra giá trị byte tương ứng, không mất input
- [ ] Nếu input thực sự không hợp lệ, phải hiện thông báo lỗi rõ ràng thay vì âm thầm trả về rỗng
- [ ] Test case cho input có dấu chấm ở mọi đơn vị (KB/MB/GB)
