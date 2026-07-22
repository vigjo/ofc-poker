# Runbook: bootstrapping a new AWS account for OFC Poker

One-time setup to go from a bare AWS account to a state where GitHub Actions
can plan and deploy both environments via OIDC - no long-lived AWS access
keys are used anywhere after this. See ADR 0005 for the design rationale.

## Prerequisites

- An AWS account you have administrator access to (used only for this
  one-time bootstrap; every later apply runs from GitHub Actions).
- A domain you control, with a **Route 53 hosted zone already created** for
  its apex (e.g. `example.com`). If you registered the domain elsewhere,
  delegate its nameservers to that Route 53 zone first - ACM DNS validation
  and the `app./api.` records both depend on it.
- Terraform >= 1.9, AWS CLI v2, and `git` installed locally.
- The GitHub repository already exists (this runbook assumes
  `github.com/<org>/ofc-poker`).

## 1. Apply the bootstrap module

This creates the shared Terraform state bucket/lock table, the ECR
repository, the GitHub OIDC provider, and the four IAM roles CI assumes.

```bash
cd infra/bootstrap
terraform init
terraform apply \
  -var="github_org=<your-github-org-or-username>" \
  -var="github_repo=ofc-poker"
```

Record the outputs - you'll need all of them:

```bash
terraform output
```

## 2. Point the environment backends at the new state bucket

Backend blocks can't reference variables, so this is a manual one-time edit.
In both `infra/envs/staging/backend.tf` and `infra/envs/production/backend.tf`,
replace:

```
bucket = "REPLACE_WITH_BOOTSTRAP_OUTPUT_terraform_state_bucket"
```

with the literal value of `terraform output -raw terraform_state_bucket`
from step 1, then commit that change.

## 3. Configure GitHub repository secrets and variables

From the bootstrap outputs:

| GitHub setting | Scope | Value |
|---|---|---|
| Secret `AWS_CI_PUSH_ROLE_ARN` | Repository | `ci_push_role_arn` |
| Secret `AWS_TERRAFORM_PLAN_ROLE_ARN` | Repository | `terraform_plan_role_arn` |
| Secret `AWS_DEPLOY_ROLE_ARN` | Environment `staging` | `deploy_role_arns["staging"]` |
| Secret `AWS_DEPLOY_ROLE_ARN` | Environment `production` | `deploy_role_arns["production"]` |
| Variable `AWS_REGION` | Repository | `us-east-1` |
| Variable `ECR_REPOSITORY_URL` | Repository | `ecr_repository_url` |
| Variable `ECR_REPOSITORY_NAME` | Repository | `ecr_repository_name` |

Then the per-environment application settings (not from Terraform output -
these are yours to choose):

| GitHub setting | Scope | Example |
|---|---|---|
| Variable `ROOT_DOMAIN` | Environment `staging` | `example.com` |
| Variable `FRONTEND_DOMAIN` | Environment `staging` | `staging.example.com` |
| Variable `API_DOMAIN` | Environment `staging` | `staging-api.example.com` |
| Variable `ALERT_EMAILS` | Environment `staging` | `you@example.com` |
| Variable `ROOT_DOMAIN` | Environment `production` | `example.com` |
| Variable `FRONTEND_DOMAIN` | Environment `production` | `play.example.com` |
| Variable `API_DOMAIN` | Environment `production` | `api.example.com` |
| Variable `ALERT_EMAILS` | Environment `production` | `you@example.com` |

`terraform-ci.yml`'s pull-request plan job runs outside any GitHub
Environment (it must, to keep the OIDC subject claim matching the
`terraform_plan` role's trust policy - see ADR/`infra/bootstrap/iam.tf`), so
it reads separate repository-level variables instead:
`STAGING_ROOT_DOMAIN`, `STAGING_FRONTEND_DOMAIN`, `STAGING_API_DOMAIN`,
`STAGING_ALERT_EMAILS`, and the `PRODUCTION_*` equivalents. Set these to the
same values as the environment-scoped ones above.

While you're in repo Settings → Environments, add a **required reviewers**
protection rule to the `production` environment - the OIDC trust policy
restricts *which workflow* can assume the deploy role, not *who* approves
it; that's this rule's job.

## 4. First apply of each environment

Locally, with your own admin credentials (subsequent applies are done by
CI):

```bash
cd infra/envs/staging
cp terraform.tfvars.example terraform.tfvars   # then edit it
terraform init
terraform apply -var="backend_image_tag=bootstrap-placeholder"
```

The very first apply will create ECS infrastructure with no valid image
tag - the service will sit unhealthy until the first real deploy runs (push
to `main` triggers `.github/workflows/deploy-staging.yml`). That's expected.

Repeat for `infra/envs/production` once staging looks correct.

## 5. Verify

- `terraform output frontend_url` and `api_base_url` resolve and serve
  traffic once the first CI-driven deploy completes.
- `aws dynamodb describe-table --table-name ofc-staging-rooms` succeeds.
- CloudWatch alarms (`ofc-poker-staging-*`) show `OK`, and the SNS
  subscription emails you added received a confirmation email you need to
  click to actually receive alerts.
- The AWS Budgets console shows the two budgets created in
  `infra/modules/budget`.

## Ongoing

- Rotating IAM/OIDC trust, changing the domain, or moving regions all start
  back at `infra/bootstrap` - it's meant to be re-applied rarely, not on
  every deploy.
- See `docs/runbooks/secrets-rotation.md` for JWT key rotation and
  `docs/runbooks/deploy-and-rollback.md` for day-to-day operations.
