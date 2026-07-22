variable "aws_region" {
  description = "AWS region for the state bucket, ECR repository, and IAM (IAM/OIDC are global, but this is where the provider talks to)."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short, unique name prefixed onto every resource this repo creates."
  type        = string
  default     = "ofc-poker"
}

variable "github_org" {
  description = "GitHub organization or user that owns the repository (used to scope OIDC trust)."
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name (without the org), used to scope OIDC trust."
  type        = string
  default     = "ofc-poker"
}
