variable "name_prefix" {
  type = string
}

variable "environment" {
  type = string
}

variable "monthly_limit_usd" {
  type    = number
  default = 30
}

variable "alert_emails" {
  type = list(string)
}
