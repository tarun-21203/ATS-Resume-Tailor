resource "aws_apigatewayv2_api" "http_api" {
  name          = "${var.project_name}-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["Content-Type", "Authorization", "x-user-id"]
    max_age       = 300
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_apigatewayv2_integration" "tailor" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.tailor_lambda_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_integration" "email" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.email_lambda_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_integration" "track" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.track_analytics_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_integration" "analytics" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.get_analytics_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_integration" "master" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "AWS_PROXY"
  integration_uri        = var.master_resume_invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "tailor_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /tailorResume"
  target    = "integrations/${aws_apigatewayv2_integration.tailor.id}"
}

resource "aws_apigatewayv2_route" "email_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /sendEmail"
  target    = "integrations/${aws_apigatewayv2_integration.email.id}"
}

resource "aws_apigatewayv2_route" "track_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /track"
  target    = "integrations/${aws_apigatewayv2_integration.track.id}"
}

resource "aws_apigatewayv2_route" "analytics_route" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /analytics"
  target    = "integrations/${aws_apigatewayv2_integration.analytics.id}"
}

resource "aws_apigatewayv2_route" "master_get" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /masterResume"
  target    = "integrations/${aws_apigatewayv2_integration.master.id}"
}

resource "aws_apigatewayv2_route" "master_put" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "PUT /masterResume"
  target    = "integrations/${aws_apigatewayv2_integration.master.id}"
}

resource "aws_apigatewayv2_route" "master_delete" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "DELETE /masterResume"
  target    = "integrations/${aws_apigatewayv2_integration.master.id}"
}

resource "aws_lambda_permission" "tailor" {
  statement_id  = "AllowExecutionFromAPIGatewayTailor"
  action        = "lambda:InvokeFunction"
  function_name = var.tailor_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "email" {
  statement_id  = "AllowExecutionFromAPIGatewayEmail"
  action        = "lambda:InvokeFunction"
  function_name = var.email_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "track" {
  statement_id  = "AllowExecutionFromAPIGatewayTrack"
  action        = "lambda:InvokeFunction"
  function_name = var.track_analytics_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "analytics" {
  statement_id  = "AllowExecutionFromAPIGatewayGetAnalytics"
  action        = "lambda:InvokeFunction"
  function_name = var.get_analytics_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "master" {
  statement_id  = "AllowExecutionFromAPIGatewayMasterResume"
  action        = "lambda:InvokeFunction"
  function_name = var.master_resume_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

output "api_endpoint" {
  value = aws_apigatewayv2_api.http_api.api_endpoint
}
