###############################################################################
# Module: Security (IAM Roles & Policies)
###############################################################################
module "security" {
  source       = "./modules/security"
  project_name = var.project_name
}

module "storage" {
  source       = "./modules/storage"
  project_name = var.project_name
}

module "messaging" {
  source           = "./modules/messaging"
  ses_source_email = var.ses_source_email
}

module "dynamodb_analytics" {
  source       = "./modules/dynamodb"
  table_name   = "${var.project_name}-analytics"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "metric_type"
  range_key    = "timestamp"

  attributes = [
    { name = "metric_type", type = "S" },
    { name = "timestamp", type = "N" }
  ]

  ttl_enabled        = true
  ttl_attribute_name = "ttl"

  tags = { Name = "${var.project_name}-analytics", Environment = "production" }
}

module "dynamodb_master_resume" {
  source       = "./modules/dynamodb"
  table_name   = "${var.project_name}-master-resume"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "user_id"

  attributes = [
    { name = "user_id", type = "S" }
  ]

  ttl_enabled = false
  tags        = { Name = "${var.project_name}-master-resume", Environment = "production" }
}

module "lambda_jar_upload" {
  source      = "./modules/s3-upload"
  bucket_name = module.storage.bucket_name
  object_key  = "lambda/resume-tailor.jar"
  source_file = "../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar"

  tags = { Name = "lambda-deployment-package", Environment = "production" }
}

module "lambda_tailor_resume" {
  source           = "./modules/lambda"
  function_name    = "${var.project_name}-tailor-function"
  handler          = "com.resumetailor.handler.TailorResumeHandler::handleRequest"
  runtime          = "java17"
  memory_size      = 2048
  timeout          = 300
  s3_bucket        = module.storage.bucket_name
  s3_key           = module.lambda_jar_upload.object_key
  role_arn         = module.security.lambda_role_arn
  source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")

  environment_variables = {
    SES_SOURCE_EMAIL         = var.ses_source_email
    S3_BUCKET_NAME           = module.storage.bucket_name
    DYNAMODB_TABLE_NAME      = module.dynamodb_analytics.table_name
    MASTER_RESUME_TABLE_NAME = module.dynamodb_master_resume.table_name
    BEDROCK_MODEL_ID         = var.bedrock_model_id
    BEDROCK_REGION           = var.bedrock_region
  }
}

module "lambda_send_email" {
  source           = "./modules/lambda"
  function_name    = "${var.project_name}-email-function"
  handler          = "com.resumetailor.handler.SendEmailHandler::handleRequest"
  runtime          = "java17"
  memory_size      = 512
  timeout          = 60
  s3_bucket        = module.storage.bucket_name
  s3_key           = module.lambda_jar_upload.object_key
  role_arn         = module.security.lambda_role_arn
  source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")

  environment_variables = {
    SES_SOURCE_EMAIL         = var.ses_source_email
    S3_BUCKET_NAME           = module.storage.bucket_name
    DYNAMODB_TABLE_NAME      = module.dynamodb_analytics.table_name
    MASTER_RESUME_TABLE_NAME = module.dynamodb_master_resume.table_name
    BEDROCK_MODEL_ID         = var.bedrock_model_id
    BEDROCK_REGION           = var.bedrock_region
  }
}

module "lambda_track_analytics" {
  source           = "./modules/lambda"
  function_name    = "${var.project_name}-track-analytics"
  handler          = "com.resumetailor.handler.AnalyticsHandler::handleRequest"
  runtime          = "java17"
  memory_size      = 512
  timeout          = 30
  s3_bucket        = module.storage.bucket_name
  s3_key           = module.lambda_jar_upload.object_key
  role_arn         = module.security.lambda_role_arn
  source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")

