locals {
  name_prefix  = "${var.project_name}-${var.environment}"
  table_prefix = "ofc-${var.environment}-"
  alert_emails = [for e in split(",", var.alert_emails_csv) : trimspace(e) if trimspace(e) != ""]
}

data "aws_route53_zone" "root" {
  name         = var.root_domain
  private_zone = false
}

module "network" {
  source      = "../../modules/network"
  name_prefix = local.name_prefix
}

module "dynamodb" {
  source                        = "../../modules/dynamodb"
  table_prefix                  = local.table_prefix
  enable_point_in_time_recovery = true
}

module "jwt_keys" {
  source      = "../../modules/jwt-keys"
  name_prefix = "${var.project_name}/${var.environment}"
}

module "certificate" {
  source          = "../../modules/certificate"
  frontend_domain = var.frontend_domain
  api_domain      = var.api_domain
  hosted_zone_id  = data.aws_route53_zone.root.zone_id
}

resource "aws_sns_topic" "alarms" {
  name = "${local.name_prefix}-alarms"
}

resource "aws_sns_topic_subscription" "alarm_emails" {
  for_each  = toset(local.alert_emails)
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = each.value
}

module "backend_service" {
  source = "../../modules/backend-service"

  name_prefix                    = local.name_prefix
  vpc_id                         = module.network.vpc_id
  public_subnet_ids              = module.network.public_subnet_ids
  alb_security_group_id          = module.network.alb_security_group_id
  backend_task_security_group_id = module.network.backend_task_security_group_id
  certificate_arn                = module.certificate.certificate_arn
  ecr_repository_url             = var.ecr_repository_url
  image_tag                      = var.backend_image_tag
  table_prefix                   = local.table_prefix
  dynamodb_table_arn             = module.dynamodb.table_arn
  cors_origins                   = "https://${var.frontend_domain}"
  jwt_private_key_secret_arn     = module.jwt_keys.private_key_secret_arn
  jwt_public_key_secret_arn      = module.jwt_keys.public_key_secret_arn
  alarm_topic_arn                = aws_sns_topic.alarms.arn
  environment                    = var.environment
  log_retention_days             = 90

  # Same single-task footprint as staging by design (see docs/adr/0004) -
  # production gets a bit more headroom per task, not more tasks.
  cpu           = 512
  memory        = 1024
  desired_count = 1
}

resource "aws_route53_record" "api" {
  zone_id = data.aws_route53_zone.root.zone_id
  name    = var.api_domain
  type    = "A"

  alias {
    name                   = module.backend_service.alb_dns_name
    zone_id                = module.backend_service.alb_zone_id
    evaluate_target_health = true
  }
}

module "frontend" {
  source          = "../../modules/frontend"
  name_prefix     = local.name_prefix
  domain_name     = var.frontend_domain
  hosted_zone_id  = data.aws_route53_zone.root.zone_id
  certificate_arn = module.certificate.certificate_arn
}

module "budget" {
  source            = "../../modules/budget"
  name_prefix       = local.name_prefix
  environment       = var.environment
  monthly_limit_usd = var.monthly_budget_usd
  alert_emails      = local.alert_emails
}
