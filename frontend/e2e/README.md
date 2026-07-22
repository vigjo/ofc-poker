# End-to-end tests

These Playwright specs drive the real, rendered UI against a real backend -
no mocked network calls. They assume the following are already running
(see the repo root `README.md` for the full local dev setup):

1. DynamoDB Local: `docker compose up -d dynamodb-local` (from the repo root).
2. The Quarkus backend on `http://localhost:8080`: `./mvnw quarkus:dev` (from
   `backend/`).

Then, from `frontend/`:

```bash
npm run build   # produces dist/, served by the Playwright webServer
npm run e2e
```

`playwright.config.ts` builds/serves the production bundle with
`vite preview` on port 4173 and points the app at the backend's default
`http://localhost:8080`. CI runs the same commands after starting DynamoDB
Local and the packaged backend jar (see
`.github/workflows/frontend-ci.yml`).

## What's covered

- `full-game.spec.ts` - two players create/join a room, the host starts the
  game, and both play a complete 13-card Pineapple hand to a scored result.
- `errors-and-reconnect.spec.ts` - joining an unknown invite code surfaces an
  error, the confirm button can't be forced before a legal move is drafted,
  and a mid-game page refresh reconnects to the same seat via the persisted
  seat token instead of re-prompting for a display name.
