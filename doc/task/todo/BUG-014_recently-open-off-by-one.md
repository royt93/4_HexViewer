# BUG-014 — Off-by-one bounds check trong AdtRecentlyOpenRecycler

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P2 |
| Điểm | 1 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Check bound dùng idx > size() thay vì >=; race với swipe-to-delete có thể trúng đúng idx == size() → IndexOutOfBoundsException.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/AdtRecentlyOpenRecycler.java:99-101, ~203-205`

## Acceptance Criteria

- [ ] Sửa điều kiện bound check thành >= size()
- [ ] Test swipe-to-delete liên tiếp nhanh nhiều item để tái hiện race condition
