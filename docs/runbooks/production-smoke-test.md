# Runbook: production/staging smoke test

Run this manually against staging before every promotion to production, and
against production immediately after every production deploy. Budget about
10 minutes.

## 1. Frontend loads

- [ ] `https://<frontend_domain>` loads over HTTPS with a valid certificate
      (no browser warning).
- [ ] Browser console has no errors on first load.
- [ ] `https://<api_domain>/health/ready` and `/health/live` both return
      `200` with `{"status":"UP", ...}`.

## 2. Create and join a room (two browser contexts / one incognito)

- [ ] Create a room as "Host" - an invite code/link appears.
- [ ] Open the invite link in a second browser context as "Guest" - Guest
      appears in the lobby's player list for Host, and vice versa.
- [ ] Host starts the game once 2+ players have joined; both clients
      transition to the active board.

## 3. Play a full hand

- [ ] Each player can place all five initial cards via click/tap.
- [ ] Each of the four three-card streets deals correctly, and discards work.
- [ ] Illegal placements (full row, already-locked row) are rejected with a
      clear error, not a crash.
- [ ] After the last street, both players see the hand-complete scoring view
      with correct row-by-row results and a scoop/fantasyland indicator
      where applicable.

## 4. Reconnect recovery

- [ ] Mid-hand, refresh Guest's browser tab. Guest reconnects and sees the
      current board state (not a blank/errored page), and Host is
      unaffected.
- [ ] Close Guest's tab entirely for ~10 seconds, then reopen the invite
      link. Guest rejoins the same seat with correct state (within the
      configured disconnect grace period).

## 5. Observability sanity check

- [ ] The request(s) you just made appear in CloudWatch Logs
      (`/ecs/<prefix>-backend`), each with an `X-Request-Id` field.
- [ ] `/metrics` on the API returns Prometheus-format output.
- [ ] No new CloudWatch alarms are in `ALARM` state
      (`<prefix>-unhealthy-hosts`, `<prefix>-alb-5xx`, `<prefix>-task-cpu-high`,
      `<prefix>-task-memory-high`).

## 6. Task restart resilience (staging only - don't do this in prod on a whim)

- [ ] Start a game, then force a new ECS deployment:
      `aws ecs update-service --cluster <cluster> --service <service> --force-new-deployment`.
- [ ] Confirm the in-progress room's state survives the restart (reload the
      page after the new task passes health checks) - this is the core
      guarantee of ADR 0002/0004.

If any step fails, stop the promotion and open an issue with the failing
step, the room ID (if applicable), and the `X-Request-Id` from the browser's
network tab before debugging further.
