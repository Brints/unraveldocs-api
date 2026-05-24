# System Metrics & Monitoring — Implementation Plan

## 1. Executive Summary

This document describes the end-to-end plan for adding **production-grade system metrics, monitoring, and alerting** to the UnravelDocs API hosted on AWS EC2. The stack is:

| Layer                     | Tool                                       | Purpose                                    |
|---------------------------|--------------------------------------------|--------------------------------------------|
| **Metrics Collection**    | Spring Boot Actuator + Micrometer          | Expose application & JVM metrics           |
| **Host Metrics**          | Node Exporter                              | CPU, memory, disk, network at the OS level |
| **Container Metrics**     | cAdvisor                                   | Per-container resource usage               |
| **Database Metrics**      | postgres_exporter                          | PostgreSQL query & connection metrics      |
| **Cache Metrics**         | redis_exporter                             | Redis hit-rate, memory, keyspace           |
| **Message Queue Metrics** | kafka_exporter (JMX / Kafka built-in)      | Broker health, consumer lag                |
| **Search Engine Metrics** | elasticsearch_exporter                     | Cluster health, indexing rate              |
| **Time-Series DB**        | Prometheus                                 | Scrape, store, and query all metrics       |
| **Visualization**         | Grafana                                    | Dashboards, graphs, alerting               |
| **Alerting**              | Grafana Alerting + AlertManager (optional) | Slack / Email / PagerDuty alerts           |

> [!NOTE]
> The project **already** includes `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, and custom Micrometer metrics classes for Kafka, OCR, and Payments. This plan builds on top of that existing instrumentation rather than replacing it.

---

## 2. Why Prometheus + Grafana (and Not Something Else)

| Alternative              | Reason Against                                                                                                                                                        |
|--------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Datadog / New Relic**  | Powerful but expensive SaaS. A `micrometer-registry-datadog` dependency is already present but disabled. If budget allows, it can be re-enabled alongside Prometheus. |
| **AWS CloudWatch**       | Great for infra-level metrics but has higher latency (1–5 min), limited querying via PromQL, and no native Grafana-quality dashboards.                                |
| **ELK Stack (existing)** | The project already runs Elasticsearch + Kibana for **logs/search**, not metrics. Keeping logs and metrics separate is a best practice.                               |

**Recommendation: Stick with Prometheus + Grafana.** It is industry-standard, free, self-hosted (no extra AWS bills), integrates perfectly with the existing Micrometer setup, and the entire Spring Boot ecosystem is built around it.

---

## 3. Architecture Overview

```
┌───────────────────────── AWS EC2 Instance ─────────────────────────┐
│                                                                     │
│  ┌─────────────────────────────────┐                                │
│  │   Spring Boot (UnravelDocs)     │                                │
│  │   :8080/actuator/prometheus     │◄──── Prometheus scrapes        │
│  └─────────────────────────────────┘                                │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Node Exporter│  │   cAdvisor   │  │ postgres_exporter        │  │
│  │    :9100     │  │    :8082     │  │    :9187                 │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────────┘  │
│         │                 │                      │                  │
│  ┌──────┴─────────────────┴──────────────────────┴──────────────┐  │
│  │                       Prometheus                              │  │
│  │                         :9090                                 │  │
│  │   (scrapes all exporters + app every 15s)                     │  │
│  └──────────────────────────┬────────────────────────────────────┘  │
│                             │                                       │
│  ┌──────────────────────────▼────────────────────────────────────┐  │
│  │                        Grafana                                │  │
│  │                         :3000                                 │  │
│  │   (dashboards, alerts, admin UI)                              │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │redis_exporter│  │kafka_exporter│  │ elasticsearch_exporter   │  │
│  │    :9121     │  │    :9308     │  │    :9114                 │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

All services communicate over the existing `unraveldocs-net` Docker bridge network.

---

## 4. Detailed Implementation Steps

### Phase 1 — Spring Boot Application Changes

#### 4.1 Update Actuator Configuration

**File:** `src/main/resources/application-production.properties`

