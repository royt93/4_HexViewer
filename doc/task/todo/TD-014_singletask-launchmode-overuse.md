# TD-014 — launchMode singleTask áp cho gần như mọi Activity

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P3 |
| Điểm | 2 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

singleTask chỉ nên dùng cho task root (thường chỉ launcher Activity), áp dụng tràn lan là nguồn tiềm ẩn bug back-stack.

## File liên quan

- `app/src/main/AndroidManifest.xml (nhiều dòng: 45, 60, 143, 153, 163, 173, 183, 193, 203, 218, 222, 233)`

## Acceptance Criteria

- [ ] Review lại từng Activity, chỉ giữ singleTask ở nơi thực sự cần (launcher/entry point)
- [ ] Test kỹ luồng back-stack sau khi đổi launchMode để không phá vỡ navigation hiện tại
