# TD-011 — Chưa có CI pipeline chạy test tự động

| Trường | Giá trị |
|---|---|
| Loại | Tech Debt |
| Ưu tiên | P2 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 1/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

Test coverage khá ổn (23 unit test file, 9 instrumented, theo doc/test/FULL_TEST_PLAN.md) nhưng không có gì chặn 1 build hỏng hoặc test fail được merge. Nên làm trước khi 4 feature lớn (T-001..T-004) đổ vào để tránh regression âm thầm.

## File liên quan

- `(không có file .github/workflows nào trong repo — đã verify)`

## Acceptance Criteria

- [ ] Có GitHub Actions workflow chạy ./gradlew test (và lint nếu có) trên mỗi PR
- [ ] PR không thể merge nếu test fail (branch protection hoặc ít nhất cảnh báo rõ ràng)
