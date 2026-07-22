# 0004. Launch with a single Fargate task, not a HA cluster

## Status

Accepted

## Context

The product is cost-conscious ("pragmatic" deployment style) private
multiplayer for at most 3 people per room. A highly-available multi-task
deployment would need either sticky WebSocket routing or a pub/sub layer
(e.g. Redis) to fan state changes out to every task, adding real monthly
cost and operational surface for a product with modest expected concurrency
at launch.

## Decision

Launch with exactly one ECS Fargate task running the Quarkus API behind an
ALB. All mutable game state lives in DynamoDB (see ADR 0002), not in
process memory that only that task can see, so:

- A task restart/redeploy loses no game state - `RoomRegistry` rehydrates
  any room from its last DynamoDB snapshot on the next request.
- The only availability gap is the time between the old task stopping and
  the new one passing its health check during a deploy or crash restart -
  acceptable for a non-HA launch runtime, and short with Fargate.

## Consequences

- The app is explicitly **not** highly available at launch: a task crash
  causes a brief outage (seconds) for every open room until ECS replaces it.
- No Redis/pub-sub cost or operational surface is needed yet.
- This is designed to be additive, not a rewrite, when traffic justifies
  it: add a second task and a Redis (or DynamoDB Streams-based) fan-out for
  WebSocket broadcasts so every task can push updates regardless of which
  task a given client is connected to. Nothing in the domain or persistence
  layers needs to change.
