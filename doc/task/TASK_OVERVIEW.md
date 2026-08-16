# TASK OVERVIEW — HexViewer Feature Roadmap

> **Cập nhật:** 2026-08-16 | **Trạng thái:** Lên plan, chưa code

> ⚠️ **Xem [BACKLOG.md](BACKLOG.md) trước khi bắt đầu code bất kỳ task nào bên dưới.** Backlog đó liệt kê 2 bug P0 gây mất dữ liệu user cần fix ở Sprint 0 trước khi build feature mới lên trên.
>
> **Cấu trúc thư mục `doc/task/` (scrum board):**
> - `todo/` — chưa bắt đầu (hiện tại: toàn bộ 4 task dưới đây + 62 task card từ BACKLOG.md)
> - `inprogress/` — đang code
> - `done/` — đã merge xong
>
> Khi bắt đầu 1 task, di chuyển file `.md` từ `todo/` → `inprogress/`; khi xong, chuyển tiếp sang `done/` và cập nhật bảng bên dưới.

## Tổng quan

| ID | Feature | Status | Effort | Priority | VIP Gate |
|----|---------|--------|--------|----------|---------|
| [T-001](todo/TASK_001_DATA_INSPECTOR.md) | Data Inspector Panel | `todo` | M (3–5 ngày) | P0 ⭐ | Partial |
| [T-002](todo/TASK_002_FILE_DIFF.md) | File Diff / Compare 2 file | `todo` | L (7–10 ngày) | P1 | Full VIP |
| [T-003](todo/TASK_003_BOOKMARKS.md) | Bookmarks & Jump Navigation | `todo` | S (2–3 ngày) | P2 | Free (limit 5) |
| [T-004](todo/TASK_004_PATTERN_SEARCH.md) | Advanced Pattern Search | `todo` | M (4–6 ngày) | P1 | Partial |

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
