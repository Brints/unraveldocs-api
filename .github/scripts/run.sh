#!/bin/bash

set -e
set -o pipefail # Exit if any command in a pipeline fails

# Function to fetch a parameter and exit if it's empty
fetch_parameter() {
  local value
  value=$(aws ssm get-parameter --name "$1" --with-decryption --query 'Parameter.Value' --output text | tr -d '\r\n')
  if [ -z "$value" ]; then
    echo "Error: SSM Parameter $1 is empty or could not be fetched." >&2
    exit 1
  fi
  echo "$value"
}

# Fetch secrets from AWS SSM Parameter Store
APP_BASE_URL=$(fetch_parameter "/unraveldocs/APP_BASE_URL")
RDS_ENDPOINT=$(fetch_parameter "/unraveldocs/RDS_ENDPOINT")
RDS_USERNAME=$(fetch_parameter "/unraveldocs/RDS_USERNAME")
RDS_PASSWORD=$(fetch_parameter "/unraveldocs/RDS_PASSWORD")
JWT_SECRET=$(fetch_parameter "/unraveldocs/JWT_SECRET")
AWS_CLOUDFRONT_URL=$(fetch_parameter "/unraveldocs/AWS_CLOUDFRONT_URL")
MAILGUN_API_KEY=$(fetch_parameter "/unraveldocs/MAILGUN_API_KEY")
MAILGUN_DOMAIN=$(fetch_parameter "/unraveldocs/MAILGUN_DOMAIN")
MAILGUN_SIGNINGIN_KEY=$(fetch_parameter "/unraveldocs/MAILGUN_SIGNINGIN_KEY")
CLOUDINARY_CLOUD_NAME=$(fetch_parameter "/unraveldocs/CLOUDINARY_CLOUD_NAME")
CLOUDINARY_API_KEY=$(fetch_parameter "/unraveldocs/CLOUDINARY_API_KEY")
CLOUDINARY_API_SECRET=$(fetch_parameter "/unraveldocs/CLOUDINARY_API_SECRET")
TWILIO_ACCOUNT_SID=$(fetch_parameter "/unraveldocs/TWILIO_ACCOUNT_SID")
TWILIO_AUTH_TOKEN=$(fetch_parameter "/unraveldocs/TWILIO_AUTH_TOKEN")
TWILIO_PHONE_NUMBER=$(fetch_parameter "/unraveldocs/TWILIO_PHONE_NUMBER")
ELASTICACHE_ENDPOINT=$(fetch_parameter "/unraveldocs/ELASTICACHE_ENDPOINT")
ELASTICACHE_PORT=$(fetch_parameter "/unraveldocs/ELASTICACHE_PORT")
RABBITMQ_ENDPOINT=$(fetch_parameter "/unraveldocs/RABBITMQ_ENDPOINT")
RABBITMQ_PORT=$(fetch_parameter "/unraveldocs/RABBITMQ_PORT")
RABBITMQ_USERNAME=$(fetch_parameter "/unraveldocs/RABBITMQ_USERNAME")
RABBITMQ_PASSWORD=$(fetch_parameter "/unraveldocs/RABBITMQ_PASSWORD")

# Define variables
CONTAINER_NAME="unraveldocs-api"
IMAGE_NAME="${ECR_REGISTRY}/${ECR_REPOSITORY}:latest"

# Stop and remove the existing container if it's running
if [ "$(docker ps -q -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Stopping existing container..."
    docker stop "${CONTAINER_NAME}"
fi
if [ "$(docker ps -aq -f status=exited -f name=^/${CONTAINER_NAME}$)" ]; then
    echo "Removing exited container..."
    docker rm "${CONTAINER_NAME}"
fi

# Clean up old, untagged (dangling) images
if [ -n "$(docker images -f "dangling=true" -q)" ]; then
    echo "Pruning dangling images..."
    docker image prune -f
fi

# Run the new container in detached mode
echo "Starting new container..."
docker run \
  --detach \
  --name "${CONTAINER_NAME}" \
  -p 8080:8080 \
  --restart always \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e APP_BASE_URL="$APP_BASE_URL" \
  -e RDS_ENDPOINT="$RDS_ENDPOINT" \
  -e RDS_USERNAME="$RDS_USERNAME" \
  -e RDS_PASSWORD="$RDS_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
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
  "${IMAGE_NAME}"

echo "Deployment script finished successfully."