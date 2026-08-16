# TASK OVERVIEW — HexViewer Feature Roadmap

> **Cập nhật:** 2026-08-16 | **Trạng thái:** Lên plan, chưa code

> ⚠️ **Xem [BACKLOG.md](BACKLOG.md) trước khi bắt đầu code bất kỳ task nào bên dưới.** Backlog đó liệt kê 3 bug P0 gây mất dữ liệu user + vô hiệu hoá VIP, cần fix ở Sprint 0 trước khi build feature mới lên trên.

## Tổng quan

| ID | Feature | Status | Effort | Priority | VIP Gate |
|----|---------|--------|--------|----------|---------|
| [T-001](TASK_001_DATA_INSPECTOR.md) | Data Inspector Panel | `todo` | M (3–5 ngày) | P0 ⭐ | Partial |
| [T-002](TASK_002_FILE_DIFF.md) | File Diff / Compare 2 file | `todo` | L (7–10 ngày) | P1 | Full VIP |
| [T-003](TASK_003_BOOKMARKS.md) | Bookmarks & Jump Navigation | `todo` | S (2–3 ngày) | P2 | Free (limit 5) |
| [T-004](TASK_004_PATTERN_SEARCH.md) | Advanced Pattern Search | `todo` | M (4–6 ngày) | P1 | Partial |

## Legend

| Status | Nghĩa |
|--------|-------|
| `todo` | Chưa bắt đầu |
| `inprogress` | Đang làm |
| `done` | Hoàn thành |

## Thứ tự implement đề xuất

```
T-003 (Bookmarks) → T-001 (Data Inspector) → T-004 (Pattern Search) → T-002 (File Diff)
  2-3 ngày              3-5 ngày                  4-6 ngày                7-10 ngày
  Quick win             Core value               Power user               Killer feature
```

> **Lý do thứ tự:** Bookmarks ít code nhất, ship sớm → tăng user engagement.
> Data Inspector là core value nhất → giữ user. Pattern Search + Diff là power features → upsell VIP.
