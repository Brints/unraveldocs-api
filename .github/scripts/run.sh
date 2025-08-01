#!/bin/bash

set -e

# Fetch secrets from AWS SSM Parameter Store
APP_BASE_URL=$(aws ssm get-parameter --name /unraveldocs/APP_BASE_URL --with-decryption --query 'Parameter.Value' --output text)
RDS_ENDPOINT=$(aws ssm get-parameter --name /unraveldocs/RDS_ENDPOINT --with-decryption --query 'Parameter.Value' --output text)
RDS_USERNAME=$(aws ssm get-parameter --name /unraveldocs/RDS_USERNAME --with-decryption --query 'Parameter.Value' --output text)
RDS_PASSWORD=$(aws ssm get-parameter --name /unraveldocs/RDS_PASSWORD --with-decryption --query 'Parameter.Value' --output text)
JWT_SECRET=$(aws ssm get-parameter --name /unraveldocs/JWT_SECRET --with-decryption --query 'Parameter.Value' --output text)
AWS_ACCESS_KEY=$(aws ssm get-parameter --name /unraveldocs/AWS_ACCESS_KEY --with-decryption --query 'Parameter.Value' --output text)
AWS_SECRET_KEY=$(aws ssm get-parameter --name /unraveldocs/AWS_SECRET_KEY --with-decryption --query 'Parameter.Value' --output text)
AWS_CLOUDFRONT_URL=$(aws ssm get-parameter --name /unraveldocs/AWS_CLOUDFRONT_URL --with-decryption --query 'Parameter.Value' --output text)
MAILGUN_API_KEY=$(aws ssm get-parameter --name /unraveldocs/MAILGUN_API_KEY --with-decryption --query 'Parameter.Value' --output text)
MAILGUN_DOMAIN=$(aws ssm get-parameter --name /unraveldocs/MAILGUN_DOMAIN --with-decryption --query 'Parameter.Value' --output text)
MAILGUN_SIGNINGIN_KEY=$(aws ssm get-parameter --name /unraveldocs/MAILGUN_SIGNINGIN_KEY --with-decryption --query 'Parameter.Value' --output text)
CLOUDINARY_CLOUD_NAME=$(aws ssm get-parameter --name /unraveldocs/CLOUDINARY_CLOUD_NAME --with-decryption --query 'Parameter.Value' --output text)
CLOUDINARY_API_KEY=$(aws ssm get-parameter --name /unraveldocs/CLOUDINARY_API_KEY --with-decryption --query 'Parameter.Value' --output text)
CLOUDINARY_API_SECRET=$(aws ssm get-parameter --name /unraveldocs/CLOUDINARY_API_SECRET --with-decryption --query 'Parameter.Value' --output text)
TWILIO_ACCOUNT_SID=$(aws ssm get-parameter --name /unraveldocs/TWILIO_ACCOUNT_SID --with-decryption --query 'Parameter.Value' --output text)
TWILIO_AUTH_TOKEN=$(aws ssm get-parameter --name /unraveldocs/TWILIO_AUTH_TOKEN --with-decryption --query 'Parameter.Value' --output text)
TWILIO_PHONE_NUMBER=$(aws ssm get-parameter --name /unraveldocs/TWILIO_PHONE_NUMBER --with-decryption --query 'Parameter.Value' --output text)
ELASTICACHE_ENDPOINT=$(aws ssm get-parameter --name /unraveldocs/ELASTICACHE_ENDPOINT --with-decryption --query 'Parameter.Value' --output text)
ELASTICACHE_PORT=$(aws ssm get-parameter --name /unraveldocs/ELASTICACHE_PORT --with-decryption --query 'Parameter.Value' --output text)
RABBITMQ_ENDPOINT=$(aws ssm get-parameter --name /unraveldocs/RABBITMQ_ENDPOINT --with-decryption --query 'Parameter.Value' --output text)
RABBITMQ_PORT=$(aws ssm get-parameter --name /unraveldocs/RABBITMQ_PORT --with-decryption --query 'Parameter.Value' --output text)
RABBITMQ_USERNAME=$(aws ssm get-parameter --name /unraveldocs/RABBITMQ_USERNAME --with-decryption --query 'Parameter.Value' --output text)
RABBITMQ_PASSWORD=$(aws ssm get-parameter --name /unraveldocs/RABBITMQ_PASSWORD --with-decryption --query 'Parameter.Value' --output text)

# Stop and remove existing container if running
docker rm -f unraveldocs-api || true

# Run the Docker container with all environment variables
docker run -d --name unraveldocs-api \
  -p 8080:8080 \
  -e APP_BASE_URL="$APP_BASE_URL" \
  -e RDS_ENDPOINT="$RDS_ENDPOINT" \
  -e RDS_USERNAME="$RDS_USERNAME" \
  -e RDS_PASSWORD="$RDS_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e AWS_ACCESS_KEY="$AWS_ACCESS_KEY" \
  -e AWS_SECRET_KEY="$AWS_SECRET_KEY" \
  -e AWS_CLOUDFRONT_URL="$AWS_CLOUDFRONT_URL" \
  -e MAILGUN_API_KEY="$MAILGUN_API_KEY" \
  -e MAILGUN_DOMAIN="$MAILGUN_DOMAIN" \
  -e MAILGUN_SIGNINGIN_KEY="$MAILGUN_SIGNINGIN_KEY" \
  -e CLOUDINARY_CLOUD_NAME="$CLOUDINARY_CLOUD_NAME" \
  -e CLOUDINARY_API_KEY="$CLOUDINARY_API_KEY" \
  -e CLOUDINARY_API_SECRET="$CLOUDINARY_API_SECRET" \
  -e TWILIO_ACCOUNT_SID="$TWILIO_ACCOUNT_SID" \
  -e TWILIO_AUTH_TOKEN="$TWILIO_AUTH_TOKEN" \
  -e TWILIO_PHONE_NUMBER="$TWILIO_PHONE_NUMBER" \
  -e ELASTICACHE_ENDPOINT="$ELASTICACHE_ENDPOINT" \
  -e ELASTICACHE_PORT="$ELASTICACHE_PORT" \
  -e RABBITMQ_ENDPOINT="$RABBITMQ_ENDPOINT" \
  -e RABBITMQ_PORT="$RABBITMQ_PORT" \
  -e RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
  -e RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
  985420682262.dkr.ecr.eu-north-1.amazonaws.com/unraveldocs-api:latest
