variable "table_prefix" {
  description = "Matches OFC_DYNAMODB_TABLE_PREFIX passed to the backend task, e.g. \"ofc-staging-\"."
  type        = string
}

variable "enable_point_in_time_recovery" {
  type    = bool
  default = false
}
