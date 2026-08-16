# Release readiness

## 🟡 In progress

- Compare signing certificate SHA-256 with Play Console and rotate the compromised credential.

## ✅ Implemented

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

## 📋 Picked

- Rotate the compromised signing credential after certificate fingerprint comparison in Play Console.

## ⏸️ Deferred

- Git history rewrite; user selected key rotation without rewriting the public remote.

## ❌ Skipped

- Store upload and rollout automation.

## 💭 Ideas

- Add CI secret scanning and release-certificate verification.
