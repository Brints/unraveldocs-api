# UnravelDocs API - AWS Deployment Guide

This comprehensive guide walks you through deploying the UnravelDocs API on AWS using **EC2**, **RDS (PostgreSQL)**, **ECR**, **Nginx**, and **Certbot** for SSL. Your API will be accessible at `api.unraveldocs.xyz`.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [AWS Architecture Overview](#aws-architecture-overview)
3. [Step 1: AWS Account Setup](#step-1-aws-account-setup)
4. [Step 2: Set Up RDS (PostgreSQL)](#step-2-set-up-rds-postgresql)
5. [Step 3: Set Up ElastiCache (Redis)](#step-3-set-up-elasticache-redis)
6. [Step 4: Set Up Amazon MQ (RabbitMQ)](#step-4-set-up-amazon-mq-rabbitmq)
7. [Step 5: Set Up ECR (Elastic Container Registry)](#step-5-set-up-ecr-elastic-container-registry)
8. [Step 6: Launch EC2 Instance](#step-6-launch-ec2-instance)
9. [Step 7: Install Docker on EC2](#step-7-install-docker-on-ec2)
10. [Step 8: Push Docker Image to ECR](#step-8-push-docker-image-to-ecr)
11. [Step 9: Pull and Run Container on EC2](#step-9-pull-and-run-container-on-ec2)
12. [Step 10: Configure Nginx as Reverse Proxy](#step-10-configure-nginx-as-reverse-proxy)
13. [Step 11: Set Up SSL with Certbot](#step-11-set-up-ssl-with-certbot)
14. [Step 12: Configure Namecheap DNS](#step-12-configure-namecheap-dns)
15. [Step 13: Production Environment Variables](#step-13-production-environment-variables)
16. [Step 14: Set Up CloudWatch Monitoring](#step-14-set-up-cloudwatch-monitoring)
17. [Step 15: Configure Auto Scaling](#step-15-configure-auto-scaling)
18. [Step 16: Set Up CI/CD with GitHub Actions](#step-16-set-up-cicd-with-github-actions)
19. [Step 17: Add CloudFront CDN](#step-17-add-cloudfront-cdn)
20. [Free Tier Considerations](#free-tier-considerations)
21. [Troubleshooting](#troubleshooting)
22. [Cost Summary](#cost-summary)

---

## Prerequisites

Before starting, ensure you have:
- ✅ An **AWS Account** (Free Tier eligible)
- ✅ **AWS CLI** installed on your local machine
- ✅ **Docker** installed locally
- ✅ A domain registered on **Namecheap** (`unraveldocs.xyz`)
- ✅ Basic understanding of Linux commands

---

## AWS Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Internet                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ HTTPS (443)
┌─────────────────────────────────────────────────────────────────┐
│                    api.unraveldocs.xyz                          │
│                    (Namecheap DNS → EC2 Public IP)              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         EC2 Instance                             │
│  ┌─────────────────┐    ┌────────────────────────────────────┐  │
│  │     Nginx       │───▶│   Docker Container                 │  │
│  │  (Reverse Proxy)│    │   UnravelDocs API (:8080)          │  │
│  │  + SSL/Certbot  │    │   + Redis                          │  │
│  └─────────────────┘    │   + Kafka                          │  │
│                         │   + Elasticsearch                  │  │
│                         └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐
│   RDS PostgreSQL │  │    S3        │  │  Amazon MQ       │
│   (Database)     │  │  (Storage)   │  │  (RabbitMQ)      │
└──────────────────┘  └──────────────┘  └──────────────────┘
```

> [!NOTE]
> For Free Tier optimization, we'll run Redis, Kafka, and Elasticsearch as Docker containers on the EC2 instance instead of using managed AWS services.

---

## Step 1: AWS Account Setup

### 1.1 Create an AWS Account
If you don't have one, sign up at [aws.amazon.com](https://aws.amazon.com).

### 1.2 Install and Configure AWS CLI

**Windows (PowerShell):**
```powershell
# Download and install AWS CLI
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi

# Configure AWS CLI
aws configure
```

You'll be prompted to enter:
```
AWS Access Key ID [None]: YOUR_ACCESS_KEY
AWS Secret Access Key [None]: YOUR_SECRET_KEY
Default region name [None]: eu-north-1
Default output format [None]: json
```

### 1.3 Create IAM User for Deployment

1. Go to **IAM** → **Users** → **Add User**
2. Username: `unraveldocs-deploy`
3. Select **Programmatic access**
4. Attach policies:
   - `AmazonEC2FullAccess`
   - `AmazonRDSFullAccess`
   - `AmazonS3FullAccess`
   - `AmazonEC2ContainerRegistryFullAccess`
5. Save the **Access Key ID** and **Secret Access Key**

---

## Step 2: Set Up RDS (PostgreSQL)

### 2.1 Create RDS Instance (Free Tier)

1. Go to **RDS** → **Create Database**
2. Choose **Standard create**
3. **Engine options**: PostgreSQL
4. **Version**: PostgreSQL 16 or latest available
5. **Templates**: **Free tier** ✅

### 2.2 Settings
```
DB instance identifier: unraveldocs-db
Master username: postgres
Master password: [Create a strong password]
```

### 2.3 Instance Configuration
```
DB instance class: db.t3.micro (Free Tier eligible)
Storage type: General Purpose SSD (gp2)
Allocated storage: 20 GB
Enable storage autoscaling: NO (to stay in Free Tier)
```

### 2.4 Connectivity
```
VPC: Default VPC
Public access: Yes (for initial setup; disable later for security)
VPC security group: Create new
  - Name: unraveldocs-rds-sg
```

### 2.5 Database Authentication
```
Password authentication
```

### 2.6 Additional Configuration
```
Initial database name: unraveldocs
Enable automated backups: No (Free Tier consideration)
Enable Enhanced monitoring: No
```

Click **Create database** and wait ~5-10 minutes for it to be available.

### 2.7 Configure RDS Security Group

1. Go to **EC2** → **Security Groups**
2. Find `unraveldocs-rds-sg`
3. Edit **Inbound rules** → Add rule:
   ```
   Type: PostgreSQL
   Port: 5432
   Source: Custom - [Your EC2 Security Group ID]
   ```

### 2.8 Note Your RDS Endpoint
Once created, copy the **Endpoint** (e.g., `unraveldocs-db.xxxxx.eu-north-1.rds.amazonaws.com`)

---

## Step 3: Set Up ElastiCache (Redis)

> [!IMPORTANT]
> AWS ElastiCache is NOT Free Tier eligible. For cost savings, we'll run Redis as a Docker container on EC2. Skip to Step 4 if you prefer managed Redis.

### Option A: Redis on EC2 (Free - Recommended)

Redis will be deployed as part of the docker-compose on EC2. See Step 9.

### Option B: ElastiCache (Paid)

If you need production-grade Redis:
1. Go to **ElastiCache** → **Create cluster**
2. Choose **Redis OSS**
3. Cluster mode: Disabled
4. Node type: `cache.t3.micro` (~$12/month)
5. Number of replicas: 0
6. Subnet group: Create new
7. Security group: Create allowing port 6379 from EC2

---

## Step 4: Set Up Amazon MQ (RabbitMQ)

> [!IMPORTANT]
> Amazon MQ is NOT Free Tier eligible. For cost savings, we'll run RabbitMQ as a Docker container on EC2.

### Option A: RabbitMQ on EC2 (Free - Recommended)

RabbitMQ will be deployed as part of the docker-compose on EC2. See Step 9.

### Option B: Amazon MQ (Paid)

If you need production-grade message broker:
1. Go to **Amazon MQ** → **Create broker**
2. Broker engine: RabbitMQ
3. Broker instance type: `mq.t3.micro` (~$22/month)

---

## Step 5: Set Up ECR (Elastic Container Registry)

### 5.1 Create ECR Repository

```bash
# Create repository
aws ecr create-repository \
    --repository-name unraveldocs-api \
    --region eu-north-1 \
    --image-scanning-configuration scanOnPush=true
```

### 5.2 Note Your ECR URI
The output will include a `repositoryUri` like:
```
123456789012.dkr.ecr.eu-north-1.amazonaws.com/unraveldocs-api
```

> [!TIP]
> ECR Free Tier includes 500 MB of storage per month.

---

## Step 6: Launch EC2 Instance

### 6.1 Launch Instance

1. Go to **EC2** → **Launch Instance**
2. **Name**: `unraveldocs-server`

### 6.2 Application and OS Images
```
Amazon Machine Image: Amazon Linux 2023 AMI (Free Tier eligible)
Architecture: 64-bit (x86)
```

### 6.3 Instance Type
```
Instance type: t2.micro (Free Tier eligible - 750 hrs/month)
```

> [!CAUTION]
> `t2.micro` has only 1 GB RAM. Running all services (API, Redis, Kafka, Elasticsearch) may require upgrading to `t3.small` or `t3.medium` for production. Monitor memory usage.

### 6.4 Key Pair
```
Create new key pair:
  - Name: unraveldocs-key
  - Type: RSA
  - Format: .pem (for Linux/Mac) or .ppk (for PuTTY on Windows)
```
**Download and save this file securely!**

### 6.5 Network Settings

Edit Security Group:
```
Security group name: unraveldocs-ec2-sg

Inbound rules:
┌─────────────┬──────────┬────────────────┐
│ Type        │ Port     │ Source         │
├─────────────┼──────────┼────────────────┤
│ SSH         │ 22       │ My IP          │
│ HTTP        │ 80       │ 0.0.0.0/0      │
│ HTTPS       │ 443      │ 0.0.0.0/0      │
│ Custom TCP  │ 8080     │ 0.0.0.0/0      │
└─────────────┴──────────┴────────────────┘
```

### 6.6 Configure Storage
```
Storage: 30 GB gp3 (Free Tier allows up to 30 GB)
```

### 6.7 Launch Instance

Click **Launch Instance** and wait for it to start.

### 6.8 Allocate Elastic IP (Optional but Recommended)

1. Go to **EC2** → **Elastic IPs** → **Allocate**
2. **Actions** → **Associate Elastic IP address**
3. Select your instance
4. Click **Associate**

> [!NOTE]
> Elastic IPs are free as long as they're associated with a running instance.

---

## Step 7: Install Docker on EC2

### 7.1 Connect to EC2 via SSH

**Linux/Mac:**
```bash
chmod 400 unraveldocs-key.pem
ssh -i unraveldocs-key.pem ec2-user@YOUR_EC2_PUBLIC_IP
```

**Windows (Git Bash):**
```bash
ssh -i unraveldocs-key.pem ec2-user@YOUR_EC2_PUBLIC_IP
```

**Windows (PuTTY):**
- Convert `.pem` to `.ppk` using PuTTYgen
- Use PuTTY with the `.ppk` file

### 7.2 Update System and Install Docker

```bash
# Update system packages
sudo dnf update -y

# Install Docker
sudo dnf install docker -y

# Start Docker and enable on boot
sudo systemctl start docker
sudo systemctl enable docker

# Add ec2-user to docker group
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installations
docker --version
docker-compose --version

# IMPORTANT: Log out and log back in for group changes to take effect
exit
```

Reconnect via SSH after logging out.

---

## Step 8: Push Docker Image to ECR

### 8.1 Build the Docker Image (Local Machine)

From your project root directory:

```bash
cd c:\Users\afiaa\Desktop\projects\Brints\unraveldocs-api

# Build the Docker image
docker build -t unraveldocs-api:latest .
```

### 8.2 Authenticate Docker with ECR

```bash
# Get login password and authenticate
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.eu-north-1.amazonaws.com
```

Replace `123456789012` with your AWS Account ID.

### 8.3 Tag and Push Image

```bash
# Tag the image for ECR
docker tag unraveldocs-api:latest 123456789012.dkr.ecr.eu-north-1.amazonaws.com/unraveldocs-api:latest

# Push to ECR
docker push 123456789012.dkr.ecr.eu-north-1.amazonaws.com/unraveldocs-api:latest
```

---

## Step 9: Pull and Run Container on EC2

### 9.1 Configure AWS CLI on EC2

```bash
# SSH into EC2
ssh -i unraveldocs-key.pem ec2-user@YOUR_EC2_PUBLIC_IP

# Configure AWS CLI
aws configure
# Enter your Access Key, Secret Key, Region (eu-north-1), and output format (json)
```

### 9.2 Authenticate Docker with ECR on EC2

```bash
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.eu-north-1.amazonaws.com
```

### 9.3 Create Production docker-compose.yml

Create the application directory:
```bash
mkdir -p ~/unraveldocs
cd ~/unraveldocs
```

Create `docker-compose.yml`:
```bash
nano docker-compose.yml
```

Paste the following:

```yaml
version: '3.8'

services:
  unraveldocs-api:
    image: 123456789012.dkr.ecr.eu-north-1.amazonaws.com/unraveldocs-api:latest
    container_name: unraveldocs-api
    restart: unless-stopped
    ports:
      - "8080:8080"
    depends_on:
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=production
    networks:
      - unraveldocs-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  redis:
    image: redis:7-alpine
    container_name: redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - unraveldocs-net

  kafka:
    image: apache/kafka:3.7.0
    container_name: kafka
    restart: unless-stopped
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,EXTERNAL://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092"]
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 30s
    networks:
      - unraveldocs-net

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
      - cluster.name=unraveldocs-cluster
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    networks:
      - unraveldocs-net

volumes:
  redis_data:
    driver: local
  kafka_data:
    driver: local
  elasticsearch_data:
    driver: local

networks:
  unraveldocs-net:
    driver: bridge
```

Press `Ctrl+X`, then `Y`, then `Enter` to save.

### 9.4 Create Production .env File

```bash
nano .env
```

Paste your production environment variables:

```bash
# ===========================================
# UnravelDocs API - Production Environment
# ===========================================

# ==================== Application ====================
APP_BASE_URL=https://api.unraveldocs.xyz
APP_SUPPORT_EMAIL=support@unraveldocs.xyz
APP_UNSUBSCRIBE_URL=https://api.unraveldocs.xyz/unsubscribe
APP_FRONTEND_URL=https://unraveldocs.xyz

# ==================== Database (RDS) ====================
POSTGRES_HOST=unraveldocs-db.xxxxx.eu-north-1.rds.amazonaws.com
POSTGRES_PORT=5432
POSTGRES_DB=unraveldocs
POSTGRES_USER=postgres
POSTGRES_PASSWORD=YOUR_RDS_PASSWORD

SPRING_DATASOURCE_URL=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}

# ==================== Redis ====================
REDIS_HOST=redis
REDIS_PORT=6379
SPRING_DATA_REDIS_URL=redis://${REDIS_HOST}:${REDIS_PORT}

# ==================== Kafka ====================
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# ==================== Elasticsearch ====================
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200
SPRING_ELASTICSEARCH_URIS=http://${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}

# ==================== AWS S3 ====================
AWS_S3_REGION=eu-north-1
AWS_ACCESS_KEY=YOUR_AWS_ACCESS_KEY
AWS_SECRET_KEY=YOUR_AWS_SECRET_KEY
AWS_S3_BUCKET=your-s3-bucket-name
# Remove endpoint for production (uses real S3)
# AWS_S3_ENDPOINT=

# ==================== JWT ====================
APP_JWT_SECRET=YOUR_VERY_LONG_SECURE_JWT_SECRET_KEY_256_BITS
APP_JWT_EXPIRATION_MS=3600000
APP_JWT_REFRESH_EXPIRATION_MS=2592000000

# ==================== Stripe ====================
STRIPE_API_KEY=sk_live_YOUR_STRIPE_LIVE_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_WEBHOOK_SECRET
STRIPE_CURRENCY=usd
STRIPE_CHECKOUT_SUCCESS_URL=https://unraveldocs.xyz/payment-success
STRIPE_CHECKOUT_CANCEL_URL=https://unraveldocs.xyz/payment-cancel

# ==================== Admin ====================
APP_ADMIN_EMAIL=admin@unraveldocs.xyz
APP_ADMIN_PASSWORD=YOUR_SECURE_ADMIN_PASSWORD

# ==================== OCR Configuration ====================
OCR_DEFAULT_PROVIDER=tesseract
TESSERACT_DATAPATH=/usr/share/tesseract-ocr/5/tessdata

# Add other environment variables as needed...
```

### 9.5 Pull and Start Services

```bash
# Pull the latest image
docker-compose pull

# Start all services in detached mode
docker-compose up -d

# View logs
docker-compose logs -f unraveldocs-api

# Check running containers
docker ps
```

---

## Step 10: Configure Nginx as Reverse Proxy

### 10.1 Install Nginx

```bash
sudo dnf install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 10.2 Configure Nginx

```bash
sudo nano /etc/nginx/conf.d/unraveldocs.conf
```

Paste:

```nginx
server {
    listen 80;
    server_name api.unraveldocs.xyz;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
        
        # For file uploads
        client_max_body_size 50M;
    }
}
```

### 10.3 Test and Reload Nginx

```bash
# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

---

## Step 11: Set Up SSL with Certbot

### 11.1 Install Certbot

```bash
sudo dnf install certbot python3-certbot-nginx -y
```

### 11.2 Obtain SSL Certificate

> [!IMPORTANT]
> Before running this command, ensure your DNS is already pointing to your EC2 IP (see Step 12).

```bash
sudo certbot --nginx -d api.unraveldocs.xyz
```

You'll be prompted to:
1. Enter your email address
2. Agree to Terms of Service
3. Choose whether to redirect HTTP to HTTPS (choose **Yes**)

### 11.3 Verify SSL Auto-Renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Check renewal timer
sudo systemctl status certbot-renew.timer
```

### 11.4 Verify Final Nginx Configuration

After Certbot modifies the config, it should look like:

```bash
sudo cat /etc/nginx/conf.d/unraveldocs.conf
```

```nginx
server {
    server_name api.unraveldocs.xyz;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
        client_max_body_size 50M;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/api.unraveldocs.xyz/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.unraveldocs.xyz/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if ($host = api.unraveldocs.xyz) {
        return 301 https://$host$request_uri;
    }

    listen 80;
    server_name api.unraveldocs.xyz;
    return 404;
}
```

---

## Step 12: Configure Namecheap DNS

### 12.1 Log in to Namecheap

1. Go to [namecheap.com](https://namecheap.com) and log in
2. Navigate to **Domain List** → **Manage** (for unraveldocs.xyz)

### 12.2 Add DNS Records

Go to **Advanced DNS** tab and add the following records:

| Type | Host | Value | TTL |
|------|------|-------|-----|
| A Record | api | YOUR_EC2_ELASTIC_IP | Automatic |
| A Record | @ | YOUR_EC2_ELASTIC_IP | Automatic |

**Example:**
```
Type: A Record
Host: api
Value: 52.123.45.67  (your EC2 Elastic IP)
TTL: Automatic
```

### 12.3 Wait for DNS Propagation

DNS changes can take 5-30 minutes (sometimes up to 48 hours). You can check propagation at [dnschecker.org](https://dnschecker.org/#A/api.unraveldocs.xyz).

### 12.4 Verify DNS

```bash
# On your local machine
nslookup api.unraveldocs.xyz

# Or
dig api.unraveldocs.xyz
```

---

## Step 13: Production Environment Variables

Here's a complete production `.env` reference:

```bash
# ===========================================
# UnravelDocs API - Complete Production Config
# ===========================================

# ==================== Core Application ====================
SPRING_PROFILES_ACTIVE=production
APP_BASE_URL=https://api.unraveldocs.xyz
APP_SUPPORT_EMAIL=support@unraveldocs.xyz
APP_UNSUBSCRIBE_URL=https://api.unraveldocs.xyz/unsubscribe
APP_FRONTEND_URL=https://unraveldocs.xyz

# ==================== Database (AWS RDS) ====================
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/unraveldocs
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=YOUR_SECURE_RDS_PASSWORD

# ==================== Redis (Docker on EC2) ====================
SPRING_DATA_REDIS_URL=redis://redis:6379

# ==================== Kafka (Docker on EC2) ====================
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# ==================== Elasticsearch (Docker on EC2) ====================
SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200

# ==================== AWS S3 ====================
AWS_S3_REGION=eu-north-1
AWS_ACCESS_KEY=YOUR_ACCESS_KEY
AWS_SECRET_KEY=YOUR_SECRET_KEY
AWS_S3_BUCKET=unraveldocs-production
AWS_FROM_EMAIL=no-reply@unraveldocs.xyz

# ==================== Security ====================
APP_JWT_SECRET=YOUR_256_BIT_SECRET_KEY_MINIMUM_32_CHARACTERS
APP_JWT_EXPIRATION_MS=3600000
APP_JWT_REFRESH_EXPIRATION_MS=2592000000

# ==================== Stripe (Production) ====================
STRIPE_API_KEY=sk_live_XXXXXXXXXXXXXXXXXXXXXXXX
STRIPE_WEBHOOK_SECRET=whsec_XXXXXXXXXXXXXXXX
STRIPE_CURRENCY=usd
STRIPE_CHECKOUT_SUCCESS_URL=https://unraveldocs.xyz/payment/success
STRIPE_CHECKOUT_CANCEL_URL=https://unraveldocs.xyz/payment/cancel

# ==================== Mailgun ====================
MAILGUN_API_KEY=YOUR_MAILGUN_API_KEY
MAILGUN_FROM_EMAIL=no-reply@unraveldocs.xyz
MAILGUN_DOMAIN=unraveldocs.xyz

# ==================== Cloudinary ====================
CLOUDINARY_CLOUD_NAME=YOUR_CLOUD_NAME
CLOUDINARY_API_KEY=YOUR_API_KEY
CLOUDINARY_API_SECRET=YOUR_API_SECRET

# ==================== Admin ====================
APP_ADMIN_EMAIL=admin@unraveldocs.xyz
APP_ADMIN_PASSWORD=YOUR_SECURE_ADMIN_PASSWORD

# ==================== OCR ====================
OCR_DEFAULT_PROVIDER=tesseract
TESSERACT_DATAPATH=/usr/share/tesseract-ocr/5/tessdata
OCR_FALLBACK_ENABLED=true
OCR_ENABLED_PROVIDERS=TESSERACT
```

---

## Free Tier Considerations

### What's Included in AWS Free Tier (12 months)

| Service | Free Tier Limit | Your Usage |
|---------|-----------------|------------|
| **EC2** | 750 hrs/month t2.micro | ✅ Using t2.micro |
| **RDS** | 750 hrs/month db.t3.micro, 20GB | ✅ Using db.t3.micro |
| **S3** | 5GB storage, 20,000 GET requests | ✅ Depends on usage |
| **ECR** | 500 MB storage | ✅ ~200-300MB per image |
| **Data Transfer** | 100 GB outbound | ⚠️ Monitor usage |

### NOT Free Tier

| Service | Cost | Alternative |
|---------|------|-------------|
| **ElastiCache Redis** | ~$12/month | Run on EC2 (Docker) |
| **Amazon MQ RabbitMQ** | ~$22/month | Run on EC2 (Docker) |
| **Elastic IP** | Free when attached | ✅ |
| **NAT Gateway** | ~$32/month | Not needed for this setup |

### Monthly Cost Estimate (Free Tier)
- EC2 t2.micro: **$0** (within free tier)
- RDS db.t3.micro: **$0** (within free tier)
- S3: **$0** (within free tier)
- ECR: **$0** (within free tier)
- **Total: ~$0/month** (first 12 months)

### ⚠️ Memory Considerations

> [!WARNING]
> t2.micro has only **1 GB RAM**. Running:
> - Spring Boot API: ~512 MB
> - Redis: ~50-100 MB
> - Kafka: ~256-512 MB
> - Elasticsearch: ~256 MB (reduced from default)
>
> **Total: ~1+ GB** - May cause issues!

**Solutions:**
1. **Upgrade to t3.small** ($8.50/month) - 2 GB RAM
2. **Upgrade to t3.medium** ($17/month) - 4 GB RAM (recommended for production)
3. **Use Swap space** (See below)

### Adding Swap Space (If Using t2.micro)

```bash
# Create 2GB swap file
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Make it persistent
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Verify
free -h
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Connection Refused to RDS
```bash
# Check EC2 can reach RDS
telnet YOUR_RDS_ENDPOINT 5432

# Solutions:
# - Ensure RDS security group allows EC2 security group
# - Verify RDS is publicly accessible (or in same VPC)
```

#### 2. Docker Container Keeps Restarting
```bash
# Check logs
docker logs unraveldocs-api --tail 100

# Common causes:
# - Database connection failed
# - Invalid environment variables
# - Out of memory (add swap)
```

#### 3. SSL Certificate Issues
```bash
# Re-run Certbot
sudo certbot --nginx -d api.unraveldocs.xyz --force-renewal

# Check Nginx config
sudo nginx -t
```

#### 4. 502 Bad Gateway
```bash
# Check if API is running
docker ps
curl http://localhost:8080/actuator/health

# Check Nginx logs
sudo tail -f /var/log/nginx/error.log
```

#### 5. Out of Memory
```bash
# Check memory usage
free -h
docker stats

# Stop unnecessary services or add swap
```

#### 6. ECR Push Permission Denied
```bash
# Re-authenticate
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin YOUR_ECR_URI
```

### Useful Commands

```bash
# View all container logs
docker-compose logs -f

# Restart a specific service
docker-compose restart unraveldocs-api

# Stop all services
docker-compose down

# Start services
docker-compose up -d

# Update to latest image
docker-compose pull && docker-compose up -d

# Check disk space
df -h

# Monitor system resources
htop  # (install with: sudo dnf install htop -y)
```

---

## Next Steps

After deployment, consider implementing the following advanced features:

1. ✅ **Set up CloudWatch** for monitoring and alerts (Step 14)
2. ✅ **Configure Auto Scaling** if traffic increases (Step 15)
3. ✅ **Set up CI/CD** with GitHub Actions to auto-deploy (Step 16)
4. ✅ **Add CloudFront CDN** for better global performance (Step 17)
5. ⬜ **Enable RDS backups** for data protection
6. ⬜ **Set up Route 53** for DNS management (optional)

---

## Step 14: Set Up CloudWatch Monitoring

CloudWatch provides monitoring and observability for your AWS resources.

### 14.1 Install CloudWatch Agent on EC2

```bash
# SSH into your EC2 instance
ssh -i unraveldocs-key.pem ubuntu@YOUR_EC2_IP

# Download CloudWatch Agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb

# Install the agent
sudo dpkg -i amazon-cloudwatch-agent.deb
```

### 14.2 Create IAM Role for CloudWatch

1. Go to **IAM** → **Roles** → **Create role**
2. Select **AWS service** → **EC2**
3. Attach policies:
   - `CloudWatchAgentServerPolicy`
   - `AmazonSSMManagedInstanceCore`
4. Name: `UnravelDocs-EC2-CloudWatch-Role`
5. Attach role to your EC2 instance:
   - Go to **EC2** → Select instance → **Actions** → **Security** → **Modify IAM role**
   - Select the role and save

### 14.3 Configure CloudWatch Agent

Create the configuration file:

```bash
sudo nano /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

Paste:

```json
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "namespace": "UnravelDocs",
    "metrics_collected": {
      "cpu": {
        "measurement": ["cpu_usage_idle", "cpu_usage_user", "cpu_usage_system"],
        "metrics_collection_interval": 60,
        "totalcpu": true
      },
      "disk": {
        "measurement": ["used_percent"],
        "metrics_collection_interval": 60,
        "resources": ["/"]
      },
      "mem": {
        "measurement": ["mem_used_percent"],
        "metrics_collection_interval": 60
      },
      "swap": {
        "measurement": ["swap_used_percent"],
        "metrics_collection_interval": 60
      }
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/syslog",
            "log_group_name": "/unraveldocs/ec2/syslog",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/nginx/access.log",
            "log_group_name": "/unraveldocs/nginx/access",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/nginx/error.log",
            "log_group_name": "/unraveldocs/nginx/error",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  }
}
```

### 14.4 Start CloudWatch Agent

```bash
# Start the agent with the config
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s

# Check agent status
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a status
```

### 14.5 Create CloudWatch Alarms

Go to **CloudWatch** → **Alarms** → **Create alarm**:

#### CPU Alarm
```
Metric: UnravelDocs → cpu_usage_user
Condition: Greater than 80% for 5 minutes
Action: Send notification to SNS topic
```

#### Memory Alarm
```
Metric: UnravelDocs → mem_used_percent
Condition: Greater than 85% for 5 minutes
Action: Send notification to SNS topic
```

#### Disk Alarm
```
Metric: UnravelDocs → disk_used_percent
Condition: Greater than 80% for 5 minutes
Action: Send notification to SNS topic
```

### 14.6 Create SNS Topic for Alerts

```bash
# Create SNS topic
aws sns create-topic --name unraveldocs-alerts

# Subscribe your email
aws sns subscribe \
  --topic-arn arn:aws:sns:eu-central-1:YOUR_ACCOUNT_ID:unraveldocs-alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

### 14.7 View Metrics in CloudWatch Console

1. Go to **CloudWatch** → **Metrics** → **All metrics**
2. Select **UnravelDocs** namespace
3. Create a **Dashboard** for easy monitoring

---

## Step 15: Configure Auto Scaling

Auto Scaling automatically adjusts capacity based on demand.

> [!IMPORTANT]
> Auto Scaling requires an AMI and Launch Template. It works best with stateless applications. Since you're using Docker with external RDS, this should work well.

### 15.1 Create AMI from Current EC2

1. Go to **EC2** → **Instances** → Select your instance
2. **Actions** → **Image and templates** → **Create image**
3. Settings:
   ```
   Image name: unraveldocs-ami-v1
   Description: UnravelDocs API with Docker, Nginx, and Certbot
   No reboot: ☑️ (to avoid downtime)
   ```
4. Click **Create image** and wait for it to be available

### 15.2 Create Launch Template

1. Go to **EC2** → **Launch Templates** → **Create launch template**

```
Template name: unraveldocs-launch-template
Template version description: v1

Amazon Machine Image: Select your AMI (unraveldocs-ami-v1)
Instance type: t3.small (or t2.micro for Free Tier testing)

Key pair: unraveldocs-key
Security groups: unraveldocs-ec2-sg

Advanced details:
  IAM instance profile: UnravelDocs-EC2-CloudWatch-Role
  User data: (see below)
```

**User Data Script** (runs on instance launch):

```bash
#!/bin/bash
cd /home/ubuntu/unraveldocs

# Login to ECR
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 315084008680.dkr.ecr.eu-central-1.amazonaws.com

# Pull latest image
docker-compose pull

# Start services
docker-compose up -d

# Restart Nginx
sudo systemctl restart nginx
```

### 15.3 Create Auto Scaling Group

1. Go to **EC2** → **Auto Scaling Groups** → **Create**

```
Name: unraveldocs-asg
Launch template: unraveldocs-launch-template

VPC: Default VPC
Subnets: Select multiple availability zones

Load balancing: Attach to new load balancer
  Load balancer type: Application Load Balancer
  Load balancer name: unraveldocs-alb
  Scheme: Internet-facing
  Listeners: HTTPS:443, HTTP:80
  Target group: Create new → unraveldocs-tg → HTTP:8080 → Health check: /actuator/health

Group size:
  Desired capacity: 1
  Minimum capacity: 1
  Maximum capacity: 3

Scaling policies:
  Target tracking scaling policy
  Metric type: Average CPU utilization
  Target value: 70%
```

### 15.4 Update DNS to Point to Load Balancer

After creating the ALB:
1. Get the ALB DNS name (e.g., `unraveldocs-alb-123456.eu-central-1.elb.amazonaws.com`)
2. Go to **Namecheap** → **Advanced DNS**
3. Change the A Record for `api` to a **CNAME Record**:
   ```
   Type: CNAME Record
   Host: api
   Value: unraveldocs-alb-123456.eu-central-1.elb.amazonaws.com
   TTL: Automatic
   ```

> [!NOTE]
> For Free Tier, you may want to keep the single EC2 instance without Auto Scaling to avoid additional costs. Configure Auto Scaling when you need to scale.

---

## Step 16: Set Up CI/CD with GitHub Actions

Your project already has a `deploy.yml` workflow. Here's how to configure it:

### 16.1 Required GitHub Secrets

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add the following secrets:

| Secret Name | Description | Example Value |
|-------------|-------------|---------------|
| `AWS_ACCESS_KEY` | IAM user access key | `AKIA...` |
| `AWS_SECRET_KEY` | IAM user secret key | `wJalr...` |
| `EC2_HOST` | EC2 public IP or Elastic IP | `52.123.45.67` |
| `EC2_USERNAME` | SSH username | `ubuntu` |
| `EC2_SSH_KEY` | Private key content (entire .pem file) | `-----BEGIN RSA PRIVATE KEY-----...` |

### 16.2 Your Existing deploy.yml

Your workflow at `.github/workflows/deploy.yml` is already configured:

```yaml
name: Deploy to AWS EC2

on:
  push:
    branches:
      - main

env:
  AWS_REGION: eu-central-1
  ECR_REPOSITORY: unraveldocs-api
  ECR_REGISTRY: 315084008680.dkr.ecr.eu-central-1.amazonaws.com/unraveldocs

jobs:
  deploy:
    name: Build, Push and Deploy
    runs-on: ubuntu-latest
    environment: production

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_KEY }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Build, tag, and push image to Amazon ECR
        id: build-image
        env:
          IMAGE_TAG: latest
        run: |
          docker build -t $ECR_REPOSITORY .
          docker tag $ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          echo "image=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG" >> $GITHUB_OUTPUT

      - name: Deploy to EC2 instance
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USERNAME }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            aws ecr get-login-password --region ${{ env.AWS_REGION }} | docker login --username AWS --password-stdin ${{ env.ECR_REGISTRY }}
            docker pull ${{ steps.build-image.outputs.image }}
            export AWS_REGION=${{ env.AWS_REGION }}
            export ECR_REGISTRY=${{ env.ECR_REGISTRY }}
            export ECR_REPOSITORY=${{ env.ECR_REPOSITORY }}
            ./run.sh
```

### 16.3 Create run.sh on EC2

SSH into your EC2 and create the deployment script:

```bash
nano ~/run.sh
```

Paste:

```bash
#!/bin/bash
set -e

echo "🚀 Starting deployment..."

cd ~/unraveldocs

# Pull latest images
echo "📦 Pulling latest images..."
docker-compose pull

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose down

# Start new containers
echo "▶️ Starting new containers..."
docker-compose up -d

# Wait for health check
echo "⏳ Waiting for health check..."
sleep 30

# Check if API is healthy
if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo "✅ Deployment successful! API is healthy."
else
    echo "❌ Health check failed. Rolling back..."
    docker-compose logs --tail=50 unraveldocs-api
    exit 1
fi

# Clean up old images
echo "🧹 Cleaning up old images..."
docker image prune -f

echo "🎉 Deployment complete!"
```

Make it executable:

```bash
chmod +x ~/run.sh
```

### 16.4 Create GitHub Environment (Optional)

For additional protection:

1. Go to **Settings** → **Environments** → **New environment**
2. Name: `production`
3. Add protection rules:
   - Required reviewers (optional)
   - Wait timer (optional)

### 16.5 Test the Pipeline

1. Make a small change to your code
2. Commit and push to `main` branch
3. Go to **Actions** tab to watch the deployment
4. Verify at `https://api.unraveldocs.xyz/actuator/health`

---

## Step 17: Add CloudFront CDN

CloudFront improves performance by caching content at edge locations worldwide.

> [!NOTE]
> CloudFront is NOT in the Free Tier for new accounts (after 12 months). However, it offers 1 TB of data transfer out and 10 million HTTP/HTTPS requests per month free for the first year.

### 17.1 Create CloudFront Distribution

1. Go to **CloudFront** → **Create distribution**

### 17.2 Origin Settings

```
Origin domain: api.unraveldocs.xyz
Protocol: HTTPS only
HTTPS port: 443
Minimum origin SSL protocol: TLSv1.2

Origin path: (leave empty)
Name: unraveldocs-api-origin

Add custom header (optional):
  Header name: X-Origin-Verify
  Value: your-secret-value (for origin protection)
```

### 17.3 Default Cache Behavior

```
Path pattern: Default (*)
Compress objects automatically: Yes

Viewer:
  Viewer protocol policy: Redirect HTTP to HTTPS
  Allowed HTTP methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE
  
Cache key and origin requests:
  Cache policy: CachingDisabled (for API - dynamic content)
  Origin request policy: AllViewer
  
Response headers policy: (optional) SimpleCORS
```

### 17.4 Settings

```
Price class: Use only North America and Europe (cheaper)
Alternate domain name (CNAME): cdn-api.unraveldocs.xyz
Custom SSL certificate: Request or import certificate from ACM
Default root object: (leave empty for API)
```

### 17.5 Request ACM Certificate

For CloudFront, you MUST use **us-east-1** region for certificates:

1. Go to **ACM** (Certificate Manager) in **us-east-1** region
2. Click **Request a certificate**
3. Domain: `cdn-api.unraveldocs.xyz` or `*.unraveldocs.xyz`
4. Validation method: DNS validation
5. Add the CNAME record to Namecheap as instructed
6. Wait for validation (usually 5-30 minutes)

### 17.6 Complete CloudFront Distribution

1. Go back to CloudFront distribution
2. Edit settings → Select your ACM certificate
3. Create distribution (takes 5-15 minutes to deploy)

### 17.7 Update DNS for CDN (Optional)

If you want to use CloudFront as the main entry point:

1. Get the CloudFront distribution domain (e.g., `d1234567890.cloudfront.net`)
2. In Namecheap, add/update:
   ```
   Type: CNAME Record
   Host: cdn-api
   Value: d1234567890.cloudfront.net
   TTL: Automatic
   ```

### 17.8 Configure Cache Behaviors for Static vs Dynamic

For API endpoints, you typically want:

| Path Pattern | Cache Policy | Use Case |
|--------------|--------------|----------|
| `/actuator/*` | CachingDisabled | Health checks |
| `/api/v1/*` | CachingDisabled | Dynamic API |
| `/swagger-ui/*` | CachingOptimized | Static docs (cacheable) |
| `/docs/*` | CachingOptimized | Static docs (cacheable) |

### 17.9 Verify CloudFront

```bash
# Test the CloudFront endpoint
curl -I https://d1234567890.cloudfront.net/actuator/health

# Check the X-Cache header
# "Hit from cloudfront" = cached
# "Miss from cloudfront" = fetched from origin
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────┐
│             UnravelDocs Deployment Cheatsheet           │
├─────────────────────────────────────────────────────────┤
│ SSH to EC2:                                             │
│   ssh -i unraveldocs-key.pem ubuntu@YOUR_IP             │
│                                                         │
│ Application Directory:                                  │
│   cd ~/unraveldocs                                      │
│                                                         │
│ Start/Stop:                                             │
│   docker-compose up -d                                  │
│   docker-compose down                                   │
│                                                         │
│ View Logs:                                              │
│   docker-compose logs -f unraveldocs-api                │
│                                                         │
│ Update Deployment:                                      │
│   docker-compose pull && docker-compose up -d           │
│                                                         │
│ Check Status:                                           │
│   docker ps                                             │
│   curl http://localhost:8080/actuator/health            │
│                                                         │
│ Nginx:                                                  │
│   sudo systemctl status nginx                           │
│   sudo nginx -t && sudo systemctl reload nginx          │
│                                                         │
│ SSL Certificate:                                        │
│   sudo certbot renew --dry-run                          │
│                                                         │
│ CloudWatch:                                             │
│   sudo /opt/aws/amazon-cloudwatch-agent/bin/\           │
│   amazon-cloudwatch-agent-ctl -a status                 │
│                                                         │
│ CI/CD:                                                  │
│   Push to main branch → Auto deploy                     │
│                                                         │
│ Domain:                                                 │
│   https://api.unraveldocs.xyz                           │
└─────────────────────────────────────────────────────────┘
```

---

## Cost Summary

| Service | Free Tier | After Free Tier |
|---------|-----------|-----------------|
| EC2 (t2.micro) | 750 hrs/month (12 mo) | ~$8.50/month |
| RDS (db.t3.micro) | 750 hrs/month (12 mo) | ~$15/month |
| ECR | 500 MB/month | $0.10/GB |
| S3 | 5 GB, 20K requests | ~$0.023/GB |
| CloudFront | 1 TB, 10M requests (12 mo) | ~$0.085/GB |
| CloudWatch | Basic metrics free | ~$3/month |
| Data Transfer | 100 GB/month | ~$0.09/GB |

**Estimated Monthly Cost (after Free Tier):** ~$30-50/month

---

**Congratulations!** 🎉 Your UnravelDocs API is now fully deployed with:
- ✅ EC2 with Docker containers
- ✅ RDS PostgreSQL database
- ✅ Nginx reverse proxy with SSL
- ✅ CloudWatch monitoring
- ✅ CI/CD with GitHub Actions
- ✅ Optional: Auto Scaling and CloudFront CDN

Your API is accessible at: **https://api.unraveldocs.xyz**

