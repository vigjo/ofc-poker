# 0003. Room-scoped guest JWTs instead of user accounts

## Status

Accepted

## Context

The product is explicitly private and invite-only: no public lobby, no
durable accounts, no real-money features. Players just need a way to prove
"I am seat X in room Y" across REST calls and the WebSocket connection,
including after a refresh or brief disconnect.

## Decision

- Creating or joining a room returns a short-lived (`ofc.auth.token-ttl-minutes`,
  default 180) RS256-signed JWT (`GuestTokenService`) scoped to one
  `roomId` + `playerId` + `displayName`, with no password or account
  behind it.
- The client persists this token (and roomId/playerId) client-side and
  sends it as a `Bearer` token on REST calls and as a `token` query
  parameter on the WebSocket upgrade (`GameSocket`).
- Every server operation re-validates the token's `roomId` claim against the
  room in the URL, so a token issued for one room can never authorize
  actions in another.

## Consequences

- Losing the token (e.g. clearing browser storage) means losing that seat -
  acceptable for a casual private-game product, and mitigated by a
  reasonably long TTL.
- No password reset, multi-device sign-in, or cross-room identity exists by
  design; a returning player who wants a persistent identity across rooms is
  explicitly out of scope for launch.
- Dev/test use a checked-in throwaway RSA keypair
  (`privateKey.pem`/`publicKey.pem`); staging/production load a real keypair
  from AWS Secrets Manager/SSM via `OFC_JWT_PRIVATE_KEY_PATH` /
  `OFC_JWT_PUBLIC_KEY_PATH` (see `.env.example` and
  `docs/runbooks/secrets-rotation.md`).