```properties
# ==================== Actuator & Prometheus ====================
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.endpoints.web.base-path=/actuator
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# Tag every metric with the application name for multi-service filtering
management.metrics.tags.application=unraveldocs-api
management.metrics.tags.environment=production

# Enable histogram buckets for latency percentile queries in Prometheus
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.distribution.percentiles-histogram.http.client.requests=true

# SLA buckets for HTTP request duration (ms → seconds in Prometheus)
management.metrics.distribution.sla.http.server.requests=50ms,100ms,250ms,500ms,1s,5s
```

> [!IMPORTANT]
> The `prometheus` endpoint must also be added to the actuator exposure list. Currently, production only exposes `health,info,metrics`. The `/actuator/prometheus` endpoint is what Prometheus will scrape.

#### 4.2 Secure the Prometheus Endpoint

Since `/actuator/**` is already `permitAll()` in `SecurityConfig.java`, the Prometheus endpoint will be accessible. For production hardening:

**Option A (Recommended — Network-level):** Keep `permitAll()` and restrict access via AWS Security Group rules so that only the EC2 instance's internal Docker network can reach port 8080.

**Option B (Application-level):** Create a dedicated management port:

```properties
# Serve actuator on a separate internal-only port
management.server.port=9091
management.server.address=0.0.0.0
```

Then do **not** expose port 9091 outside the Docker network.

#### 4.3 Add Custom System Metrics Bean (Optional Enhancement)

Create a `SystemMetricsConfig.java` to register custom gauges for business-level KPIs:

**File:** `src/main/java/com/extractor/unraveldocs/config/SystemMetricsConfig.java`

```java
@Configuration
public class SystemMetricsConfig {

    @Bean
    MeterBinder customMetrics(UserRepository userRepo,
                               DocumentRepository docRepo) {
        return registry -> {
            // Total registered users
            Gauge.builder("app.users.total", userRepo, UserRepository::count)
                 .description("Total registered users")
                 .register(registry);

            // Total documents processed
            Gauge.builder("app.documents.total", docRepo, DocumentRepository::count)
                 .description("Total documents in the system")
                 .register(registry);
        };
    }
}
```

---

### Phase 2 — Docker Compose: Add Monitoring Stack

#### 4.4 Prometheus Service

Add to `docker-compose.yml`:

```yaml
  # --- MONITORING SERVICES ---
  prometheus:
    image: prom/prometheus:v3.3.0
    container_name: prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./monitoring/prometheus/alerts.yml:/etc/prometheus/alerts.yml:ro
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
    depends_on:
      unraveldocs-api:
        condition: service_started
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9090/-/ready"]
      interval: 15s
      timeout: 5s
      retries: 5
    networks:
      - unraveldocs-net
```

#### 4.5 Grafana Service

```yaml
  grafana:
    image: grafana/grafana:11.6.0
    container_name: grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=${GRAFANA_ADMIN_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_SERVER_ROOT_URL=${GRAFANA_ROOT_URL:-http://localhost:3000}
      - GF_INSTALL_PLUGINS=grafana-clock-panel,grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
      - ./monitoring/grafana/dashboards:/var/lib/grafana/dashboards:ro
    depends_on:
      prometheus:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:3000/api/health"]
      interval: 15s
      timeout: 5s
      retries: 5
    networks:
      - unraveldocs-net
```

#### 4.6 Node Exporter (Host/OS Metrics)

```yaml
  node-exporter:
    image: prom/node-exporter:v1.9.0
    container_name: node-exporter
    restart: unless-stopped
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    networks:
      - unraveldocs-net
```

#### 4.7 cAdvisor (Container Metrics)

```yaml
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:v0.51.0
    container_name: cadvisor
    restart: unless-stopped
    ports:
      - "8082:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
    privileged: true
    networks:
      - unraveldocs-net
```

#### 4.8 Database Exporter (PostgreSQL)

```yaml
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:v0.16.0
    container_name: postgres-exporter
    restart: unless-stopped
    ports:
      - "9187:9187"
    environment:
      DATA_SOURCE_NAME: "postgresql://${POSTGRES_USER:-postgres}:${POSTGRES_PASSWORD:-postgres}@postgres:5432/${POSTGRES_DB:-unraveldocs}?sslmode=disable"
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - unraveldocs-net
```

