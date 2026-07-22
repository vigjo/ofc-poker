# Generates the RS256 keypair used to sign/verify guest seat tokens and
# stores both halves in Secrets Manager. The ECS task definition injects
# these as `secrets` (env vars), and backend/docker-entrypoint.sh writes them
# to disk before the JVM starts (see backend/.env.example for why - Fargate
# has no native secret-to-file mount).
#
# Rotation: tainting/re-applying this resource issues a new keypair, which
# invalidates every outstanding guest token immediately (players are bounced
# to reconnect/rejoin). Acceptable for a private, low-stakes game; see
# docs/runbooks/secrets-rotation.md.
resource "tls_private_key" "jwt" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "aws_secretsmanager_secret" "jwt_private_key" {
  name                    = "${var.name_prefix}/jwt-private-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt_private_key" {
  secret_id = aws_secretsmanager_secret.jwt_private_key.id
  # SmallRye JWT requires PKCS#8 ("BEGIN PRIVATE KEY"); tls_private_key's
  # default private_key_pem attribute is PKCS#1 and won't parse.
  secret_string = tls_private_key.jwt.private_key_pem_pkcs8
}

resource "aws_secretsmanager_secret" "jwt_public_key" {
  name                    = "${var.name_prefix}/jwt-public-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "jwt_public_key" {
  secret_id     = aws_secretsmanager_secret.jwt_public_key.id
  secret_string = tls_private_key.jwt.public_key_pem
}
