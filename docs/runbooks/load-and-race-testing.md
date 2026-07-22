# Runbook: load and race-condition testing

Two scripts in `scripts/loadtest/` exercise the concurrency guarantees the
plan calls out explicitly (concurrent rooms, stale-move races, WebSocket/API
longevity under load). Run both against staging - never production - before
trusting a change that touches room lifecycle, persistence, or auth.

## Race-condition check

A dependency-free Node script (built-in `fetch`, Node >= 18) that fires
genuinely concurrent requests at a running backend and asserts the API
contract holds under contention: double-start doesn't double-deal,
concurrent reads are consistent, per-player views don't leak opponent
state, and duplicate move submissions are handled consistently.

```bash
node scripts/loadtest/race-condition-check.mjs https://staging-api.example.com
```

Exits non-zero (with a `[FAIL]` line for each failing check) if anything
regressed. This is intentionally light on setup - it doesn't need a browser
or Playwright - so it's cheap to run locally against `quarkus:dev` too:

```bash
node scripts/loadtest/race-condition-check.mjs http://localhost:8080
```

## Load test

A [k6](https://k6.io) script ramping up to 20 concurrent virtual users
creating/joining rooms and reading state, plus a steady background of
health-check polling, with thresholds asserting p95 latency stays under
500ms and the error rate stays under 1%.

```bash
k6 run -e BASE_URL=https://staging-api.example.com scripts/loadtest/k6-room-smoke.js
```

Interpreting results:

- If `http_req_duration` thresholds fail, check whether it's CPU/memory
  bound (CloudWatch `<prefix>-task-cpu-high`/`-task-memory-high` alarms,
  `infra/modules/backend-service` sizing) or DynamoDB throttling (on-demand
  tables auto-scale but not instantaneously under a sudden spike).
- If `http_req_failed` exceeds 1%, check the ALB's `HTTPCode_Target_5XX_Count`
  metric and the backend's CloudWatch Logs for the failing requests'
  `X-Request-Id`s.
- 20 concurrent VUs is a deliberately modest ceiling for a private 2-3
  player game - this validates "doesn't fall over under a burst of
  simultaneous invites," not high-traffic scale. Raise `stages` if you have
  a specific concurrency target to validate against.

Both scripts create real (if TTL-expiring) rooms in whichever environment
you point them at - don't run them against production while anyone might
have a game in progress.
