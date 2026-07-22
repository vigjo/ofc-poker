# 0001. Rewrite on Quarkus (Java 25) + React/TypeScript

## Status

Accepted

## Context

The repository started as a pre-alpha Python/Flask + unbuilt React (JSX)
spike (preserved under `legacy-python-prototype/`). It had no working
dependency manifests, tests, persistence, or infrastructure, and its rules
engine was incomplete. The goal is a production-style, cost-conscious AWS
deployment of a private, invite-only 2-3 player Pineapple OFC game.

## Decision

Rewrite from scratch:

- **Backend**: Java 25 on Amazon Corretto, Quarkus (current LTS), with a
  pure domain module (`poker.ofc.domain`) isolated from transport and
  storage concerns, so the rules engine can be tested without a running
  server or database.
- **Frontend**: React + TypeScript + Vite, with API types generated from
  the backend's OpenAPI contract rather than duplicated by hand.

Java/Quarkus was chosen over continuing in Python for stronger static
typing across a fairly intricate rules engine, first-class virtual threads
and structured concurrency on modern JDKs, and Quarkus's low-overhead
container startup, which matters for a cost-conscious single-task Fargate
deployment. Corretto 25 (not an LTS-only conservative choice, but not a
preview-feature release either) keeps the JDK current without relying on
preview APIs in production code.

## Consequences

- The old prototype is reference-only; none of its code is reused directly,
  though its rules intent and `src/svg-cards.svg` asset inform the rewrite.
- Two toolchains (Maven + npm) must be kept in CI and local dev instead of
  one, in exchange for stronger guarantees in each layer.
