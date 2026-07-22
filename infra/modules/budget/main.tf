# Filtering by the "Environment" tag requires it to be activated as a
# user-defined cost allocation tag in Billing & Cost Management first - that
# activation can't be done via Terraform and takes up to 24h to take effect
# (see docs/runbooks/aws-bootstrap.md). Until then this budget simply tracks
# whole-account spend for both filters, which is a safe (if coarser) default.
resource "aws_budgets_budget" "monthly_cost" {
  name         = "${var.name_prefix}-monthly"
  budget_type  = "COST"
  limit_amount = var.monthly_limit_usd
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  cost_filter {
    name = "TagKeyValue"
    values = [
      "user:Environment${"$"}${var.environment}",
    ]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = var.alert_emails
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = var.alert_emails
  }
}
