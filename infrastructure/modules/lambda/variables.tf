variable "function_name" {
  description = "Name of the Lambda function"
  type        = string
}

variable "handler" {
  description = "Lambda function handler"
  type        = string
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java17"
}

variable "architectures" {
  description = "Instruction set architecture for Lambda function"
  type        = list(string)
  default     = ["x86_64"]
}

variable "memory_size" {
  description = "Amount of memory in MB for Lambda function"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Timeout in seconds for Lambda function"
  type        = number
  default     = 30
}

variable "s3_bucket" {
  description = "S3 bucket containing the Lambda deployment package"
  type        = string
}

variable "s3_key" {
  description = "S3 key of the Lambda deployment package"
  type        = string
}

variable "role_arn" {
  description = "ARN of the IAM role for Lambda execution"
  type        = string
}

variable "environment_variables" {
  description = "Environment variables for the Lambda function"
  type        = map(string)
  default     = {}
}

variable "source_code_hash" {
  description = "Hash of the Lambda deployment package"
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}
