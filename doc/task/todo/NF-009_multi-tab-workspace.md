# NF-009 — Multi-tab / mở nhiều file cùng lúc

| Trường | Giá trị |
|---|---|
| Loại | New Feature |
| Ưu tiên | P3 |
| Điểm | 13 |
| Trạng thái | todo |
| Độ tin cậy | 2/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Hiện chỉ hỗ trợ 1 file tại 1 thời điểm; mở file mới đóng file hiện tại và mất context vị trí đang xem. Nặng vì đụng thẳng kiến trúc ActMain — nên làm SAU TD-003 (tách ViewModel).

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/ActMain.java`
- `app/src/main/java/com/galaxyjoy/hexviewer/models/FileData.java`

## Acceptance Criteria

- [ ] Mở được 2-5 file cùng lúc, chuyển tab giữ nguyên vị trí/selection/undo history mỗi tab
- [ ] Không tăng đáng kể memory footprint so với hiện tại khi chỉ có 1 tab active
