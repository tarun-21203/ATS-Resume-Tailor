resource "aws_s3_bucket" "resumes" {
  bucket = "${var.project_name}-archive-${random_id.bucket_suffix.hex}"
}

resource "random_id" "bucket_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket" "website" {
  bucket = "${var.project_name}-web-${random_id.bucket_suffix.hex}"
}

resource "aws_s3_bucket_website_configuration" "website_config" {
  bucket = aws_s3_bucket.website.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"
  }
}

resource "aws_s3_bucket_public_access_block" "website_access" {
  bucket = aws_s3_bucket.website.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "website_policy" {
  bucket = aws_s3_bucket.website.id

  # Ensure public access block is removed BEFORE applying the public policy
  depends_on = [aws_s3_bucket_public_access_block.website_access]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.website.arn}/*"
      },
    ]
  })
}

output "bucket_name" {
  value = aws_s3_bucket.resumes.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.resumes.arn
}

output "website_url" {
  value = aws_s3_bucket_website_configuration.website_config.website_endpoint
}

output "website_bucket_name" {
  value = aws_s3_bucket.website.bucket
}
