# One DNS-validated ACM certificate covering both the frontend and API
# subdomains. This only works cleanly because the whole stack is deployed in
# us-east-1: CloudFront requires its certificate to live there regardless of
# the distribution's edge locations, and ALB certificates must be in the
# ALB's own region - when both are us-east-1, one certificate serves both.
# If you ever move the ALB to a different region, split this into two certs.
resource "aws_acm_certificate" "main" {
  domain_name               = var.frontend_domain
  subject_alternative_names = [var.api_domain]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "validation" {
  for_each = {
    for dvo in aws_acm_certificate.main.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id         = var.hosted_zone_id
  name            = each.value.name
  type            = each.value.type
  records         = [each.value.record]
  ttl             = 60
  allow_overwrite = true
}

resource "aws_acm_certificate_validation" "main" {
  certificate_arn         = aws_acm_certificate.main.arn
  validation_record_fqdns = [for record in aws_route53_record.validation : record.fqdn]
}
