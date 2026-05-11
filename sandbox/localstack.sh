#!/bin/bash
set -euo pipefail

REGION="ap-southeast-1"
SENDER_EMAIL="no-reply@localhost"
ENDPOINT="http://localhost:4566"
AWSLOCAL="aws --endpoint-url=http://localhost:4566"

echo "==> Verifying sender email identity..."
$AWSLOCAL ses verify-email-identity \
  --email-address "$SENDER_EMAIL" \
  --region "$REGION"

echo "==> Creating welcome-mail-localdev template..."
$AWSLOCAL ses create-template \
  --region "$REGION" \
  --template '{
    "TemplateName": "welcome-mail-localdev",
    "SubjectPart": "Welcome, {{username}}!",
    "HtmlPart": "<h1>Welcome, {{username}}!</h1><p>Thanks for signing up.</p>",
    "TextPart": "Welcome, {{username}}! Thanks for signing up."
  }'

echo "==> Creating forgot-password-mail-localdev template..."
$AWSLOCAL ses create-template \
  --region "$REGION" \
  --template '{
    "TemplateName": "forgot-password-mail-localdev",
    "SubjectPart": "Reset your password",
    "HtmlPart": "<h1>Password Reset</h1><p>Click <a href=\"http://{{domain}}/reset-password?token={{token}}\">here</a> to reset your password.</p>",
    "TextPart": "Reset your password: http://{{domain}}/reset-password?token={{token}}"
  }'

echo "==> SES initialization complete."
$AWSLOCAL ses list-templates --region "$REGION"
