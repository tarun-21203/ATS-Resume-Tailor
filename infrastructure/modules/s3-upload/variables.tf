variable "bucket_name" {
  description = "Name of the S3 bucket"
  type        = string
}

variable "object_key" {
  description = "S3 object key (path)"
  type        = string
}

variable "source_file" {
  description = "Path to the source file to upload"
  type        = string
}

variable "tags" {
  description = "Tags to apply to the S3 object"
  type        = map(string)
  default     = {}
}
