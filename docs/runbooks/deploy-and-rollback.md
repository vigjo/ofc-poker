# Runbook: deploying and rolling back

## Normal flow

1. Open a PR touching `backend/`, `frontend/`, or `infra/`. `backend-ci.yml`,
   `frontend-ci.yml`, and `terraform-ci.yml` run automatically; the Terraform
   plan is posted as a PR comment for review.
2. Merge to `main`. `deploy-staging.yml` builds the backend image, pushes it
   to ECR tagged with the commit SHA, runs `terraform apply` for the staging
   environment (which rolls the ECS service to the new task definition),
   then builds and publishes the frontend against staging's just-applied API
   URL.
3. Smoke-test staging by hand (create a room, invite a second browser/
   incognito window, play a hand, refresh mid-hand). See
   `docs/runbooks/production-smoke-test.md` for the full checklist - run it
   against staging too before promoting.
4. Promote to production by publishing a GitHub release (tag `vX.Y.Z`) -
   `deploy-production.yml` deploys the **same image digest** already
   validated in staging (it verifies the tag exists in ECR before applying,
   and defaults to `github.sha` of the release commit). The `production`
   GitHub Environment's required-reviewers rule pauses the run for approval.
5. Repeat the smoke test against production.

## Rolling back

Because staging and production always deploy an explicit, immutable
(`image_tag_mutability = IMMUTABLE`) image tag rather than a moving
`latest`, rollback is just "deploy the previous good tag again":

```bash
# Find the previous good SHA (e.g. from git log or the prior release tag).
cd infra/envs/production   # or staging
terraform apply -var="backend_image_tag=<previous-sha>"
```

Or, without touching Terraform state directly, re-run
`deploy-production.yml` via `workflow_dispatch` with `image_tag` set to the
previous SHA/release tag - the workflow verifies that tag exists in ECR
before applying.

The frontend has no separate rollback path: it is rebuilt from source
against the current API URL on every deploy. To roll it back, redeploy from
the previous commit (`git revert`, or `workflow_dispatch` on the previous
commit via the Actions UI's "re-run" on an old workflow run for the same
branch state).

Database state (DynamoDB) is not versioned per-deploy; rolling back the
application code does not roll back game state. See
`docs/runbooks/restore-drill.md` for point-in-time recovery in production.

## Diagnosing a bad deploy

- ECS deployment circuit breaker (`infra/modules/backend-service`) will
  auto-rollback a task definition that never passes its health check within
  the deployment window - check the ECS service's "Deployments" tab for
  `ROLLBACK_IN_PROGRESS`/`ROLLBACK_COMPLETED`.
- CloudWatch alarms `<prefix>-unhealthy-hosts` and `<prefix>-alb-5xx` fire to
  the SNS topic (and your subscribed email) on sustained failures.
- Logs: CloudWatch Logs group `/ecs/<prefix>-backend`, filterable by the
  `X-Request-Id` correlation ID from any bug report (see
  `CorrelationIdFilter`).
