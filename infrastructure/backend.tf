terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }

  backend "s3" {
    bucket       = "resume-tailor-tfstate"
    key          = "dev/terraform.tfstate"
    region       = "ca-central-1"
    use_lockfile = true
    encrypt      = true
  }
}
