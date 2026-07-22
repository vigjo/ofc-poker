output "frontend_url" {
  value = module.frontend.url
}

output "api_base_url" {
  value = "https://${var.api_domain}"
}

output "ws_base_url" {
  value = "wss://${var.api_domain}"
}

output "frontend_bucket_name" {
  value = module.frontend.bucket_name
}

output "cloudfront_distribution_id" {
  value = module.frontend.distribution_id
}

output "ecs_cluster_name" {
  value = module.backend_service.cluster_name
}

output "ecs_service_name" {
  value = module.backend_service.service_name
}

output "dynamodb_table_name" {
  value = module.dynamodb.table_name
}
