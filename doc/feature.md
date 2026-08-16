# Release readiness

## 🟡 In progress

- Compare signing certificate SHA-256 with Play Console and rotate the compromised credential.

## ✅ Implemented

- Transparent large-file streaming: normal Open automatically uses a bounded 128 KiB
  resident window above 30 MiB, with 256 KiB pages and an 8 MiB LRU cache.
- Large-file navigation and safety: continuous window switching, 64-bit Go To,
  whole-file streaming search, non-seekable URI spool fallback, fixed-length edits,
  transactional Save As staging/rollback, and cancellation that never deletes the source file.
- Unknown-size cloud/pipe sources are resolved through bounded disk spooling and reused
  across window/search requests; recent files retain streaming mode without persisting a
  transient window.
- Save safety regression fixes: fixed-length resident validation, full-write loops,
  in-place rollback, and exact final-block reads for legacy sequential windows.
- Search parity and lifecycle: spaced hex queries remain byte searches, plain searches
  retain case-insensitive UTF-8 behavior, clear cancels stale results, and hex/plain views
  preserve the absolute window anchor.
- Streaming verification: unit coverage for policy/cache/ranges/search/edit/save and
  on-device widget/integration coverage for real, sparse, seekable and pipe-backed files.
- UMP consent accept/reject smoke on S24 Ultra.
- Manual banner lifecycle and stale-banner recovery.
- Async splash coordinator with exactly-once callback guards.
- Release-critical code/security audit: 331 unit tests, lint, R8 and signed AAB pass.
- Debug-only UMP values isolated from release BuildConfig.
- Portable local/CI signing configuration with no developer-specific fallback.
- Tracked signing password redacted from the current tree.
- Ad/VIP instrumentation: 2/2 pass on Samsung S24 Ultra.
- Full instrumentation: 35/35 pass on Samsung S24 Ultra after stabilizing the
  ActLineUpdate IME/result-contract test.
- Current large-file release gate: 400 unit tests, zero-error fail-fast lint, R8,
  signed APK/AAB, 58/58 connected tests on Pixel 7 Pro (including transactional
  Save As rollback/cleanup), and release cold-start smoke all pass (2026-08-16).

## 📋 Picked

- Rotate the compromised signing credential after certificate fingerprint comparison in Play Console.

## ⏸️ Deferred

- Git history rewrite; user selected key rotation without rewriting the public remote.

## ❌ Skipped

- Store upload and rollout automation.

## 💭 Ideas

- Add CI secret scanning and release-certificate verification.