#### 4.9 Redis Exporter

```yaml
  redis-exporter:
    image: oliver006/redis_exporter:v1.67.0
    container_name: redis-exporter
    restart: unless-stopped
    ports:
      - "9121:9121"
    environment:
      REDIS_ADDR: "redis://redis:6379"
    depends_on:
      redis:
        condition: service_healthy
    networks:
      - unraveldocs-net
```

#### 4.10 Kafka Exporter

```yaml
  kafka-exporter:
    image: danielqsj/kafka-exporter:v1.9.0
    container_name: kafka-exporter
    restart: unless-stopped
    ports:
      - "9308:9308"
    command:
      - '--kafka.server=kafka:29092'
      - '--topic.filter=.*'
      - '--group.filter=.*'
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - unraveldocs-net
```

#### 4.11 Elasticsearch Exporter

```yaml
  elasticsearch-exporter:
    image: quay.io/prometheuscommunity/elasticsearch-exporter:v1.8.0
    container_name: elasticsearch-exporter
    restart: unless-stopped
    ports:
      - "9114:9114"
    command:
      - '--es.uri=http://elasticsearch:9200'
      - '--es.all'
      - '--es.indices'
      - '--es.shards'
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - unraveldocs-net
```

#### 4.12 Additional Volumes

Add to the `volumes:` section:

```yaml
  prometheus_data:
    driver: local
  grafana_data:
    driver: local
```

---

### Phase 3 — Prometheus Configuration

#### 4.13 Create Prometheus Config File

**File:** `monitoring/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s          # How often to scrape targets
  evaluation_interval: 15s      # How often to evaluate alert rules
  scrape_timeout: 10s

# Alert rules
rule_files:
  - "alerts.yml"

scrape_configs:
  # ---- UnravelDocs Spring Boot App ----
  - job_name: 'unraveldocs-api'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['unraveldocs-api:8080']
        labels:
          app: 'unraveldocs'
          environment: 'production'

  # ---- Prometheus Self-Monitoring ----
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # ---- Node Exporter (Host Metrics) ----
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
        labels:
          instance: 'ec2-host'

  # ---- cAdvisor (Container Metrics) ----
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
        labels:
          instance: 'docker-host'

  # ---- PostgreSQL Exporter ----
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
        labels:
          database: 'unraveldocs'

  # ---- Redis Exporter ----
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # ---- Kafka Exporter ----
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']

  # ---- Elasticsearch Exporter ----
  - job_name: 'elasticsearch'
    static_configs:
      - targets: ['elasticsearch-exporter:9114']
```

#### 4.14 Create Alert Rules

**File:** `monitoring/prometheus/alerts.yml`

