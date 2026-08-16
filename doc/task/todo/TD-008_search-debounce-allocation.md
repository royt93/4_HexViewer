# TD-008 — Search không debounce, clone toàn bộ list mỗi keystroke

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

EntryFilter.apply()/SearchableFilterFactory.multilineSearch chạy background nên không treo UI, nhưng tốn GC vì clone/allocate mỗi ký tự gõ. Cấu trúc dữ liệu hiện tại cũng không mở rộng tốt cho wildcard/regex — cần cho T-004 Pattern Search.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/search/EntryFilter.java:55-71`
- `app/src/main/java/com/galaxyjoy/hexviewer/ui/adt/search/SearchableFilterFactory.java:154-201`

## Acceptance Criteria

- [ ] Search debounce ~300ms trước khi chạy filter thật
- [ ] Không allocate full-list clone mỗi keystroke
- [ ] Cấu trúc đủ linh hoạt để T-004 (wildcard/regex) build lên trên mà không cần viết lại
