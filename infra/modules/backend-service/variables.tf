variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "alb_security_group_id" {
  type = string
}

variable "backend_task_security_group_id" {
  type = string
}

variable "certificate_arn" {
  description = "ACM certificate ARN for the ALB HTTPS listener (must be in the same region as the ALB)."
  type        = string
}

variable "ecr_repository_url" {
  type = string
}

variable "image_tag" {
  description = "Container image tag to deploy. Updated by CI on each deploy (see .github/workflows/deploy-*.yml)."
  type        = string
  default     = "latest"
}

variable "cpu" {
  type    = number
  default = 256
}

variable "memory" {
  type    = number
  default = 512
}

variable "desired_count" {
  description = "Number of Fargate tasks. The launch architecture is single-task by design (see docs/adr/0004); do not raise without adding Redis pub/sub for WebSocket fan-out first."
  type        = number
  default     = 1
}

variable "table_prefix" {
  type = string
}

variable "dynamodb_table_arn" {
  type = string
}

variable "cors_origins" {
  description = "Value of QUARKUS_HTTP_CORS_ORIGINS, e.g. https://app.example.com"
  type        = string
}

variable "jwt_private_key_secret_arn" {
  type = string
}

variable "jwt_public_key_secret_arn" {
  type = string
}

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "alarm_topic_arn" {
  description = "SNS topic ARN to notify on ALB/ECS alarms."
  type        = string
}

variable "environment" {
  description = "staging or production - used for tagging and log grouping only."
  type        = string
}
