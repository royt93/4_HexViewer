# NF-004 — Binary template parser kiểu 010 Editor

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P3 |
| Điểm | 13 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

User tự định nghĩa struct (tên field, kiểu, offset) để auto-annotate vùng byte tương ứng. Xây trên nền NF-011 + T-001 Data Inspector — nên làm sau cả 2.

## File liên quan

- `(feature mới hoàn toàn, chưa có code liên quan)`

## Acceptance Criteria

- [ ] User định nghĩa được 1 struct đơn giản (field name + type + length) và thấy annotate trực tiếp trên hex view
- [ ] Có sẵn vài template mẫu cho định dạng phổ biến (dùng chung dữ liệu với NF-011)
