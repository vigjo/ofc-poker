terraform {
  required_version = ">= 1.9.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Bucket/table names come from `terraform output` in infra/bootstrap -
  # backend blocks can't reference variables or data sources, so these are
  # filled in by hand once, immediately after the bootstrap apply. See
  # docs/runbooks/aws-bootstrap.md.
  backend "s3" {
    bucket         = "REPLACE_WITH_BOOTSTRAP_OUTPUT_terraform_state_bucket"
    key            = "staging/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "ofc-poker-terraform-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}
