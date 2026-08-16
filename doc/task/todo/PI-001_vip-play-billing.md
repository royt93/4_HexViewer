# PI-001 — Thay VIP key hardcode bằng Google Play Billing

| Trường | Giá trị |
|---|---|
| Loại | Product Idea |
| Ưu tiên | — |
| Điểm | — |
| Trạng thái | todo |
| Độ tin cậy | 4/4 — đồng thuận tuyệt đối |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Không chỉ là fix bug (BUG-003) mà là fix lại toàn bộ mô hình kinh doanh. Hiện tại build.gradle chưa hề có billing library nào — toàn bộ VIP dựa vào key hardcode + xem ad.

LƯU Ý: user đã quyết định CHƯA xử lý ngay, ưu tiên việc khác trước.

## File liên quan

- `app/build.gradle (hiện chưa có billing library nào)`
- `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/`

## Acceptance Criteria

- [ ] Ý tưởng sản phẩm — cần lên task riêng (epic) với estimate cụ thể khi được pick vào sprint
