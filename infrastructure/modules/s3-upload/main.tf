resource "aws_s3_object" "upload" {
  bucket = var.bucket_name
  key    = var.object_key
  source = var.source_file
  etag   = filemd5(var.source_file)

  tags = var.tags
}

output "object_key" {
  value = aws_s3_object.upload.key
}

output "etag" {
  value = aws_s3_object.upload.etag
}

output "version_id" {
  value = aws_s3_object.upload.version_id
}
