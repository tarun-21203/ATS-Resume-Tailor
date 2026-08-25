#!/bin/bash

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

###############################################################################
# Colours
###############################################################################
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
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
err()  { echo -e "${RED}[ERROR]${RESET} $1" >&2; exit 1; }

###############################################################################
# Parse arguments
###############################################################################
usage() {
  cat << EOF
Usage: ./destroy.sh [OPTIONS]

Options:
  -e  Verified SES sender email
  -r  AWS Region (default: ca-central-1)
  -b  Bedrock region (default: us-east-1)
  -m  Bedrock model ID (default: anthropic.claude-3-haiku-20240307-v1:0)
  -p  Project name prefix (default: resume-tailor)
  -a  AWS CLI profile (default: fcscrs)
  -h  Show this help message

Example:
  ./destroy.sh -e me@example.com
  ./destroy.sh -a my-profile -r us-east-1
EOF
  exit 0
}

while getopts ":e:r:b:m:p:a:h" opt; do
  case $opt in
    e) SES_SOURCE_EMAIL="$OPTARG"  ;;
    r) AWS_REGION="$OPTARG"        ;;
    b) BEDROCK_REGION="$OPTARG"    ;;
    m) BEDROCK_MODEL_ID="$OPTARG"  ;;
    p) PROJECT_NAME="$OPTARG"      ;;
    a) AWS_PROFILE="$OPTARG"       ;;
    h) usage                       ;;
    :) err "Option -$OPTARG requires an argument." ;;
    \?) err "Unknown option: -$OPTARG" ;;
  esac
done

# Export AWS profile
export AWS_PROFILE="$AWS_PROFILE"
info "Using AWS profile: $AWS_PROFILE"

# Export Terraform variables
export TF_VAR_ses_source_email="${SES_SOURCE_EMAIL:-dummy@example.com}"
export TF_VAR_aws_region="$AWS_REGION"
export TF_VAR_bedrock_region="$BEDROCK_REGION"
export TF_VAR_bedrock_model_id="$BEDROCK_MODEL_ID"
export TF_VAR_project_name="$PROJECT_NAME"

###############################################################################
# Derived paths
###############################################################################
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$ROOT_DIR/infrastructure"

step "Resume Tailor - Infrastructure Destroy"

# Navigate to infrastructure directory
cd "$INFRA_DIR"

# Check if terraform is initialized
if [ ! -d ".terraform" ]; then
  echo "Error: Terraform not initialized. Run deploy.sh first."
  exit 1
fi

# Get API Gateway ID if it exists
API_ID=$(terraform output -raw api_endpoint 2>/dev/null | grep -oP '(?<=https://)[^.]+' || echo "")

if [ -n "$API_ID" ]; then
  echo "Found API Gateway: $API_ID"
  echo "Cleaning up routes to prevent dependency conflicts..."
  
  # Get all routes for this API
  ROUTES=$(aws apigatewayv2 get-routes --api-id "$API_ID" --query 'Items[].RouteId' --output text 2>/dev/null || echo "")
  
  if [ -n "$ROUTES" ]; then
    for ROUTE_ID in $ROUTES; do
      echo "Deleting route: $ROUTE_ID"
      aws apigatewayv2 delete-route --api-id "$API_ID" --route-id "$ROUTE_ID" 2>/dev/null || true
    done
  fi
fi

echo ""
echo "Running terraform destroy..."
terraform destroy -auto-approve

echo ""
echo -e "${GREEN}======================================================"
echo -e "  Infrastructure destroyed successfully!"
echo -e "======================================================${RESET}"

# Clean up exported vars
unset TF_VAR_ses_source_email TF_VAR_aws_region TF_VAR_bedrock_region TF_VAR_bedrock_model_id TF_VAR_project_name
cd "$ROOT_DIR"
