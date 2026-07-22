output "private_key_secret_arn" {
  value = aws_secretsmanager_secret.jwt_private_key.arn
}

output "public_key_secret_arn" {
  value = aws_secretsmanager_secret.jwt_public_key.arn
}
