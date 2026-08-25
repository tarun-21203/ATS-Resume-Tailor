#!/usr/bin/env bash
# ==============================================================================
#  deploy.sh — Local deployment script for Resume Tailor
# ==============================================================================
#  Usage:
#    ./deploy.sh -e <ses_email> [OPTIONS]
#
#  Options:
#    -e  Verified SES sender email (required)
#    -r  AWS Region            (default: ca-central-1)
#    -b  Bedrock Region        (default: us-east-1)
#    -m  Bedrock model ID      (default: anthropic.claude-3-haiku-20240307-v1:0)
#    -p  Project name prefix   (default: resume-tailor)
#    -a  AWS CLI profile       (default: fcscrs)
#    -d  Plan only — skip apply
#    -s  Skip build (skip Java and React builds)
#    -h  Show this help message
#
#  Examples:
#    ./deploy.sh -e me@example.com
#    ./deploy.sh -e me@example.com -r ca-central-1 -d
#    ./deploy.sh -e me@example.com -a my-other-profile
# ==============================================================================

set -euo pipefail

###############################################################################
# Defaults
###############################################################################
SES_SOURCE_EMAIL=""
AWS_REGION="ca-central-1"
BEDROCK_REGION="us-east-1"
BEDROCK_MODEL_ID="anthropic.claude-3-haiku-20240307-v1:0"
PROJECT_NAME="resume-tailor"
AWS_PROFILE="fcscrs"
PLAN_ONLY=false
SKIP_BUILD=false

###############################################################################
# Colours
###############################################################################
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
WHITE='\033[1;37m'
RESET='\033[0m'

###############################################################################
# Helpers
###############################################################################
step() {
  echo ""
  echo -e "${CYAN}======================================================"
  echo -e "  $1"
  echo -e "======================================================${RESET}"
}

ok()   { echo -e "${GREEN}[OK]${RESET} $1"; }
info() { echo -e "${YELLOW}[INFO]${RESET} $1"; }
warn() { echo -e "${YELLOW}[WARN]${RESET} $1"; }
err()  { echo -e "${RED}[ERROR]${RESET} $1" >&2; exit 1; }

assert_tool() {
  local tool="$1"
  local hint="${2:-}"
  if ! command -v "$tool" &>/dev/null; then
    err "'$tool' not found on PATH.${hint:+ Hint: $hint}"
  fi
  ok "$tool found — $(command -v "$tool")"
}

###############################################################################
# Parse arguments
###############################################################################
usage() {
  grep '^#  ' "$0" | sed 's/^#  //'
  exit 0
}

while getopts ":e:r:b:m:p:a:dsh" opt; do
  case $opt in
    e) SES_SOURCE_EMAIL="$OPTARG"  ;;
    r) AWS_REGION="$OPTARG"        ;;
    b) BEDROCK_REGION="$OPTARG"    ;;
    m) BEDROCK_MODEL_ID="$OPTARG"  ;;
    p) PROJECT_NAME="$OPTARG"      ;;
    a) AWS_PROFILE="$OPTARG"       ;;
    d) PLAN_ONLY=true              ;;
    s) SKIP_BUILD=true             ;;
    h) usage                       ;;
    :) err "Option -$OPTARG requires an argument." ;;
    \?) err "Unknown option: -$OPTARG" ;;
  esac
done

[[ -z "$SES_SOURCE_EMAIL" ]] && err "SES source email is required. Use -e <value>"

# Export AWS profile so both AWS CLI and Terraform pick it up
export AWS_PROFILE="$AWS_PROFILE"
info "Using AWS profile: $AWS_PROFILE"

###############################################################################
# Derived paths
###############################################################################
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
INFRA_DIR="$ROOT_DIR/infrastructure"
TF_STATE_BUCKET="${PROJECT_NAME}-tfstate"
TF_LOCK_TABLE="${PROJECT_NAME}-tflock"
LAMBDA_JAR="$BACKEND_DIR/build/libs/resume-tailor-0.0.1-SNAPSHOT-aws.jar"

###############################################################################
# STEP 1 — Preflight checks
###############################################################################
step "1 / 6  Preflight — checking required tools"

assert_tool java      "Install JDK 21 from https://adoptium.net/"
assert_tool gradle    "Install Gradle or use the ./gradlew wrapper"
assert_tool node      "Install Node.js 18+ from https://nodejs.org/"
assert_tool npm       "Comes with Node.js"
assert_tool terraform "Install from https://developer.hashicorp.com/terraform/install"
assert_tool aws       "Install AWS CLI v2 from https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"

echo "Java version: $(java -version 2>&1 | head -1)"

echo "Checking AWS credentials (profile: $AWS_PROFILE)..."
IDENTITY=$(aws sts get-caller-identity --profile "$AWS_PROFILE" --output json 2>&1) \
  || err "AWS credentials not configured. Run 'aws configure' first."
ACCOUNT=$(echo "$IDENTITY" | python3 -c "import sys,json; print(json.load(sys.stdin)['Account'])")
ARN=$(echo "$IDENTITY"     | python3 -c "import sys,json; print(json.load(sys.stdin)['Arn'])")
ok "AWS Account: $ACCOUNT | ARN: $ARN"

