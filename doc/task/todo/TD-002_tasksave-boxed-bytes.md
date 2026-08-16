# TD-002 — TaskSave box toàn bộ file thành List<Byte> trước khi ghi

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P1 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 4/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

bytes.addAll(entry.getRaw()) box từng byte một → hàng triệu object boxed với file lớn, không có MemoryMonitor guard nào (khác với TaskOpen đã có). Nguy cơ OOM khi save file lớn cao hơn khi open file tương đương.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/task/TaskSave.java:151-154`

## Acceptance Criteria

- [ ] Ghi thẳng byte[]/NIO FileChannel theo chunk, không tạo List<Byte> trung gian
- [ ] Thêm MemoryMonitor guard tương tự TaskOpen cho path save
- [ ] Test save file gần ngưỡng kích thước tối đa, theo dõi peak heap trước/sau
