variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "ofc-poker"
}

variable "environment" {
  type    = string
  default = "staging"
}

variable "root_domain" {
  description = "Apex domain whose Route53 hosted zone already exists, e.g. \"example.com\"."
  type        = string
}

variable "frontend_domain" {
  description = "Fully-qualified domain for the SPA, e.g. \"staging.example.com\"."
  type        = string
}

variable "api_domain" {
  description = "Fully-qualified domain for the backend API/WebSocket, e.g. \"staging-api.example.com\"."
  type        = string
}

variable "ecr_repository_url" {
  description = "Output from infra/bootstrap (ecr_repository_url)."
  type        = string
}

variable "backend_image_tag" {
  description = "Backend image tag to deploy (an ECR tag, normally the deploying commit's SHA). Passed by CI via -var; see .github/workflows/deploy-staging.yml."
  type        = string
  default     = "bootstrap-placeholder"
}

variable "alert_emails_csv" {
  description = "Comma-separated email addresses subscribed to ops alarms and budget notifications (a plain string, not a list, so CI can pass it as a single GitHub Actions variable/TF_VAR_ env var without any JSON-encoding gymnastics)."
  type        = string
}

variable "monthly_budget_usd" {
  type    = number
  default = 20
}
