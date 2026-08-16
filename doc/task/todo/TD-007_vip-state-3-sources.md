# TD-007 — VIP state nằm rải rác 3 nơi phải tự đồng bộ tay

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 5 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Flag/expiry nội bộ AdManager, VipPrefs SharedPreferences, và cờ first-init trong AppPreferences phải tự đồng bộ tay — lệch 1 chỗ là hiện sai trạng thái VIP cho user.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/ActVipManagement.kt:274-284`
- `app/src/main/java/com/galaxyjoy/hexviewer/feature/vip/VipPrefs.kt`

## Acceptance Criteria

- [ ] Gộp về 1 nguồn sự thật (single source of truth) cho trạng thái VIP
- [ ] Không còn chỗ nào phải tự check "cả 3 đồng ý" để suy ra trạng thái VIP
