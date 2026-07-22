output "bucket_name" {
  value = aws_s3_bucket.spa.bucket
}

output "distribution_id" {
  value = aws_cloudfront_distribution.spa.id
}

output "distribution_domain_name" {
  value = aws_cloudfront_distribution.spa.domain_name
}

output "url" {
  value = "https://${var.domain_name}"
}
