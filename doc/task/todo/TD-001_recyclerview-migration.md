# TD-001 — Migrate ListView/ArrayAdapter sang RecyclerView + DiffUtil

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P1 |
| Điểm | 13 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Core hex/text view vẫn dùng ListView/ArrayAdapter legacy (AdtRecentlyOpenRecycler và AdtHashResult đã dùng RecyclerView cho các màn hình mới hơn, nhưng màn hình chính chưa migrate). Đây là điều kiện tiên quyết để làm mượt highlight/diff cho T-002 (File Diff) và T-004 (Pattern Search).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/AdtHexTextArray.java`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/AdtSearchableListArray.java`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/AdtPlainTextListArray.java`

## Acceptance Criteria

- [ ] Hex view + Plain text view chạy trên RecyclerView với DiffUtil thay vì notifyDataSetChanged() toàn bộ
- [ ] Multi-select (GenericMultiChoiceCallback) hoạt động đúng tương đương bản cũ sau migrate
- [ ] Không regression về hiệu năng scroll trên file lớn (benchmark trước/sau)
- [ ] T-002/T-004 có thể build trên nền RecyclerView này mà không cần migrate lại
