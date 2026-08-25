resource "aws_ses_email_identity" "default" {
  email = var.ses_source_email
}
