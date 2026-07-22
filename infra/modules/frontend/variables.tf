variable "name_prefix" {
  type = string
}

variable "domain_name" {
  description = "Fully-qualified domain for the SPA, e.g. app.example.com"
  type        = string
}

variable "hosted_zone_id" {
  type = string
}

variable "certificate_arn" {
  description = "ACM certificate ARN. Must be issued in us-east-1 (CloudFront requirement) regardless of the provider's default region."
  type        = string
}
