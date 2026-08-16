# NF-012 — Hash theo vùng chọn, không chỉ whole-file

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P2 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

ActHashCalculator/TaskHash hiện chỉ hash whole-file. Mở rộng để hash được 1 vùng byte đã chọn — hữu ích để verify từng phần của file (vd 1 section trong firmware).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActHashCalculator.java`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskHash.java`

## Acceptance Criteria

- [ ] Chọn vùng byte → tính hash (các thuật toán hiện có) chỉ trên vùng đó
- [ ] Kết quả hiển thị rõ đây là hash của selection, không nhầm với whole-file
