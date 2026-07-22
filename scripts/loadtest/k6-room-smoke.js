// Load/smoke test for room creation and basic API responsiveness under
// concurrency. Run with k6 (https://k6.io) against a real deployed
// environment - never against production without warning whoever might be
// mid-game, since it creates real (if TTL-expiring) rooms.
//
// Usage:
//   k6 run -e BASE_URL=https://staging-api.example.com scripts/loadtest/k6-room-smoke.js
//
// See docs/runbooks/load-and-race-testing.md for interpretation and the
// companion race-condition script.
import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    concurrent_room_creation: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "30s", target: 20 },
        { duration: "1m", target: 20 },
        { duration: "15s", target: 0 },
      ],
      exec: "createAndJoinRoom",
    },
    steady_health_checks: {
      executor: "constant-vus",
      vus: 2,
      duration: "1m45s",
      exec: "healthCheck",
    },
  },
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
  },
};

export function createAndJoinRoom() {
  const createRes = http.post(
    `${BASE_URL}/api/rooms`,
    JSON.stringify({ displayName: `LoadTestHost-${__VU}-${__ITER}` }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(createRes, {
    "create room succeeds": (r) => r.status === 200,
    "create room returns invite code": (r) => !!r.json("inviteCode"),
  });
  if (createRes.status !== 200) {
    return;
  }

  const roomId = createRes.json("roomId");
  const joinRes = http.post(
    `${BASE_URL}/api/rooms/${roomId}/join`,
    JSON.stringify({ displayName: `LoadTestGuest-${__VU}-${__ITER}` }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(joinRes, { "join room succeeds": (r) => r.status === 200 });

  const token = createRes.json("seatToken");
  const stateRes = http.get(`${BASE_URL}/api/rooms/${roomId}/state`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  check(stateRes, { "read state succeeds": (r) => r.status === 200 });

  sleep(1);
}

export function healthCheck() {
  const res = http.get(`${BASE_URL}/health/ready`);
  check(res, { "readiness is up": (r) => r.status === 200 });
  sleep(2);
}