if [[ "$SKIP_BUILD" == false ]]; then
  ###############################################################################
  # STEP 2 — Build Java backend
  ###############################################################################
  step "2 / 6  Building Java backend (Lambda JAR)"

  cd "$BACKEND_DIR"
  gradle clean build -x test
  ok "Lambda JAR built: $LAMBDA_JAR"
  cd "$ROOT_DIR"
else
  info "Skipping backend build as -s flag is set."
fi

###############################################################################
# STEP 4 — Bootstrap Terraform state resources (idempotent)
###############################################################################
step "3 / 6  Bootstrapping Terraform state backend (S3)"

# --- S3 state bucket ---
if aws s3api head-bucket --bucket "$TF_STATE_BUCKET" --region "$AWS_REGION" --profile "$AWS_PROFILE" 2>/dev/null; then
  ok "S3 state bucket '$TF_STATE_BUCKET' already exists."
else
  echo "Creating S3 state bucket: $TF_STATE_BUCKET"
  if [[ "$AWS_REGION" == "us-east-1" ]]; then
    aws s3api create-bucket --bucket "$TF_STATE_BUCKET" --region "$AWS_REGION" --profile "$AWS_PROFILE" > /dev/null
  else
    aws s3api create-bucket --bucket "$TF_STATE_BUCKET" --region "$AWS_REGION" --profile "$AWS_PROFILE" \
      --create-bucket-configuration LocationConstraint="$AWS_REGION" > /dev/null
  fi
  aws s3api put-bucket-versioning --bucket "$TF_STATE_BUCKET" --profile "$AWS_PROFILE" \
    --versioning-configuration Status=Enabled > /dev/null
  ok "S3 bucket created and versioning enabled."
fi

###############################################################################
# STEP 5 — Terraform init + validate + plan
###############################################################################
step "4 / 6  Terraform init → validate → plan"

export TF_VAR_ses_source_email="$SES_SOURCE_EMAIL"
export TF_VAR_aws_region="$AWS_REGION"
export TF_VAR_bedrock_region="$BEDROCK_REGION"
export TF_VAR_bedrock_model_id="$BEDROCK_MODEL_ID"
export TF_VAR_project_name="$PROJECT_NAME"

cd "$INFRA_DIR"

echo "--- terraform init ---"
terraform init -input=false

echo "--- terraform validate ---"
terraform validate

echo "--- terraform plan ---"
terraform plan -input=false -out=tfplan

###############################################################################
# STEP 6 — Terraform apply
###############################################################################
if [[ "$PLAN_ONLY" == true ]]; then
  info "-d (plan-only) flag set. Skipping apply."
  info "To deploy, run:  cd infrastructure && terraform apply tfplan"
else
  step "5 / 6  Terraform apply"
  terraform apply -input=false -auto-approve tfplan
fi

# ###############################################################################
# # STEP 6 — Build and Sync Frontend
# ###############################################################################
# We do this here because we need the real API_ENDPOINT from Terraform output
step "6 / 6  Frontend Pipeline (Build & Upload)"

# Try to get outputs (this works if state exists, even if we just did a plan)
API_ENDPOINT=$(terraform output -raw api_endpoint 2>/dev/null || echo "")
WEB_BUCKET=$(terraform output -raw website_bucket_name 2>/dev/null || echo "")
FRONTEND_URL=$(terraform output -raw frontend_url 2>/dev/null || echo "")
BUCKET_NAME=$(terraform output -raw resume_bucket_name 2>/dev/null || echo "")

if [[ -z "$API_ENDPOINT" ]]; then
  warn "API Endpoint not found in Terraform state. Deploy infrastructure first to build frontend."
else
  if [[ "$SKIP_BUILD" == false ]]; then
    info "Building React frontend with API_URL: $API_ENDPOINT"
    
    cd "$FRONTEND_DIR"
    echo "VITE_API_URL=$API_ENDPOINT" > .env
    echo "VITE_API_URL=$API_ENDPOINT" > .env.production
    
    npm ci
    npm run build
    
    if [[ -n "$WEB_BUCKET" ]]; then
      aws s3 sync dist "s3://$WEB_BUCKET" --profile "$AWS_PROFILE"
      ok "Frontend uploaded to $FRONTEND_URL"
    fi
    cd "$ROOT_DIR"
  elif [ -d "$FRONTEND_DIR/dist" ] && [[ -n "$WEB_BUCKET" ]]; then
    info "Syncing existing frontend artifacts to S3..."
    aws s3 sync "$FRONTEND_DIR/dist" "s3://$WEB_BUCKET" --profile "$AWS_PROFILE"
    ok "Existing frontend synced."
  fi
fi

if [[ -n "$API_ENDPOINT" ]]; then
  echo ""
  echo -e "${GREEN}======================================================"
  echo -e "  Deployment Status"
  echo -e "======================================================${RESET}"
  echo -e "${WHITE}  Frontend URL  : $FRONTEND_URL${RESET}"
  echo -e "${WHITE}  API Endpoint  : $API_ENDPOINT${RESET}"
  echo -e "${WHITE}  Resume Bucket : $BUCKET_NAME${RESET}"
  echo ""
fi

# Clean up exported vars
unset TF_VAR_ses_source_email TF_VAR_aws_region TF_VAR_bedrock_region TF_VAR_bedrock_model_id TF_VAR_project_name
cd "$ROOT_DIR"
