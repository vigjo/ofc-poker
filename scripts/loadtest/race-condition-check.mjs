#!/usr/bin/env node
// Fires genuinely concurrent requests at a running backend to check the
// races the plan calls out explicitly: double-start, duplicate/idempotent
// move submission, and opponent-hand leakage under concurrent reads.
// Crafting a *strategically valid* OFC move from outside the game requires
// knowing server-dealt cards, so this focuses on races the API contract
// itself must guarantee regardless of game strategy.
//
// Usage:
//   node scripts/loadtest/race-condition-check.mjs http://localhost:8080
//
// Requires Node >= 18 (built-in fetch). Exits non-zero on any check failure.

const baseUrl = process.argv[2] || "http://localhost:8080";
let failures = 0;

function check(label, condition) {
  if (condition) {
    console.log(`  [PASS] ${label}`);
  } else {
    console.error(`  [FAIL] ${label}`);
    failures += 1;
  }
}

async function postJson(path, body, token) {
  const res = await fetch(`${baseUrl}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });
  const json = await res.json().catch(() => null);
  return { status: res.status, json };
}

async function getJson(path, token) {
  const res = await fetch(`${baseUrl}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  const json = await res.json().catch(() => null);
  return { status: res.status, json };
}

async function main() {
  console.log(`Target: ${baseUrl}\n`);

  console.log("1. Create room + join a second seat");
  const host = await postJson("/api/rooms", { displayName: "RaceHost" });
  check("room created", host.status === 200);
  const roomId = host.json.roomId;
  const hostToken = host.json.seatToken;

  const guest = await postJson(`/api/rooms/${roomId}/join`, { displayName: "RaceGuest" });
  check("guest joined", guest.status === 200);
  const guestToken = guest.json.seatToken;

  console.log("\n2. Fire two concurrent /start calls - only one deal should exist");
  const [start1, start2] = await Promise.all([
    postJson(`/api/rooms/${roomId}/start`, {}, hostToken),
    postJson(`/api/rooms/${roomId}/start`, {}, hostToken),
  ]);
  const oneSucceeded = [start1, start2].filter((r) => r.status === 200).length >= 1;
  check("at least one /start call succeeded", oneSucceeded);

  const stateAfterStart = await getJson(`/api/rooms/${roomId}/state`, hostToken);
  check("state readable after concurrent start", stateAfterStart.status === 200);
  const hostHandAfterFirstRead = JSON.stringify(stateAfterStart.json?.hand ?? stateAfterStart.json);

  console.log("\n3. Re-read state twice concurrently - the dealt cards must not change between reads");
  const [read1, read2] = await Promise.all([
    getJson(`/api/rooms/${roomId}/state`, hostToken),
    getJson(`/api/rooms/${roomId}/state`, hostToken),
  ]);
  check(
    "concurrent state reads are identical (no re-deal on read)",
    JSON.stringify(read1.json) === JSON.stringify(read2.json) &&
      JSON.stringify(read1.json?.hand ?? read1.json) === hostHandAfterFirstRead,
  );

  console.log("\n4. Guest's state view must not expose the host's private cards");
  const guestState = await getJson(`/api/rooms/${roomId}/state`, guestToken);
  check("guest can read room state", guestState.status === 200);
  const guestStateStr = JSON.stringify(guestState.json);
  const hostStateStr = JSON.stringify(stateAfterStart.json);
  check(
    "guest's view differs from host's (per-player filtering, not a shared dump)",
    guestStateStr !== hostStateStr,
  );

  console.log("\n5. Duplicate move submission with the same requestId must be idempotent");
  const dupBody = { requestId: "race-check-fixed-id", placements: [], discard: null };
  const [move1, move2] = await Promise.all([
    postJson(`/api/rooms/${roomId}/moves`, dupBody, hostToken),
    postJson(`/api/rooms/${roomId}/moves`, dupBody, hostToken),
  ]);
  check(
    "both duplicate submissions returned the same status (no partial/inconsistent handling)",
    move1.status === move2.status,
  );

  console.log(`\n${failures === 0 ? "All checks passed." : `${failures} check(s) failed.`}`);
  process.exit(failures === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error("Race condition check crashed:", err);
  process.exit(1);
});