  environment_variables = {
    DYNAMODB_TABLE_NAME      = module.dynamodb_analytics.table_name
    MASTER_RESUME_TABLE_NAME = module.dynamodb_master_resume.table_name
    SES_SOURCE_EMAIL         = var.ses_source_email
    S3_BUCKET_NAME           = module.storage.bucket_name
    BEDROCK_MODEL_ID         = var.bedrock_model_id
    BEDROCK_REGION           = var.bedrock_region
  }
}

module "lambda_get_analytics" {
  source           = "./modules/lambda"
  function_name    = "${var.project_name}-get-analytics"
  handler          = "com.resumetailor.handler.GetAnalyticsHandler::handleRequest"
  runtime          = "java17"
  memory_size      = 512
  timeout          = 30
  s3_bucket        = module.storage.bucket_name
  s3_key           = module.lambda_jar_upload.object_key
  role_arn         = module.security.lambda_role_arn
  source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")

  environment_variables = {
    DYNAMODB_TABLE_NAME      = module.dynamodb_analytics.table_name
    MASTER_RESUME_TABLE_NAME = module.dynamodb_master_resume.table_name
    SES_SOURCE_EMAIL         = var.ses_source_email
    S3_BUCKET_NAME           = module.storage.bucket_name
    BEDROCK_MODEL_ID         = var.bedrock_model_id
    BEDROCK_REGION           = var.bedrock_region
  }
}

module "lambda_master_resume" {
  source           = "./modules/lambda"
  function_name    = "${var.project_name}-master-resume"
  handler          = "com.resumetailor.handler.MasterResumeHandler::handleRequest"
  runtime          = "java17"
  memory_size      = 512
  timeout          = 60
  s3_bucket        = module.storage.bucket_name
  s3_key           = module.lambda_jar_upload.object_key
  role_arn         = module.security.lambda_role_arn
  source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")

  environment_variables = {
    SES_SOURCE_EMAIL         = var.ses_source_email
    S3_BUCKET_NAME           = module.storage.bucket_name
    DYNAMODB_TABLE_NAME      = module.dynamodb_analytics.table_name
    MASTER_RESUME_TABLE_NAME = module.dynamodb_master_resume.table_name
    BEDROCK_MODEL_ID         = var.bedrock_model_id
    BEDROCK_REGION           = var.bedrock_region
  }
}

module "networking" {
  source                        = "./modules/networking"
  project_name                  = var.project_name
  tailor_lambda_invoke_arn      = module.lambda_tailor_resume.invoke_arn
  tailor_lambda_function_name   = module.lambda_tailor_resume.function_name
  email_lambda_invoke_arn       = module.lambda_send_email.invoke_arn
  email_lambda_function_name    = module.lambda_send_email.function_name
  track_analytics_invoke_arn    = module.lambda_track_analytics.invoke_arn
  track_analytics_function_name = module.lambda_track_analytics.function_name
  get_analytics_invoke_arn      = module.lambda_get_analytics.invoke_arn
  get_analytics_function_name   = module.lambda_get_analytics.function_name
  master_resume_invoke_arn      = module.lambda_master_resume.invoke_arn
  master_resume_function_name   = module.lambda_master_resume.function_name
}

output "api_endpoint" { value = module.networking.api_endpoint }
output "resume_bucket_name" { value = module.storage.bucket_name }
output "frontend_url" { value = "http://${module.storage.website_url}" }
output "website_bucket_name" { value = module.storage.website_bucket_name }

###############################################################################
# Cognito (User Accounts)
###############################################################################
resource "aws_cognito_user_pool" "users" {
  name                     = "${var.project_name}-users"
  auto_verified_attributes = ["email"]

  schema {
    attribute_data_type = "String"
    name                = "email"
    required            = true
    mutable             = true
  }
}

resource "aws_cognito_user_pool_client" "web" {
  name         = "${var.project_name}-web-client"
  user_pool_id = aws_cognito_user_pool.users.id

  explicit_auth_flows = [
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_PASSWORD_AUTH"
  ]

  generate_secret = false
}

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.users.id
}

output "cognito_client_id" {
  value = aws_cognito_user_pool_client.web.id
}
