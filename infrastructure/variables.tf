variable "aws_region" {
  description = "AWS Region to deploy resources into"
  type        = string
  default     = "ca-central-1"
}

variable "project_name" {
  description = "Project name prefix used for all resources"
  type        = string
  default     = "resume-tailor"
}

variable "bedrock_model_id" {
  description = "Bedrock model id used for AI generation"
  type        = string
  default     = "anthropic.claude-3-haiku-20240307-v1:0"
}

variable "bedrock_region" {
  description = "AWS region where Bedrock model is available"
  type        = string
  default     = "us-east-1"
}

variable "ses_source_email" {
  description = "Verified SES source email address used for sending tailored resumes"
  type        = string
}