```yaml
groups:
  # ========================
  # Host-Level Alerts
  # ========================
  - name: host_alerts
    rules:
      - alert: HighCpuUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on {{ $labels.instance }}"
          description: "CPU usage is above 85% for more than 5 minutes (current: {{ $value }}%)"

      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100 > 90
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High memory usage on {{ $labels.instance }}"
          description: "Memory usage is above 90% (current: {{ $value }}%)"

      - alert: DiskSpaceLow
        expr: (1 - (node_filesystem_avail_bytes{fstype!~"tmpfs|overlay"} / node_filesystem_size_bytes{fstype!~"tmpfs|overlay"})) * 100 > 85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Disk space low on {{ $labels.instance }}"
          description: "Disk usage is above 85% on {{ $labels.mountpoint }} (current: {{ $value }}%)"

      - alert: DiskSpaceCritical
        expr: (1 - (node_filesystem_avail_bytes{fstype!~"tmpfs|overlay"} / node_filesystem_size_bytes{fstype!~"tmpfs|overlay"})) * 100 > 95
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Disk space critically low on {{ $labels.instance }}"
          description: "Disk usage above 95% on {{ $labels.mountpoint }} (current: {{ $value }}%)"

  # ========================
  # Application Alerts
  # ========================
  - name: application_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP 5xx error rate"
          description: "More than 5% of requests are returning 5xx errors (current: {{ $value }})"

      - alert: HighResponseLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High P95 response latency"
          description: "P95 latency is above 2 seconds (current: {{ $value }}s)"

      - alert: ApplicationDown
        expr: up{job="unraveldocs-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "UnravelDocs API is down"
          description: "The application has been unreachable for more than 1 minute"

      - alert: JvmHeapUsageHigh
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap usage is high"
          description: "Heap memory usage above 85% (current: {{ $value }}%)"

      - alert: HikariPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max * 100 > 90
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "HikariCP connection pool is nearly exhausted"
          description: "Active connections are above 90% of pool max (current: {{ $value }}%)"

  # ========================
  # Database Alerts
  # ========================
  - name: database_alerts
    rules:
      - alert: PostgresDown
        expr: pg_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL is down"
          description: "PostgreSQL exporter cannot connect to the database"

      - alert: PostgresTooManyConnections
        expr: pg_stat_database_numbackends / pg_settings_max_connections * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL connections high"
          description: "More than 80% of max connections are in use (current: {{ $value }}%)"

      - alert: PostgresSlowQueries
        expr: rate(pg_stat_database_blk_read_time[5m]) > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL slow queries detected"
          description: "Block read time is elevated (current: {{ $value }}ms)"

  # ========================
  # Redis Alerts
  # ========================
  - name: redis_alerts
    rules:
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis is down"
          description: "Redis exporter cannot connect to the server"

      - alert: RedisHighMemory
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage is high"
          description: "Redis is using more than 85% of max memory (current: {{ $value }}%)"

  # ========================
  # Kafka Alerts
  # ========================
  - name: kafka_alerts
    rules:
      - alert: KafkaConsumerLag
        expr: kafka_consumergroup_lag_sum > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer group lag is high"
          description: "Consumer group {{ $labels.consumergroup }} has lag {{ $value }} on topic {{ $labels.topic }}"

      - alert: KafkaBrokerDown
        expr: kafka_brokers < 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "No Kafka brokers available"
          description: "Number of available brokers is {{ $value }}"

  # ========================
  # Elasticsearch Alerts
  # ========================
  - name: elasticsearch_alerts
    rules:
      - alert: ElasticsearchClusterRed
        expr: elasticsearch_cluster_health_status{color="red"} == 1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Elasticsearch cluster is RED"
          description: "Elasticsearch cluster health status is red — data may be unavailable"

      - alert: ElasticsearchClusterYellow
        expr: elasticsearch_cluster_health_status{color="yellow"} == 1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Elasticsearch cluster is YELLOW"
          description: "Replicas may not be assigned — not all data is redundant"
```

---

### Phase 4 — Grafana Provisioning (Auto-Configuration)

#### 4.15 Datasource Provisioning

**File:** `monitoring/grafana/provisioning/datasources/prometheus.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: '15s'
```

#### 4.16 Dashboard Provisioning

**File:** `monitoring/grafana/provisioning/dashboards/dashboards.yml`

```yaml
apiVersion: 1

providers:
  - name: 'UnravelDocs Dashboards'
    orgId: 1
    folder: 'UnravelDocs'
    folderUid: 'unraveldocs'
    type: file
    disableDeletion: false
    editable: true
    updateIntervalSeconds: 30
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: false
```

#### 4.17 Pre-Built Dashboards to Import

Create JSON dashboard files under `monitoring/grafana/dashboards/`. Community dashboard IDs to start with:

| Dashboard                              | Grafana ID | Purpose                               |
|----------------------------------------|------------|---------------------------------------|
| Spring Boot Statistics                 | `19004`    | JVM, HTTP, HikariCP, Actuator         |
| Node Exporter Full                     | `1860`     | CPU, memory, disk, network            |
| Docker Container Monitoring (cAdvisor) | `14282`    | Container CPU, memory, I/O            |
| PostgreSQL Database                    | `9628`     | Connections, queries, cache hit ratio |
| Redis Dashboard                        | `11835`    | Memory, commands, connections         |
| Kafka Exporter Overview                | `7589`     | Brokers, topics, consumer lag         |
| Elasticsearch Cluster                  | `2322`     | Cluster health, indexing, search      |

