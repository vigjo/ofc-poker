# 0002. Whole-room snapshot in a single DynamoDB item, not event sourcing

## Status

Accepted

## Context

Room/game state (`Room`, `PineappleHand`, per-player boards, running totals)
must survive an API task restart and support optimistic concurrency so two
racing requests for the same room can't corrupt state. An event-sourced
design (append-only move log + periodic snapshots) was considered, since the
domain already models discrete moves.

## Decision

Persist each room as **one DynamoDB item** (`RoomItem`) keyed by `roomId`,
containing the room's status/version as first-class attributes plus the
entire `RoomSnapshot` (seats, current hand, boards, running totals,
fantasyland carry-over) serialized as a single JSON blob (`stateJson`).

Every accepted mutation increments `Room.version()` and is written with a
DynamoDB conditional expression (`version = :expectedPreviousVersion`, or
`attribute_not_exists(roomId)` for the first write), giving exactly the
"one accepted move per game version" guarantee the plan requires, without a
separate events table (`RoomRepository#save`).

In-memory recent events (`Room#eventsSince`) exist only to help a
still-connected client fast-forward without a full state refetch; they are
capped and never persisted. After a real process restart, a reconnecting
client always gets a full snapshot instead of replayed history - simpler,
and no more expensive than a real game in progress on a 2-3 player table.

## Consequences

- No audit trail of every historical move survives an API restart - out of
  scope for launch, and easy to add later as a separate append-only table if
  needed without changing the room aggregate's shape.
- Reads/writes are always whole-room, which is fine at 2-3 players' worth of
  state per item and keeps `RoomRepository` and `RoomRegistry` simple.
- `RoomItem`'s DynamoDB Enhanced Client table schema is built explicitly with
  `StaticTableSchema` rather than `TableSchema.fromBean(...)`: the latter's
  runtime bean-reflection path does not reliably resolve application classes
  across Quarkus's layered dev/test class loaders (observed as a spurious
  `ClassNotFoundException` for the item class itself during `@QuarkusTest`
  runs). The explicit schema is also reflection-free and marginally faster.
