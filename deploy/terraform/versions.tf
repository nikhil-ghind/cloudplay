terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.27"
    }
  }

  # Configure a remote backend for shared state in real deployments.
  # backend "s3" {
  #   bucket         = "cloudplay-tfstate"
  #   key            = "cloudplay/eks/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "cloudplay-tf-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Project   = "cloudplay"
      ManagedBy = "terraform"
    }
  }
}