> [!TIP]
> These dashboards can be downloaded from [grafana.com/grafana/dashboards](https://grafana.com/grafana/dashboards) and placed as JSON files into the `monitoring/grafana/dashboards/` directory. They will be auto-loaded.

Additionally, create a **custom UnravelDocs Overview Dashboard** that aggregates key panels:
- API request rate & error rate
- OCR processing latency (p50/p95/p99) — uses existing `OcrMetrics`
- Kafka message throughput & consumer lag — uses existing `KafkaMetrics`
- Payment transaction counts & failures — uses existing `PaymentMetrics`
- Active users gauge — from the custom `SystemMetricsConfig`
- JVM heap usage & GC activity
- HikariCP pool utilization

---

### Phase 5 — Alerting Configuration

#### 4.18 Grafana Alert Notifications

Configure notification channels in Grafana for:

| Channel                  | Provider         | Use Case                    |
|--------------------------|------------------|-----------------------------|
| **Email**                | SMTP / AWS SES   | Low-severity warnings       |
| **Slack**                | Incoming Webhook | Warning + critical alerts   |
| **PagerDuty** (optional) | API Integration  | Critical on-call escalation |

Configuration via Grafana UI or provisioning file:

**File:** `monitoring/grafana/provisioning/alerting/notifications.yml`

```yaml
apiVersion: 1

contactPoints:
  - orgId: 1
    name: slack-alerts
    receivers:
      - uid: slack-1
        type: slack
        settings:
          url: ${SLACK_WEBHOOK_URL}
          recipient: '#unraveldocs-alerts'
          title: '{{ .CommonLabels.alertname }}'
          text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
        disableResolveMessage: false

  - orgId: 1
    name: email-alerts
    receivers:
      - uid: email-1
        type: email
        settings:
          addresses: ${GRAFANA_ALERT_EMAIL:-alerts@unraveldocs.xyz}
```

---

### Phase 6 — Security & Access Control

#### 4.19 Network Security

| Concern                                | Solution                                                                                       |
|----------------------------------------|------------------------------------------------------------------------------------------------|
| **Prometheus UI** (`:9090`)            | Do NOT expose to the internet. Restrict in AWS Security Group to internal/VPN only.            |
| **Grafana** (`:3000`)                  | Expose via HTTPS (NGINX reverse proxy or ALB). Require login with strong admin password.       |
| **Exporters** (`:9100`, `:9121`, etc.) | Internal only. Never expose outside the Docker network.                                        |
| **Actuator** `/actuator/prometheus`    | Already behind the Docker network. For extra safety, use a separate management port (see 4.2). |

#### 4.20 Grafana Authentication Hardening

```properties
# Add to .env file
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=<strong-generated-password>
GRAFANA_ROOT_URL=https://monitoring.unraveldocs.xyz
```

Consider enabling OAuth2 (Google) or LDAP for team access instead of shared credentials.

---

### Phase 7 — Directory Structure

After implementation, the new files/directories will be:

```
unraveldocs-api/
├── monitoring/
│   ├── prometheus/
│   │   ├── prometheus.yml              # Scrape configuration
│   │   └── alerts.yml                   # Alert rules
│   └── grafana/
│       ├── provisioning/
│       │   ├── datasources/
│       │   │   └── prometheus.yml       # Auto-register Prometheus datasource
│       │   ├── dashboards/
│       │   │   └── dashboards.yml       # Dashboard provisioning config
│       │   └── alerting/
│       │       └── notifications.yml    # Alert notification channels
│       └── dashboards/
│           ├── spring-boot.json         # Spring Boot / JVM dashboard
│           ├── node-exporter.json       # Host metrics dashboard
│           ├── docker-containers.json   # Container metrics dashboard
│           ├── postgresql.json          # Database dashboard
│           ├── redis.json               # Cache dashboard
│           ├── kafka.json               # Message queue dashboard
│           ├── elasticsearch.json       # Search engine dashboard
│           └── unraveldocs-overview.json # Custom application dashboard
├── docker-compose.yml                    # Updated with monitoring services
└── src/
    └── main/
        ├── resources/
        │   └── application-production.properties  # Updated actuator config
        └── java/.../config/
            └── SystemMetricsConfig.java  # Custom business metrics (optional)
```

---

## 5. Metrics Collected — Complete Reference

### 5.1 Application Metrics (Spring Boot Actuator + Micrometer)

| Category              | Metric Examples                                                                                 |
|-----------------------|-------------------------------------------------------------------------------------------------|
| **HTTP**              | `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, bucket histograms     |
| **JVM Memory**        | `jvm_memory_used_bytes`, `jvm_memory_max_bytes`, `jvm_buffer_memory_used_bytes`                 |
| **JVM GC**            | `jvm_gc_pause_seconds_count`, `jvm_gc_pause_seconds_sum`                                        |
| **JVM Threads**       | `jvm_threads_live_threads`, `jvm_threads_peak_threads`, `jvm_threads_daemon_threads`            |
| **HikariCP**          | `hikaricp_connections_active`, `hikaricp_connections_idle`, `hikaricp_connections_pending`      |
| **Kafka (custom)**    | `kafka.messaging.messages.sent`, `kafka.messaging.send.latency`, `kafka.messaging.messages.dlq` |
| **OCR (custom)**      | `ocr.requests.total`, `ocr.requests.duration`, `ocr.confidence`, `ocr.fallbacks.total`          |
| **Payments (custom)** | `payment.created`, `payment.failed`, `payment.webhook.processed`, `payment.api.latency`         |
| **Logback**           | `logback_events_total` by level (ERROR, WARN, INFO)                                             |
| **Process**           | `process_cpu_usage`, `process_uptime_seconds`, `process_files_open_files`                       |

### 5.2 Host Metrics (Node Exporter)

| Category    | Metrics                                                                 |
|-------------|-------------------------------------------------------------------------|
| **CPU**     | `node_cpu_seconds_total`, load averages                                 |
| **Memory**  | `node_memory_MemTotal_bytes`, `node_memory_MemAvailable_bytes`, swap    |
| **Disk**    | `node_filesystem_avail_bytes`, `node_disk_io_time_seconds_total`        |
| **Network** | `node_network_receive_bytes_total`, `node_network_transmit_bytes_total` |

### 5.3 Container Metrics (cAdvisor)

| Metric                                  | Description                    |
|-----------------------------------------|--------------------------------|
| `container_cpu_usage_seconds_total`     | CPU usage per container        |
| `container_memory_usage_bytes`          | Memory usage per container     |
| `container_network_receive_bytes_total` | Network I/O per container      |
| `container_fs_usage_bytes`              | Filesystem usage per container |

### 5.4 Database Metrics (postgres_exporter)

| Metric                           | Description                     |
|----------------------------------|---------------------------------|
| `pg_stat_database_numbackends`   | Active connections per database |
| `pg_stat_database_tup_fetched`   | Rows fetched                    |
| `pg_stat_database_tup_inserted`  | Rows inserted                   |
| `pg_stat_database_deadlocks`     | Deadlock count                  |
| `pg_stat_database_blk_read_time` | Block read time (slow queries)  |

### 5.5 Redis Metrics (redis_exporter)

| Metric                           | Description                        |
|----------------------------------|------------------------------------|
| `redis_connected_clients`        | Number of connected clients        |
| `redis_memory_used_bytes`        | Memory consumed                    |
| `redis_keyspace_hits_total`      | Successful key lookups (cache hit) |
| `redis_keyspace_misses_total`    | Failed key lookups (cache miss)    |
| `redis_commands_processed_total` | Total commands processed           |

### 5.6 Kafka Metrics (kafka_exporter)

| Metric                               | Description                |
|--------------------------------------|----------------------------|
| `kafka_brokers`                      | Number of brokers          |
| `kafka_topic_partitions`             | Partitions per topic       |
| `kafka_consumergroup_lag`            | Consumer lag per partition |
| `kafka_consumergroup_current_offset` | Current offset             |

### 5.7 Elasticsearch Metrics (elasticsearch_exporter)

| Metric                                     | Description                       |
|--------------------------------------------|-----------------------------------|
| `elasticsearch_cluster_health_status`      | Cluster health (green/yellow/red) |
| `elasticsearch_indices_docs_total`         | Total indexed documents           |
| `elasticsearch_jvm_memory_used_bytes`      | JVM memory in ES                  |
| `elasticsearch_indices_search_query_total` | Search queries executed           |

---

## 6. Deployment Checklist

### Pre-Deployment

- [ ] Create the `monitoring/` directory structure with all config files
- [ ] Update `docker-compose.yml` with all monitoring services and volumes
- [ ] Update `application-production.properties` with Prometheus actuator config
- [ ] Download community Grafana dashboard JSON files
- [ ] Generate strong Grafana admin password and add to `.env`
- [ ] Add `SLACK_WEBHOOK_URL` (if using Slack alerts) to `.env`

### Deployment

- [ ] SSH into the EC2 instance
- [ ] Pull the latest code: `git pull origin main`
- [ ] Create required config files in `monitoring/` (or they deploy with the code)
- [ ] Restart all services: `docker compose up -d --build`
- [ ] Verify Prometheus targets: visit `http://<ec2-ip>:9090/targets` — all should be `UP`
- [ ] Verify Grafana: visit `http://<ec2-ip>:3000` — login with admin credentials
- [ ] Check dashboards are auto-loaded and showing data
- [ ] Test an alert fires correctly (e.g., stop a service briefly)

### Post-Deployment

- [ ] Set up HTTPS for Grafana (NGINX reverse proxy or AWS ALB)
- [ ] Restrict Prometheus/exporter ports in Security Group (internal only)
- [ ] Configure alert channels (Slack, Email)
- [ ] Create Grafana user accounts for team members (viewer roles)
- [ ] Set up Grafana snapshot sharing if needed for external stakeholders

---

## 7. Resource Considerations for EC2

The monitoring stack adds overhead. Estimated resource usage:

| Service                | CPU           | Memory                         |
|------------------------|---------------|--------------------------------|
| Prometheus             | ~0.5 vCPU     | ~500 MB (grows with retention) |
| Grafana                | ~0.1 vCPU     | ~200 MB                        |
| Node Exporter          | Negligible    | ~20 MB                         |
| cAdvisor               | ~0.1 vCPU     | ~100 MB                        |
| postgres_exporter      | Negligible    | ~15 MB                         |
| redis_exporter         | Negligible    | ~10 MB                         |
| kafka_exporter         | Negligible    | ~15 MB                         |
| elasticsearch_exporter | Negligible    | ~15 MB                         |
| **Total Additional**   | **~0.8 vCPU** | **~875 MB**                    |

> [!WARNING]
> If the EC2 instance is a `t3.small` (2 vCPU, 2 GB RAM), this will be tight. Recommended minimum: **`t3.medium` (2 vCPU, 4 GB)** or ideally **`t3.large` (2 vCPU, 8 GB)** given the existing services already running (Postgres, Redis, Kafka, Elasticsearch, and the JVM app).

---

## 8. Future Enhancements

| Enhancement                   | Description                                                                                                                                           |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Loki + Promtail**           | Add log aggregation to Grafana for unified logs + metrics                                                                                             |
| **Tempo / Jaeger**            | Distributed tracing for request flows across services                                                                                                 |
| **Grafana OnCall**            | Incident management with escalation policies                                                                                                          |
| **Remote Prometheus Storage** | Use Thanos or Cortex for long-term metric retention beyond 30 days                                                                                    |
| **Custom SLO Dashboards**     | Define SLIs/SLOs for API availability and latency targets                                                                                             |
| **Admin Dashboard API**       | Build a Spring Boot endpoint (`/api/v1/admin/metrics`) that directly queries Prometheus and returns key KPIs as JSON for embedded frontend dashboards |
