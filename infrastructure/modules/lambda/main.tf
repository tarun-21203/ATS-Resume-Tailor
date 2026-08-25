resource "aws_lambda_function" "function" {
  s3_bucket        = var.s3_bucket
  s3_key           = var.s3_key
  function_name    = var.function_name
  role             = var.role_arn
  handler          = var.handler
  runtime          = var.runtime
  architectures    = var.architectures
  memory_size      = var.memory_size
  timeout          = var.timeout
  source_code_hash = var.source_code_hash

  environment {
    variables = var.environment_variables
  }
}

resource "aws_cloudwatch_log_group" "log_group" {
  name              = "/aws/lambda/${aws_lambda_function.function.function_name}"
  retention_in_days = var.log_retention_days
}

output "function_name" {
  value = aws_lambda_function.function.function_name
}

output "invoke_arn" {
  value = aws_lambda_function.function.invoke_arn
}

output "arn" {
  value = aws_lambda_function.function.arn
}
