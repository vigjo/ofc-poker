# ---------------------------------------------------------------------------
# Four narrowly-scoped roles, each trusted by GitHub's OIDC token for a
# specific workflow trigger - never a long-lived access key:
#   - ci_push:            push the backend image on merges to main.
#   - terraform_plan:     read-only plan/validate on pull requests.
#   - deploy_staging:     apply + release, gated by the "staging" GitHub
#                          Environment (see .github/workflows/deploy-staging.yml).
#   - deploy_production:  apply + release, gated by the "production" GitHub
#                          Environment's required-reviewers rule (configure
#                          that protection rule in repo Settings; this trust
#                          policy alone only restricts *which* environment
#                          claim is accepted, not who can approve it).
# ---------------------------------------------------------------------------

locals {
  repo_subject = "repo:${var.github_org}/${var.github_repo}"
}

data "aws_iam_policy_document" "assume_role_ref_main" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.repo_subject}:ref:refs/heads/main"]
    }
  }
}

data "aws_iam_policy_document" "assume_role_pull_request" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.repo_subject}:pull_request"]
    }
  }
}

data "aws_iam_policy_document" "assume_role_environment" {
  for_each = toset(["staging", "production"])

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["${local.repo_subject}:environment:${each.key}"]
    }
  }
}

# --- ci_push: push-only access to the shared ECR repository ---------------

resource "aws_iam_role" "ci_push" {
  name               = "${var.project_name}-ci-push"
  assume_role_policy = data.aws_iam_policy_document.assume_role_ref_main.json
}

data "aws_iam_policy_document" "ci_push" {
  statement {
    sid       = "EcrAuth"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    sid    = "EcrPush"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:BatchGetImage",
      "ecr:DescribeImages",
    ]
    resources = [aws_ecr_repository.backend.arn]
  }
}

resource "aws_iam_role_policy" "ci_push" {
  name   = "ecr-push"
  role   = aws_iam_role.ci_push.id
  policy = data.aws_iam_policy_document.ci_push.json
}

# --- terraform_plan: read-only, for `terraform plan` on pull requests -----

resource "aws_iam_role" "terraform_plan" {
  name               = "${var.project_name}-terraform-plan"
  assume_role_policy = data.aws_iam_policy_document.assume_role_pull_request.json
}

resource "aws_iam_role_policy_attachment" "terraform_plan_read_only" {
  role       = aws_iam_role.terraform_plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

resource "aws_iam_role_policy" "terraform_plan_state_access" {
  name   = "state-access"
  role   = aws_iam_role.terraform_plan.id
  policy = data.aws_iam_policy_document.state_access.json
}

# --- deploy_staging / deploy_production: apply + release -------------------

resource "aws_iam_role" "deploy" {
  for_each           = data.aws_iam_policy_document.assume_role_environment
  name               = "${var.project_name}-terraform-deploy-${each.key}"
  assume_role_policy = each.value.json
}

resource "aws_iam_role_policy" "deploy_state_access" {
  for_each = aws_iam_role.deploy
  name     = "state-access"
  role     = each.value.id
  policy   = data.aws_iam_policy_document.state_access.json
}

resource "aws_iam_role_policy" "deploy_app_access" {
  for_each = aws_iam_role.deploy
  name     = "app-resources"
  role     = each.value.id
  policy   = data.aws_iam_policy_document.deploy_app_access.json
}

resource "aws_iam_role_policy_attachment" "deploy_ecr" {
  for_each   = aws_iam_role.deploy
  role       = each.value.name
  policy_arn = aws_iam_policy.ecr_pull_push.arn
}

resource "aws_iam_policy" "ecr_pull_push" {
  name   = "${var.project_name}-ecr-pull-push"
  policy = data.aws_iam_policy_document.ci_push.json
}

# Terraform state bucket/lock-table access, shared by every role that runs
# `terraform init`/`plan`/`apply`.
data "aws_iam_policy_document" "state_access" {
  statement {
    sid       = "StateBucket"
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
    resources = [aws_s3_bucket.terraform_state.arn, "${aws_s3_bucket.terraform_state.arn}/*"]
  }
  statement {
    sid       = "StateLock"
    effect    = "Allow"
    actions   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
    resources = [aws_dynamodb_table.terraform_locks.arn]
  }
}

# Everything Terraform needs to create/update the app's own resources. This
# is intentionally broad-by-service rather than hand-crafted per-resource -
# a fully least-privilege policy for ECS/ALB/CloudFront/DynamoDB/Secrets/IAM
# together is a lot of surface area for a single-task side project, and the
# blast radius is already contained by the OIDC trust conditions above (only
# these specific, environment-gated workflows can ever assume this role).
# IAM itself is scoped to just the roles/policies this project creates.
data "aws_iam_policy_document" "deploy_app_access" {
  statement {
    sid    = "AppServices"
    effect = "Allow"
    actions = [
      "ec2:*Vpc*", "ec2:*Subnet*", "ec2:*SecurityGroup*", "ec2:*InternetGateway*",
      "ec2:*RouteTable*", "ec2:*Route", "ec2:DescribeAvailabilityZones", "ec2:DescribeAddresses",
      "ecs:*",
      "elasticloadbalancing:*",
      "dynamodb:*",
      "secretsmanager:*",
      "logs:*",
      "s3:*",
      "cloudfront:*",
      "acm:*",
      "route53:*",
      "budgets:*",
      "application-autoscaling:*",
      "cloudwatch:*",
      "sns:*",
    ]
    resources = ["*"]
  }
  statement {
    sid    = "IamForOwnResources"
    effect = "Allow"
    actions = [
      "iam:CreateRole", "iam:DeleteRole", "iam:GetRole", "iam:TagRole",
      "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy",
      "iam:AttachRolePolicy", "iam:DetachRolePolicy", "iam:ListAttachedRolePolicies",
      "iam:ListRolePolicies", "iam:PassRole", "iam:CreateServiceLinkedRole",
    ]
    resources = [
      "arn:aws:iam::*:role/${var.project_name}-*",
      "arn:aws:iam::*:role/aws-service-role/*",
    ]
  }
}
