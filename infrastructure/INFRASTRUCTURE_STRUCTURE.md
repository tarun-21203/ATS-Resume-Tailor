# Infrastructure Structure

## Overview
The infrastructure is organized using Terraform modules for reusability and clarity. All Lambda functions are defined in main.tf using a reusable Lambda module.

## Directory Structure

```
infrastructure/
├── main.tf                    # Main infrastructure configuration
├── variables.tf               # Input variables
├── provider.tf               # AWS provider configuration
├── backend.tf                # Terraform state backend
├── modules/
│   ├── lambda/               # Reusable Lambda function module
│   │   ├── main.tf
│   │   └── variables.tf
│   ├── security/             # IAM roles and policies
│   │   ├── main.tf
│   │   └── variables.tf
│   ├── storage/              # S3 buckets
│   │   ├── main.tf
│   │   └── variables.tf
│   ├── messaging/            # SES configuration
│   │   ├── main.tf
│   │   └── variables.tf
│   └── networking/           # API Gateway
│       ├── main.tf
│       └── variables.tf
```

## Main.tf Components

### 1. Security Module
- IAM role for Lambda execution
- Policies for S3, SES, and DynamoDB access

### 2. Storage Module
- S3 bucket for resume storage
- S3 bucket for frontend hosting
- Bucket policies and CORS configuration

### 3. Messaging Module
- SES email identity verification

### 4. DynamoDB Table (Direct Resource)
- Table name: `{project_name}-analytics`
- Hash key: `metric_type`
- Range key: `timestamp`
- Billing: PAY_PER_REQUEST

### 5. Lambda JAR Upload (Direct Resource)
- Uploads backend JAR to S3
- Key: `lambda/resume-tailor.jar`

### 6. Lambda Functions (Using Lambda Module)

#### a. Tailor Resume Lambda
- **Function**: `{project_name}-tailor-function`
- **Handler**: `TailorResumeHandler::handleRequest`
- **Memory**: 2048 MB
- **Timeout**: 300 seconds
- **Purpose**: Process resume tailoring with Amazon Bedrock AI

#### b. Send Email Lambda
- **Function**: `{project_name}-email-function`
- **Handler**: `SendEmailHandler::handleRequest`
- **Memory**: 512 MB
- **Timeout**: 60 seconds
- **Purpose**: Send tailored resume via email

#### c. Track Analytics Lambda
- **Function**: `{project_name}-track-analytics`
- **Handler**: `AnalyticsHandler::handleRequest`
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Purpose**: Track analytics events to DynamoDB

#### d. Get Analytics Lambda
- **Function**: `{project_name}-get-analytics`
- **Handler**: `GetAnalyticsHandler::handleRequest`
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Purpose**: Retrieve analytics metrics from DynamoDB

### 7. Networking Module
- API Gateway HTTP API
- Routes:
  - `POST /tailorResume`
  - `POST /sendEmail`
  - `POST /track`
  - `GET /analytics`
  - `GET /masterResume`
  - `PUT /masterResume`
  - `DELETE /masterResume`
- Lambda integrations and permissions
- CORS configuration

## Lambda Module

The reusable Lambda module (`modules/lambda/`) accepts:

### Inputs
- `function_name` - Name of the Lambda function
- `handler` - Function handler path
- `runtime` - Runtime (default: java17)
- `architectures` - CPU architecture (default: x86_64)
- `memory_size` - Memory in MB
- `timeout` - Timeout in seconds
- `s3_bucket` - S3 bucket with deployment package
- `s3_key` - S3 key of deployment package
- `role_arn` - IAM role ARN
- `environment_variables` - Map of environment variables
- `source_code_hash` - Hash for change detection
- `log_retention_days` - CloudWatch log retention (default: 7)

### Outputs
- `function_name` - Lambda function name
- `invoke_arn` - ARN for API Gateway integration
- `arn` - Full Lambda ARN

### Features
- Automatically creates CloudWatch log group
- Configurable memory, timeout, and runtime
- Flexible environment variables

## Environment Variables

All Lambda functions have access to:
- `SES_SOURCE_EMAIL` - Verified SES email
- `S3_BUCKET_NAME` - S3 bucket for resumes
- `BEDROCK_MODEL_ID` - Bedrock model used for generation
- `BEDROCK_REGION` - Bedrock runtime region
- `DYNAMODB_TABLE_NAME` - Analytics table name
- `MASTER_RESUME_TABLE_NAME` - User master resume metadata table

## Deployment

### Prerequisites
1. Build backend JAR:
   ```bash
   cd backend
   ./gradlew clean shadowJar
   ```

2. Set required variables:
   - `ses_source_email` - Your verified SES email
   - `bedrock_model_id` - Bedrock model ID
   - `bedrock_region` - Bedrock region

### Deploy
```bash
cd infrastructure
terraform init
terraform plan -var="ses_source_email=your@email.com" -var="bedrock_model_id=anthropic.claude-3-haiku-20240307-v1:0" -var="bedrock_region=us-east-1"
terraform apply -var="ses_source_email=your@email.com" -var="bedrock_model_id=anthropic.claude-3-haiku-20240307-v1:0" -var="bedrock_region=us-east-1"
```

Or use the deploy script:
```bash
./deploy.sh -e your@email.com
```

### Destroy
```bash
./destroy.sh -e your@email.com
```

## Outputs

After deployment, Terraform provides:
- `api_endpoint` - API Gateway base URL
- `resume_bucket_name` - S3 bucket for resumes
- `frontend_url` - Static website URL
- `website_bucket_name` - Frontend S3 bucket name

## Benefits of This Structure

1. **Modularity**: Lambda module is reusable for all functions
2. **Clarity**: All Lambda definitions in one place (main.tf)
3. **Maintainability**: Easy to add/modify Lambda functions
4. **Consistency**: All functions use the same module pattern
5. **DRY**: No code duplication across Lambda functions
6. **Flexibility**: Easy to customize per-function settings

## Adding a New Lambda Function

To add a new Lambda function:

1. Create the handler in backend Java code
2. Add a new module block in main.tf:
   ```hcl
   module "lambda_new_function" {
     source           = "./modules/lambda"
     function_name    = "${var.project_name}-new-function"
     handler          = "com.resumetailor.handler.NewHandler::handleRequest"
     memory_size      = 512
     timeout          = 30
     s3_bucket        = module.storage.bucket_name
     s3_key           = aws_s3_object.lambda_jar.key
     role_arn         = module.security.lambda_role_arn
     source_code_hash = filebase64sha256("../backend/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar")
     
     environment_variables = {
       # Add required env vars
     }
   }
   ```
3. Add API Gateway route in networking module if needed
4. Deploy with `terraform apply`

## Notes

- All Lambda functions share the same JAR file
- CloudWatch logs are automatically created with 7-day retention
- DynamoDB table uses PAY_PER_REQUEST billing
- API Gateway has CORS enabled for all origins
- Sensitive values should be supplied through environment variables or Parameter Store
