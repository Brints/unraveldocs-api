# Brokers Package — Documentation

> **Package:** `com.extractor.unraveldocs.brokers`  
> **Last Updated:** February 26, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Package Structure](#package-structure)
3. [Architecture](#architecture)
4. [Core Abstractions](#core-abstractions)
   - [Message](#message)
   - [MessageProducer](#messageproducer)
   - [MessageConsumer](#messageconsumer)
   - [MessageResult](#messageresult)
   - [MessageBrokerFactory](#messagebrokerfactory)
   - [MessageBrokerType](#messagebrokertype)
   - [MessagingException](#messagingexception)
5. [Kafka Configuration](#kafka-configuration)
   - [MessagingProperties](#messagingproperties)
   - [KafkaProducerConfig](#kafkaproducerconfig)
   - [KafkaConsumerConfig](#kafkaconsumerconfig)
   - [KafkaTopicConfig](#kafkatopicconfig)
   - [EventHandlerConfiguration](#eventhandlerconfiguration)
6. [Topics Reference](#topics-reference)
7. [Event System](#event-system)
   - [BaseEvent & EventMetadata](#baseevent--eventmetadata)
   - [EventTypes](#eventtypes)
   - [EventHandler](#eventhandler)
   - [EventPublisherService](#eventpublisherservice)
8. [Kafka Producer](#kafka-producer)
   - [KafkaMessageProducer](#kafkamessageproducer)
9. [Kafka Consumer](#kafka-consumer)
   - [EmailKafkaConsumer](#emailkafkaconsumer)
10. [Message Schemas](#message-schemas)
    - [EmailNotificationMessage](#emailnotificationmessage)
    - [DocumentProcessingMessage](#documentprocessingmessage)
    - [PaymentEventMessage](#paymenteventmessage)
    - [UserEventMessage](#usereventmessage)
11. [Domain Producer Services](#domain-producer-services)
    - [EmailMessageProducerService](#emailmessageproducerservice)
    - [DocumentProcessingProducerService](#documentprocessingproducerservice)
    - [PaymentEventProducerService](#paymenteventproducerservice)
12. [Error Handling & Retry Strategy](#error-handling--retry-strategy)
    - [KafkaErrorHandler](#kafkaerrorhandler)
    - [Exponential Backoff & DLQ](#exponential-backoff--dlq)
13. [Observability](#observability)
    - [KafkaMetrics](#kafkametrics)
    - [KafkaHealthIndicator](#kafkahealthindicator)
    - [KafkaHealthEndpoint](#kafkahealthendpoint)
14. [Configuration Reference](#configuration-reference)
15. [Flow Diagrams](#flow-diagrams)

---

## Overview

The **Brokers** package is the central messaging infrastructure for UnravelDocs. It provides a **broker-agnostic abstraction layer** (`core/`) on top of a **Kafka implementation** (`kafka/`), with a design that allows alternative brokers (e.g., RabbitMQ) to be plugged in without changing any consumer code.

Key responsibilities:

| Capability | Description |
|---|---|
| **Abstraction** | `Message<T>`, `MessageProducer<T>`, `MessageConsumer<T>` contracts decouple domain code from any specific broker |
| **Kafka Implementation** | `KafkaMessageProducer` implements `MessageProducer`, backed by Spring's `KafkaTemplate` |
| **Topic Management** | 10 primary topics + retry topics + DLQ topics declared as Spring `@Bean`s; auto-created on startup |
| **Event Publishing** | `EventPublisherService` wraps `MessageBrokerFactory` for strongly-typed event routing |
| **Event Routing** | `EventHandlerConfiguration` maps `EventHandler` beans by type string for dynamic dispatch |
| **Email Queuing** | `EmailMessageProducerService` + `EmailKafkaConsumer` handle the full async email delivery pipeline |
| **Document Processing** | `DocumentProcessingProducerService` queues OCR and text-extraction jobs |
| **Payment Events** | `PaymentEventProducerService` publishes payment lifecycle events |
| **Error Resilience** | `DefaultErrorHandler` with exponential backoff; `KafkaErrorHandler` for programmatic DLQ routing; non-retryable exception list |
| **Observability** | `KafkaMetrics` (Micrometer counters + timers); `KafkaHealthIndicator` + `KafkaHealthEndpoint` (Actuator) |

> **Conditional Loading:** Every Kafka bean is annotated `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`. If `spring.kafka.bootstrap-servers` is not set in `application.properties`, the entire Kafka infrastructure is skipped gracefully.

---

## Package Structure

```
brokers/
├── config/
│   └── MessagingProperties.java             # @ConfigurationProperties(prefix = "messaging") — all tunable settings
├── core/                                    # Broker-agnostic abstractions (no Kafka dependency)
│   ├── Message.java                         # Generic immutable message record with factory methods
│   ├── MessageBrokerFactory.java            # Discovers and routes to registered MessageProducer beans
│   ├── MessageBrokerType.java               # Enum: KAFKA | RABBITMQ
│   ├── MessageConsumer.java                 # Interface + nested MessageHandler / BatchMessageHandler
│   ├── MessageProducer.java                 # Interface: send() (async) + sendAndWait() (sync)
│   ├── MessageResult.java                   # Immutable send-result record with factory methods
│   └── MessagingException.java             # RuntimeException wrapping broker-specific errors
├── kafka/
│   ├── config/
│   │   ├── EventHandlerConfiguration.java   # Builds Map<String, EventHandler<?>> from all EventHandler beans
│   │   ├── KafkaConsumerConfig.java         # ConsumerFactory, ConcurrentKafkaListenerContainerFactory, DefaultErrorHandler
│   │   ├── KafkaProducerConfig.java         # ProducerFactory, KafkaTemplate — idempotent, snappy-compressed
│   │   └── KafkaTopicConfig.java            # Declares all 30 Kafka topics (primary + retry + DLQ) as @Bean
│   ├── consumer/
│   │   └── EmailKafkaConsumer.java          # @KafkaListener — consumes unraveldocs-emails, delegates to EmailOrchestratorService
│   ├── events/
│   │   ├── BaseEvent.java                   # Generic event envelope: EventMetadata + typed payload
│   │   ├── EventHandler.java                # Interface: handleEvent(T) + getEventType()
│   │   ├── EventMetadata.java               # Event envelope header: type, source, timestamp, correlationId
│   │   ├── EventPublisherService.java       # Wraps MessageBrokerFactory; topic-specific publish methods
│   │   └── EventTypes.java                  # Constants for all event type strings
│   ├── handler/
│   │   └── KafkaErrorHandler.java           # Programmatic DLQ routing + retry-from-DLQ + retry-or-DLQ logic
│   ├── health/
│   │   ├── KafkaHealthEndpoint.java         # Actuator endpoint: GET /actuator/kafka
│   │   └── KafkaHealthIndicator.java        # Calls Kafka AdminClient to verify cluster connectivity
│   ├── metrics/
│   │   └── KafkaMetrics.java               # Micrometer counters (sent/received/failed/dlq) + timers (send/process latency)
│   └── producer/
│       └── KafkaMessageProducer.java        # MessageProducer<T> impl: async send via KafkaTemplate + metrics instrumentation
├── messages/                                # Domain message record types (pure data, no Spring dependency)
│   ├── DocumentProcessingMessage.java       # Document OCR/extraction job payload
│   ├── EmailNotificationMessage.java        # Email queuing payload with priority + tracking
│   ├── PaymentEventMessage.java             # Payment lifecycle event payload
│   └── UserEventMessage.java               # User lifecycle event payload
├── service/                                 # High-level domain producer services used by other packages
│   ├── DocumentProcessingProducerService.java
│   ├── EmailMessageProducerService.java
│   └── PaymentEventProducerService.java
└── documentation/
    └── api_docs.md                          # This file
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Domain Packages                           │
│  (auth, user, subscription, documents, admin, team …)       │
└────────────────────────┬────────────────────────────────────┘
                         │ uses
          ┌──────────────▼──────────────────────┐
          │        Domain Producer Services      │
          │  EmailMessageProducerService          │
          │  DocumentProcessingProducerService    │
          │  PaymentEventProducerService          │
          │  EventPublisherService                │
          └──────────────┬──────────────────────┘
                         │ calls
          ┌──────────────▼──────────────────────┐
          │        MessageBrokerFactory           │  ← discovers all MessageProducer beans
          └──────────────┬──────────────────────┘
                         │ selects
          ┌──────────────▼──────────────────────┐
          │       KafkaMessageProducer            │  implements MessageProducer<T>
          │   (KafkaTemplate + KafkaMetrics)      │
          └──────────────┬──────────────────────┘
                         │ sends to
          ┌──────────────▼──────────────────────┐
          │          Apache Kafka                 │
          │   Topics → Retry Topics → DLQ Topics  │
          └──────────────┬──────────────────────┘
                         │ consumed by
     ┌───────────────────┼───────────────────────┐
     │                   │                       │
     ▼                   ▼                       ▼
EmailKafkaConsumer  UserEventListener    (other listeners
(brokers package)   (user package)        in other packages)
     │
     ▼
EmailOrchestratorService
```

---

## Core Abstractions

### `Message<T>`
**Package:** `com.extractor.unraveldocs.brokers.core`

A generic, immutable message record that wraps any payload for sending across any broker.

```java
public record Message<T>(
    String id,           // Auto-generated UUID
    T payload,           // The message content
    String topic,        // Destination topic/queue
    String key,          // Partition/routing key (nullable)
    Map<String, String> headers,  // Additional headers (immutable copy)
    Instant timestamp    // Creation timestamp
)
```

**Factory methods:**

| Method | Description |
|---|---|
| `Message.of(payload, topic)` | Auto-generates ID + timestamp; no key, empty headers |
| `Message.of(payload, topic, key)` | Same as above but with a partition key |
| `Message.of(payload, topic, key, headers)` | Full control; headers are defensively copied |
| `msg.withTopic(newTopic)` | Returns a copy with a different topic |
| `msg.withKey(newKey)` | Returns a copy with a different key |

---

### `MessageProducer<T>`
**Package:** `com.extractor.unraveldocs.brokers.core`

Interface all producer implementations must satisfy.

| Method | Return | Description |
|---|---|---|
| `getBrokerType()` | `MessageBrokerType` | Identifies which broker this producer targets |
| `send(Message<T>)` | `CompletableFuture<MessageResult>` | Non-blocking async send |
| `sendAndWait(Message<T>)` | `MessageResult` | Blocking send; throws `MessagingException` on failure |
| `send(T payload, String topic)` *(default)* | `CompletableFuture<MessageResult>` | Convenience wrapper — builds `Message.of(payload, topic)` |
| `send(T payload, String topic, String key)` *(default)* | `CompletableFuture<MessageResult>` | Convenience wrapper with key |

---

### `MessageConsumer<T>`
**Package:** `com.extractor.unraveldocs.brokers.core`

Marker interface defining what a consumer must expose, plus two nested functional interfaces for processing callbacks.

| Method / Interface | Description |
|---|---|
| `getBrokerType()` | Identifies the broker this consumer reads from |
| `getTopics()` | Returns the array of topics to subscribe to |
| `getGroupId()` | Returns the consumer group ID |
| `MessageHandler<T>` (nested) | `@FunctionalInterface`: `handle(Message<T>)` — single-message processing |
| `BatchMessageHandler<T>` (nested) | `@FunctionalInterface`: `handle(Iterable<Message<T>>)` — batch processing |

---

### `MessageResult`
**Package:** `com.extractor.unraveldocs.brokers.core`

Immutable record returned by every send operation.

| Field | Type | Description |
|---|---|---|
| `success` | `boolean` | Whether the send succeeded |
| `messageId` | `String` | The `Message.id` of the sent message |
| `topic` | `String` | Destination topic |
| `partition` | `Integer` | Kafka partition assigned (nullable for non-Kafka brokers) |
| `offset` | `Long` | Kafka offset assigned (nullable) |
| `timestamp` | `Instant` | When the acknowledgment was received |
| `errorMessage` | `String` | Error description on failure; `null` on success |

**Factory methods:**

| Method | Description |
|---|---|
| `MessageResult.success(messageId, topic, partition, offset)` | Full success result with Kafka metadata |
| `MessageResult.success(messageId, topic)` | Success without partition/offset info |
| `MessageResult.failure(messageId, topic, errorMessage)` | Failure with a string description |
| `MessageResult.failure(messageId, topic, exception)` | Failure from an exception; extracts `getMessage()` |

---

### `MessageBrokerFactory`
**Package:** `com.extractor.unraveldocs.brokers.core`

Spring `@Component` that auto-discovers all `MessageProducer<?>` beans and registers them in an `EnumMap<MessageBrokerType, MessageProducer<?>>`.

**Default broker selection priority:** `KAFKA` → `RABBITMQ` → first available

| Method | Description |
|---|---|
| `getProducer(MessageBrokerType)` | Returns typed producer; throws `IllegalArgumentException` if not registered |
| `getProducerIfAvailable(MessageBrokerType)` | Returns `Optional<MessageProducer<T>>` |
| `getDefaultProducer()` | Returns the default broker's producer; throws `IllegalStateException` if none registered |
| `getDefaultBrokerType()` | Returns the current default `MessageBrokerType` |
| `isSupported(MessageBrokerType)` | Returns `true` if that broker type is registered |
| `getSupportedBrokers()` | Returns `Set<MessageBrokerType>` of all registered broker types |

---

### `MessageBrokerType`
**Package:** `com.extractor.unraveldocs.brokers.core`

| Value | Description |
|---|---|
| `KAFKA` | Apache Kafka (current primary implementation) |
| `RABBITMQ` | RabbitMQ (supported by config; implementation not yet wired) |

---

### `MessagingException`
**Package:** `com.extractor.unraveldocs.brokers.core`

`RuntimeException` subclass that wraps broker-specific errors. Carries the broker type, message ID, and topic for contextual error reporting.

**Constructors / Factory methods:**

| Method | Description |
|---|---|
| `new MessagingException(String message)` | Basic message-only constructor |
| `new MessagingException(String, Throwable)` | Wraps a cause |
| `new MessagingException(String, Throwable, brokerType, messageId, topic)` | Full context |
| `MessagingException.sendFailed(brokerType, messageId, topic, cause)` | Static factory — formats: `"Failed to send message {id} to topic {topic} via {broker}"` |
| `MessagingException.receiveFailed(brokerType, topic, cause)` | Static factory — formats: `"Failed to receive message from topic {topic} via {broker}"` |

---

## Kafka Configuration

### `MessagingProperties`
**Package:** `com.extractor.unraveldocs.brokers.config`  
**Prefix:** `messaging`

Top-level configuration object. All sub-properties are nested classes.

**Top-level:**

| Property | Type | Default | Description |
|---|---|---|---|
| `messaging.defaultBroker` | `String` | `"KAFKA"` | Which broker to use when not specified |
| `messaging.enabled` | `boolean` | `true` | Master switch for the messaging system |

**`messaging.kafka.*`:**

| Property | Type | Default | Description |
|---|---|---|---|
| `kafka.enabled` | `boolean` | `true` | Kafka-specific toggle |
| `kafka.topicPrefix` | `String` | `"unraveldocs"` | Prefix used when creating topic names |
| `kafka.defaultPartitions` | `int` | `3` | Partition count for auto-created topics |
| `kafka.defaultReplicationFactor` | `short` | `1` | Replication factor for auto-created topics |
| `kafka.defaultRetentionMs` | `long` | `604800000` | Default message retention: 7 days |

**`messaging.kafka.consumer.*`:**

| Property | Type | Default | Description |
|---|---|---|---|
| `consumer.concurrency` | `int` | `3` | Number of concurrent listener threads per container |
| `consumer.maxPollRecords` | `int` | `500` | Max records fetched per poll cycle |
| `consumer.sessionTimeoutMs` | `int` | `30000` | Session timeout (30 s) |
| `consumer.heartbeatIntervalMs` | `int` | `10000` | Heartbeat interval (10 s) |

**`messaging.kafka.producer.*`:**

| Property | Type | Default | Description |
|---|---|---|---|
| `producer.sendTimeoutSeconds` | `long` | `30` | Timeout for `sendAndWait()` (blocking sends) |
| `producer.transactionalEnabled` | `boolean` | `false` | Enable Kafka transactions |
| `producer.transactionalIdPrefix` | `String` | `"unraveldocs-tx-"` | Prefix for transactional IDs (only used when `transactionalEnabled = true`) |

**`messaging.kafka.retry.*`:**

| Property | Type | Default | Description |
|---|---|---|---|
| `retry.maxAttempts` | `int` | `3` | Max delivery attempts before routing to DLQ |
| `retry.initialIntervalMs` | `long` | `1000` | First retry backoff: 1 s |
| `retry.multiplier` | `double` | `2.0` | Exponential backoff multiplier |
| `retry.maxIntervalMs` | `long` | `30000` | Maximum backoff cap: 30 s |
| `retry.retryTopicsEnabled` | `boolean` | `true` | Whether intermediate retry topics are used |

**`messaging.rabbitmq.*`:**

| Property | Type | Default | Description |
|---|---|---|---|
| `rabbitmq.enabled` | `boolean` | `false` | RabbitMQ toggle (disabled by default) |
| `rabbitmq.exchangePrefix` | `String` | `"unraveldocs"` | Exchange name prefix |
| `rabbitmq.queuePrefix` | `String` | `"unraveldocs"` | Queue name prefix |

---

### `KafkaProducerConfig`
**Package:** `com.extractor.unraveldocs.brokers.kafka.config`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Configures the `ProducerFactory<String, Object>` and `KafkaTemplate<String, Object>`.

**Key producer settings (read from `application.properties` or defaults):**

| Setting | Config Key | Default | Description |
|---|---|---|---|
| Bootstrap servers | `spring.kafka.bootstrap-servers` | *(required)* | Kafka broker addresses |
| Key serializer | — | `StringSerializer` | Keys are always strings |
| Value serializer | — | `JacksonJsonSerializer` | Values serialized as JSON with type info headers |
| Acknowledgment | `spring.kafka.producer.acks` | `"all"` | All in-sync replicas must ack (strongest guarantee) |
| Retries | `spring.kafka.producer.retries` | `3` | Broker-level retries |
| Idempotence | — | `true` | Prevents duplicate messages on retry |
| Batch size | `spring.kafka.producer.batch-size` | `16384` | 16 KB batch accumulation |
| Linger | `spring.kafka.producer.linger-ms` | `5` | Wait up to 5 ms to fill batch |
| Buffer memory | `spring.kafka.producer.buffer-memory` | `33554432` | 32 MB producer buffer |
| Compression | — | `"snappy"` | Snappy compression for all messages |
| Transactional ID | `messaging.kafka.producer.transactionalIdPrefix` + UUID | *(disabled)* | Only set when `transactionalEnabled = true` |

---

### `KafkaConsumerConfig`
**Package:** `com.extractor.unraveldocs.brokers.kafka.config`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Configures `ConsumerFactory<String, Object>`, `ConcurrentKafkaListenerContainerFactory`, and the `DefaultErrorHandler`.

**Key consumer settings:**

| Setting | Config Key | Default | Description |
|---|---|---|---|
| Bootstrap servers | `spring.kafka.bootstrap-servers` | *(required)* | Kafka broker addresses |
| Group ID | `spring.kafka.consumer.group-id` | `"unraveldocs-group"` | Default consumer group |
| Auto-offset reset | `spring.kafka.consumer.auto-offset-reset` | `"earliest"` | Read from beginning on new group |
| Auto commit | `spring.kafka.consumer.enable-auto-commit` | `false` | Manual acknowledgment mode |
| Key deserializer | — | `ErrorHandlingDeserializer<StringDeserializer>` | Wraps errors without crashing the consumer |
| Value deserializer | — | `ErrorHandlingDeserializer<JacksonJsonDeserializer>` | JSON with `com.extractor.unraveldocs.*` trusted packages |
| Type info headers | — | `true` | Uses `__TypeId__` header for polymorphic deserialization |
| Ack mode | — | `MANUAL_IMMEDIATE` | Consumer calls `acknowledgment.acknowledge()` explicitly |
| Concurrency | `messaging.kafka.consumer.concurrency` | `3` | Parallel listener threads |

**Error handler (DefaultErrorHandler):**
- Exponential backoff: `initialIntervalMs` → `×multiplier` → cap at `maxIntervalMs`
- Total elapsed time limit = `maxIntervalMs × maxAttempts`
- After exhausting retries: `DeadLetterPublishingRecoverer` routes message to `<topic>-dlq`
- **Non-retryable exceptions** (routed directly to DLQ): `IllegalArgumentException`, `NullPointerException`, `ClassCastException`

---

### `KafkaTopicConfig`
**Package:** `com.extractor.unraveldocs.brokers.kafka.config`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Declares all Kafka topics as Spring `@Bean`s. Topics are auto-created by the Kafka Admin client on application startup if they do not already exist.

See the full [Topics Reference](#topics-reference) section below.

---

### `EventHandlerConfiguration`
**Package:** `com.extractor.unraveldocs.brokers.kafka.config`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Scans the Spring context for all `EventHandler<?>` beans and builds:

```java
Map<String, EventHandler<?>> eventHandlers
```

The map key is `EventHandler.getEventType()` — the same string used as the `event-type` Kafka header. This map is injected into `UserEventListener` and other consumer classes for O(1) event dispatch.

---

## Topics Reference

All topic names share the prefix `unraveldocs-`. Topics are organized into three tiers:

### Primary Topics

| Constant | Topic Name | Partitions | Retention | Description |
|---|---|---|---|---|
| `TOPIC_EMAILS` | `unraveldocs-emails` | 3 | 7 days | Email send requests consumed by `EmailKafkaConsumer` |
| `TOPIC_DOCUMENTS` | `unraveldocs-documents` | 6 | 7 days | Document processing jobs (higher partition count for throughput) |
| `TOPIC_PAYMENTS` | `unraveldocs-payments` | 3 | 30 days | Payment lifecycle events (longer retention for audit) |
| `TOPIC_USERS` | `unraveldocs-users` | 3 | 7 days | User lifecycle events consumed by `UserEventListener` |
| `TOPIC_NOTIFICATIONS` | `unraveldocs-notifications` | 3 | 3 days | Push/in-app notification events |
| `TOPIC_RECEIPTS` | `unraveldocs-receipts` | 3 | 30 days | Payment receipt events (long retention for audit) |
| `TOPIC_OCR` | `unraveldocs-ocr` | 3 | 7 days | OCR processing requests |
| `TOPIC_TEAM_EVENTS` | `unraveldocs-team-events` | 3 | 7 days | Team lifecycle events |
| `TOPIC_ADMIN_EVENTS` | `unraveldocs-admin-events` | 3 | 7 days | Admin action events |
| `TOPIC_ELASTICSEARCH` | `unraveldocs-elasticsearch` | 6 | 7 days | Elasticsearch indexing jobs (higher partitions) |

### Retry Topics (1 day retention, 1 partition each)

| Constant | Topic Name |
|---|---|
| `TOPIC_EMAILS_RETRY` | `unraveldocs-emails-retry` |
| `TOPIC_DOCUMENTS_RETRY` | `unraveldocs-documents-retry` |
| `TOPIC_PAYMENTS_RETRY` | `unraveldocs-payments-retry` |
| `TOPIC_USERS_RETRY` | `unraveldocs-users-retry` |
| `TOPIC_RECEIPTS_RETRY` | `unraveldocs-receipts-retry` |
| `TOPIC_OCR_RETRY` | `unraveldocs-ocr-retry` |
| `TOPIC_TEAM_EVENTS_RETRY` | `unraveldocs-team-events-retry` |
| `TOPIC_ADMIN_EVENTS_RETRY` | `unraveldocs-admin-events-retry` |
| `TOPIC_ELASTICSEARCH_RETRY` | `unraveldocs-elasticsearch-retry` |

### Dead Letter Queue (DLQ) Topics (30 days retention, 1 partition each)

| Constant | Topic Name |
|---|---|
| `TOPIC_EMAILS_DLQ` | `unraveldocs-emails-dlq` |
| `TOPIC_DOCUMENTS_DLQ` | `unraveldocs-documents-dlq` |
| `TOPIC_PAYMENTS_DLQ` | `unraveldocs-payments-dlq` |
| `TOPIC_USERS_DLQ` | `unraveldocs-users-dlq` |
| `TOPIC_RECEIPTS_DLQ` | `unraveldocs-receipts-dlq` |
| `TOPIC_OCR_DLQ` | `unraveldocs-ocr-dlq` |
| `TOPIC_TEAM_EVENTS_DLQ` | `unraveldocs-team-events-dlq` |
| `TOPIC_ADMIN_EVENTS_DLQ` | `unraveldocs-admin-events-dlq` |
| `TOPIC_ELASTICSEARCH_DLQ` | `unraveldocs-elasticsearch-dlq` |

> **Total topic count:** 10 primary + 9 retry + 9 DLQ = **28 topics**

---

## Event System

### `BaseEvent<T>` & `EventMetadata`
**Package:** `com.extractor.unraveldocs.brokers.kafka.events`

`BaseEvent<T>` is the envelope for all strongly-typed events published via `EventPublisherService`. It separates routing metadata from the business payload.

```
BaseEvent<T>
├── metadata: EventMetadata
│   ├── eventType: String       ← matches an EventTypes constant (used as Kafka header + map key)
│   ├── eventSource: String     ← class name of the publishing service (e.g., "ChangePasswordImpl")
│   ├── eventTimestamp: long    ← System.currentTimeMillis() at publish time
│   └── correlationId: String   ← UUID — used as Kafka message key for ordering + tracing
└── payload: T                  ← domain-specific event object (e.g., PasswordChangedEvent)
```

**Typical construction pattern (used throughout domain packages):**

```java
SomeDomainEvent payload = SomeDomainEvent.builder()
    .email(user.getEmail())
    .firstName(user.getFirstName())
    .build();

EventMetadata metadata = EventMetadata.builder()
    .eventType(EventTypes.SOME_EVENT)
    .eventSource("SomeServiceImpl")
    .eventTimestamp(System.currentTimeMillis())
    .correlationId(UUID.randomUUID().toString())
    .build();

BaseEvent<SomeDomainEvent> event = new BaseEvent<>(metadata, payload);
eventPublisherService.publishUserEvent(event);
```

---

### `EventTypes`
**Package:** `com.extractor.unraveldocs.brokers.kafka.events`

String constant class — all values are used as the `event-type` Kafka header. Consumers use these strings as map keys for `EventHandler` dispatch.

**User Events:**

| Constant | Value | Published By | Consumed By |
|---|---|---|---|
| `USER_REGISTERED` | `"UserRegistered"` | `SignupUserImpl`, `EmailVerificationImpl` (resend) | `UserRegisteredEventHandler` |
| `USER_DELETION_SCHEDULED` | `"UserDeletionScheduled"` | `DeleteUserImpl` | `UserDeletionScheduledEventHandler` |
| `USER_DELETED` | `"UserDeleted"` | `DeleteUserImpl` | `UserDeletedEventHandler` |
| `PASSWORD_CHANGED` | `"PasswordChanged"` | `ChangePasswordImpl` | `PasswordChangedEventHandler` |
| `PASSWORD_RESET_REQUESTED` | `"PasswordResetRequested"` | `PasswordResetImpl` | `PasswordResetEventHandler` |
| `PASSWORD_RESET_SUCCESSFUL` | `"PasswordResetSuccessful"` | `PasswordResetImpl` | `PasswordResetSuccessfulEventHandler` |
| `WELCOME_EVENT` | `"WelcomeEvent"` | `EmailVerificationImpl` (after verify) | `WelcomeEmailHandler` |
| `ADMIN_CREATED` | `"AdminCreated"` | Admin package | *(handler in admin package)* |

**OCR Events:**

| Constant | Value | Published By | Consumed By |
|---|---|---|---|
| `OCR_REQUESTED` | `"OcrRequested"` | OCR processing service | OCR event listener |

**Elasticsearch Events:**

| Constant | Value |
|---|---|
| `ES_DOCUMENT_INDEX` | `"EsDocumentIndex"` |
| `ES_USER_INDEX` | `"EsUserIndex"` |
| `ES_PAYMENT_INDEX` | `"EsPaymentIndex"` |
| `ES_SUBSCRIPTION_INDEX` | `"EsSubscriptionIndex"` |

**Team Events:**

| Constant | Value |
|---|---|
| `TEAM_TRIAL_EXPIRING` | `"TeamTrialExpiring"` |
| `TEAM_SUBSCRIPTION_CHARGED` | `"TeamSubscriptionCharged"` |
| `TEAM_SUBSCRIPTION_FAILED` | `"TeamSubscriptionFailed"` |
| `TEAM_CREATED` | `"TeamCreated"` |

---

### `EventHandler<T>`
**Package:** `com.extractor.unraveldocs.brokers.kafka.events`

Simple two-method interface that all event handler components implement.

```java
public interface EventHandler<T> {
    void handleEvent(T event);   // Business logic for this event
    String getEventType();       // Returns the EventTypes constant string used as map key
}
```

All `@Component` implementations are auto-registered by `EventHandlerConfiguration` into `Map<String, EventHandler<?>>`.

---

### `EventPublisherService`
**Package:** `com.extractor.unraveldocs.brokers.kafka.events`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Thin routing layer over `MessageBrokerFactory`. Wraps `BaseEvent<T>` into a `Message<Object>`, adds the `event-type` header, uses `correlationId` as the Kafka message key (for partition ordering), and fires async.

| Method | Topic | Description |
|---|---|---|
| `publishEvent(String topic, BaseEvent<T>)` | Any | Generic publish — extracts payload, builds `Message`, calls `KafkaMessageProducer.send()` |
| `publishUserEvent(BaseEvent<T>)` | `TOPIC_USERS` | Shortcut for user lifecycle events |
| `publishTeamEvent(BaseEvent<T>)` | `TOPIC_TEAM_EVENTS` | Shortcut for team events |
| `publishAdminEvent(BaseEvent<T>)` | `TOPIC_ADMIN_EVENTS` | Shortcut for admin events |
| `publishOcrEvent(BaseEvent<T>)` | `TOPIC_OCR` | Shortcut for OCR events |

On success: logs event type + correlationId + topic.  
On failure: logs error and throws `MessagingException.sendFailed(...)`.

---

## Kafka Producer

### `KafkaMessageProducer<T>`
**Package:** `com.extractor.unraveldocs.brokers.kafka.producer`  
**Implements:** `MessageProducer<T>`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

The concrete Kafka implementation of the `MessageProducer` interface. All broker-agnostic code routes through here at runtime.

**`send(Message<T>)` — Async:**
```
1. Build ProducerRecord from Message (topic, key, timestamp, payload)
2. Attach "message-id" header from message.id
3. Attach all custom headers from message.headers
4. kafkaTemplate.send(record)        ← CompletableFuture
5. On success:
   ├─ kafkaMetrics.stopSendTimer()
   ├─ kafkaMetrics.recordMessageSent()
   └─ return MessageResult.success(messageId, topic, partition, offset)
6. On failure:
   ├─ kafkaMetrics.recordMessageFailed()
   └─ return MessageResult.failure(messageId, topic, exception)
```

**`sendAndWait(Message<T>)` — Synchronous (blocking):**
```
1. Build ProducerRecord (same as above)
2. kafkaTemplate.send(record).get(sendTimeoutSeconds, SECONDS)
3. On success → return MessageResult.success(...)
4. On timeout/exception → throw MessagingException.sendFailed(...)
```

**Kafka headers attached to every message:**

| Header Name | Value |
|---|---|
| `message-id` | `Message.id` (UUID) |
| *(custom)* | Each key-value from `Message.headers` |

---

## Kafka Consumer

### `EmailKafkaConsumer`
**Package:** `com.extractor.unraveldocs.brokers.kafka.consumer`  
**Topic:** `unraveldocs-emails`  
**Group ID:** `email-consumer-group`  
**Ack Mode:** `MANUAL_IMMEDIATE`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

The dedicated consumer for the email delivery pipeline.

**Processing logic:**
```
1. Receive EmailNotificationMessage from topic
2. Map to EmailMessage (to, subject, templateName, templateModel)
3. emailOrchestratorService.sendEmail(emailMessage)
4. On success: log + acknowledgment.acknowledge()
5. On failure: log error + rethrow exception → triggers DefaultErrorHandler retry → DLQ
```

> **Note:** Other consumers (`UserEventListener` in the `user` package, Elasticsearch listeners, etc.) follow the same manual-ack pattern but live in their respective domain packages.

---

## Message Schemas

### `EmailNotificationMessage`
**Package:** `com.extractor.unraveldocs.brokers.messages`

```
EmailNotificationMessage
├── to: String                          ← Recipient email
├── subject: String                     ← Email subject line
├── templateName: String                ← Thymeleaf template name (without .html)
├── templateVariables: Map<String, Object> ← Template rendering context
├── priority: EmailPriority             ← HIGH | NORMAL | LOW
├── requestedAt: Instant                ← When the email was queued
├── userId: String                      ← Optional — for tracking/correlation
└── correlationId: String               ← Optional — for event correlation
```

**Priority enum:** `HIGH` (password reset, OTP), `NORMAL` (general), `LOW`

**Factory methods:**

| Method | Priority | Description |
|---|---|---|
| `EmailNotificationMessage.of(to, subject, templateName, vars)` | `NORMAL` | Standard email |
| `EmailNotificationMessage.highPriority(to, subject, templateName, vars)` | `HIGH` | Time-sensitive (e.g., password reset) |
| `msg.forUser(userId)` | *(copy)* | Returns copy with `userId` set |
| `msg.withCorrelationId(id)` | *(copy)* | Returns copy with `correlationId` set |

---

### `DocumentProcessingMessage`
**Package:** `com.extractor.unraveldocs.brokers.messages`

```
DocumentProcessingMessage
├── documentId: String                  ← Document UUID in the system
├── userId: String                      ← Owner user ID
├── s3Key: String                       ← S3 object key
├── bucketName: String                  ← S3 bucket name
├── fileName: String                    ← Original filename
├── mimeType: String                    ← MIME type (e.g., application/pdf)
├── processingType: ProcessingType      ← OCR_EXTRACTION | TEXT_EXTRACTION | IMAGE_ANALYSIS | PDF_CONVERSION | THUMBNAIL_GENERATION
├── metadata: Map<String, String>       ← Optional extra parameters
├── requestedAt: Instant                ← When the job was queued
└── priority: ProcessingPriority        ← HIGH | NORMAL | LOW
```

**Factory methods:**

| Method | `processingType` | `priority` | Description |
|---|---|---|---|
| `DocumentProcessingMessage.forOcr(...)` | `OCR_EXTRACTION` | `NORMAL` | Standard OCR job |
| `DocumentProcessingMessage.forTextExtraction(...)` | `TEXT_EXTRACTION` | `NORMAL` | Text extraction job |
| `msg.withMetadata(map)` | *(copy)* | *(same)* | Returns copy with metadata applied |

---

### `PaymentEventMessage`
**Package:** `com.extractor.unraveldocs.brokers.messages`

```
PaymentEventMessage
├── eventType: PaymentEventType         ← See enum below
├── paymentId: String                   ← Internal payment UUID
├── providerPaymentId: String           ← Gateway's payment reference
├── provider: String                    ← "STRIPE" | "PAYSTACK" | "PAYPAL"
├── userId: String                      ← User who triggered the payment
├── amount: BigDecimal                  ← Payment amount (null for failures)
├── currency: String                    ← ISO currency code
├── status: String                      ← "succeeded" | "failed" | etc.
├── subscriptionId: String              ← Associated subscription (nullable)
├── metadata: Map<String, String>       ← Extra event-specific context
└── eventTimestamp: Instant             ← When the event occurred
```

**`PaymentEventType` enum:**

| Value | Description |
|---|---|
| `PAYMENT_INITIATED` | Payment process started |
| `PAYMENT_SUCCEEDED` | Payment completed successfully |
| `PAYMENT_FAILED` | Payment attempt failed |
| `PAYMENT_REFUNDED` | Payment was refunded |
| `SUBSCRIPTION_CREATED` | New subscription created |
| `SUBSCRIPTION_RENEWED` | Subscription auto-renewed |
| `SUBSCRIPTION_CANCELLED` | Subscription cancelled |
| `SUBSCRIPTION_EXPIRED` | Subscription expired without renewal |
| `INVOICE_PAID` | Invoice paid successfully |
| `INVOICE_FAILED` | Invoice payment failed |

**Factory methods:**

| Method | Event Type | Description |
|---|---|---|
| `PaymentEventMessage.paymentSucceeded(paymentId, providerPaymentId, provider, userId, amount, currency)` | `PAYMENT_SUCCEEDED` | Full payment success |
| `PaymentEventMessage.paymentFailed(paymentId, providerPaymentId, provider, userId, failureReason)` | `PAYMENT_FAILED` | Failure with reason in metadata |
| `PaymentEventMessage.subscriptionCreated(...)` | `SUBSCRIPTION_CREATED` | Subscription creation event |

---

### `UserEventMessage`
**Package:** `com.extractor.unraveldocs.brokers.messages`

```
UserEventMessage
├── eventType: UserEventType            ← See enum below
├── userId: String                      ← User UUID
├── email: String                       ← User email
├── eventData: Map<String, Object>      ← Event-specific extra data
├── eventTimestamp: Instant             ← When the event occurred
├── ipAddress: String                   ← Originating IP (security events; nullable)
└── userAgent: String                   ← User agent string (security events; nullable)
```

**`UserEventType` enum:**

| Value | Description |
|---|---|
| `USER_REGISTERED` | New account created |
| `USER_VERIFIED` | Email verified |
| `USER_LOGIN` | Successful login |
| `USER_LOGOUT` | Logout |
| `USER_UPDATED` | Profile updated |
| `USER_DELETED` | Account deleted |
| `PASSWORD_CHANGED` | Password changed in-session |
| `PASSWORD_RESET_REQUESTED` | Forgot-password request |
| `PASSWORD_RESET_COMPLETED` | Password reset completed |
| `ROLE_CHANGED` | User role changed by admin |
| `SUBSCRIPTION_CHANGED` | Subscription tier changed |
| `SUSPICIOUS_ACTIVITY` | Security anomaly detected |

**Factory methods:**

| Method | Event Type | Description |
|---|---|---|
| `UserEventMessage.userRegistered(userId, email)` | `USER_REGISTERED` | Registration event |
| `UserEventMessage.userVerified(userId, email)` | `USER_VERIFIED` | Verification event |
| `UserEventMessage.loginEvent(userId, email, ipAddress, userAgent)` | `USER_LOGIN` | Login with IP + agent |

---

## Domain Producer Services

These services are the public API that all other packages use to interact with the broker. They abstract away `Message` construction and Kafka topic routing.

### `EmailMessageProducerService`
**Package:** `com.extractor.unraveldocs.brokers.service`

| Method | Priority | Description |
|---|---|---|
| `queueEmail(to, subject, templateName, vars)` | `NORMAL` | Standard async email via `TOPIC_EMAILS` |
| `queueHighPriorityEmail(to, subject, templateName, vars)` | `HIGH` | Time-sensitive email (password reset, OTP) |
| `queueEmailForUser(to, subject, templateName, vars, userId)` | `NORMAL` | Tracks email against a `userId` |

All methods return `CompletableFuture<MessageResult>`. Callers may chain `.thenAccept(...)` or ignore the result for fire-and-forget.

**Internal flow:**
```
1. Build EmailNotificationMessage (static factory)
2. Wrap in Message<EmailNotificationMessage>.of(emailMessage, TOPIC_EMAILS)
3. messageBrokerFactory.getProducer(KAFKA).send(message)
4. Return CompletableFuture<MessageResult>
```

---

### `DocumentProcessingProducerService`
**Package:** `com.extractor.unraveldocs.brokers.service`

| Method | Processing Type | Priority | Description |
|---|---|---|---|
| `queueForOcr(documentId, userId, s3Key, bucketName, fileName, mimeType)` | `OCR_EXTRACTION` | `NORMAL` | Standard OCR job to `TOPIC_DOCUMENTS` |
| `queueForTextExtraction(...)` | `TEXT_EXTRACTION` | `NORMAL` | Text extraction job |
| `queueHighPriorityOcr(...)` | `OCR_EXTRACTION` | `HIGH` | High-priority OCR (e.g., trial or premium users) |

All methods return `CompletableFuture<MessageResult>`. Documents are keyed by `documentId` for partition ordering.

---

### `PaymentEventProducerService`
**Package:** `com.extractor.unraveldocs.brokers.service`

| Method | Event Type | Description |
|---|---|---|
| `publishPaymentSucceeded(paymentId, providerPaymentId, provider, userId, amount, currency)` | `PAYMENT_SUCCEEDED` | Publishes to `TOPIC_PAYMENTS` |
| `publishPaymentFailed(paymentId, providerPaymentId, provider, userId, failureReason)` | `PAYMENT_FAILED` | Publishes failure event |
| `publishSubscriptionCreated(paymentId, providerPaymentId, provider, userId, subscriptionId, amount, currency)` | `SUBSCRIPTION_CREATED` | Subscription creation event |

All methods return `CompletableFuture<MessageResult>`. Payment events are keyed by `userId` for user-level ordering.

---

## Error Handling & Retry Strategy

### `KafkaErrorHandler`
**Package:** `com.extractor.unraveldocs.brokers.kafka.handler`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Programmatic DLQ handler that can be called by consumer code directly (as opposed to the framework's `DefaultErrorHandler`).

**DLQ headers added to every failed message:**

| Header | Value |
|---|---|
| `original-topic` | The source topic name |
| `exception-message` | `exception.getMessage()` |
| `exception-class` | `exception.getClass().getName()` |
| `failure-timestamp` | `Instant.now().toString()` |
| `retry-count` | Current retry attempt number |

**Public methods:**

| Method | Description |
|---|---|
| `routeToDlq(ConsumerRecord, Exception)` | Immediately routes record to `<topic>-dlq` with DLQ headers; original headers preserved |
| `retryFromDlq(ConsumerRecord<String, Object>)` | Re-publishes to original topic (read from `original-topic` header); increments `retry-count`; returns `true` on success |
| `routeToRetryOrDlq(ConsumerRecord, Exception, int maxRetries)` | Checks `retry-count` vs `maxRetries` — routes to retry topic if under limit, else DLQ |
| `getRetryTopic(String sourceTopic)` | Maps source topic → retry topic name (returns `null` for unknown topics) |

**Topic routing (built-in mappings):**

| Source | Retry Topic | DLQ Topic |
|---|---|---|
| `unraveldocs-emails` | `unraveldocs-emails-retry` | `unraveldocs-emails-dlq` |
| `unraveldocs-documents` | `unraveldocs-documents-retry` | `unraveldocs-documents-dlq` |
| `unraveldocs-payments` | `unraveldocs-payments-retry` | `unraveldocs-payments-dlq` |
| `unraveldocs-users` | `unraveldocs-users-retry` | `unraveldocs-users-dlq` |
| *(any other topic)* | `null` | `<topic>-dlq` (fallback) |

---

### Exponential Backoff & DLQ

The Spring `DefaultErrorHandler` (configured in `KafkaConsumerConfig`) handles retries automatically for all `@KafkaListener` methods:

```
Message received by @KafkaListener
    │
    ├─ Processing succeeds → acknowledgment.acknowledge() → done
    │
    └─ Processing throws exception
           │
           ├─ Is exception non-retryable? (IllegalArgumentException, NullPointerException, ClassCastException)
           │     └─ YES → route directly to DLQ
           │
           └─ NO → Exponential backoff retry
                    Attempt 1: wait 1s
                    Attempt 2: wait 2s
                    Attempt 3: wait 4s  (capped at 30s)
                         │
                         └─ All attempts exhausted
                               └─ DeadLetterPublishingRecoverer → <topic>-dlq
```

---

## Observability

### `KafkaMetrics`
**Package:** `com.extractor.unraveldocs.brokers.kafka.metrics`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")`

Micrometer-based metrics. All metrics are per-topic, lazily initialized in `ConcurrentHashMap` caches.

**Metric prefix:** `kafka.messaging`

**Counters:**

| Metric Name | Tag: `operation` | Description |
|---|---|---|
| `kafka.messaging.messages.sent` | `sent` | Messages successfully published |
| `kafka.messaging.messages.received` | `received` | Messages successfully consumed |
| `kafka.messaging.messages.failed` | `failed` | Send failures |
| `kafka.messaging.messages.dlq` | `dlq` | Messages routed to DLQ |

**Timers:**

| Metric Name | Tag: `operation` | Description |
|---|---|---|
| `kafka.messaging.send.latency` | `send` | End-to-end send time (from `send()` call to broker ack) |
| `kafka.messaging.process.latency` | `process` | Consumer processing time |

**Usage in `KafkaMessageProducer`:**
```java
Timer.Sample timer = kafkaMetrics.startSendTimer();
// ... send to Kafka ...
kafkaMetrics.stopSendTimer(timer, topic);    // records send latency
kafkaMetrics.recordMessageSent(topic);       // increments sent counter
// On failure:
kafkaMetrics.recordMessageFailed(topic);     // increments failed counter
```

Metrics are visible via Spring Boot Actuator at `GET /actuator/metrics/kafka.messaging.*`.

---

### `KafkaHealthIndicator`
**Package:** `com.extractor.unraveldocs.brokers.kafka.health`  
**Conditional:** `@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")` + `@ConditionalOnClass(KafkaAdmin.class)`

Uses Kafka `AdminClient` to describe the cluster. Times out after 10 seconds.

**`checkHealth()` returns:**

| Key | Type | Description |
|---|---|---|
| `status` | `String` | `"UP"` or `"DOWN"` |
| `clusterId` | `String` | Kafka cluster UUID |
| `brokersAvailable` | `Integer` | Number of broker nodes reachable |
| `brokers` | `String` | Comma-separated broker addresses |
| `controllerId` | `Integer` | ID of the controller broker |
| `controllerHost` | `String` | `host:port` of the controller broker |
| `error` | `String` | Error message (only present when `status = "DOWN"`) |
| `errorType` | `String` | Exception class name (only present on `"DOWN"`) |

**`isHealthy()`** — returns `true` if `brokersAvailable > 0`.

---

### `KafkaHealthEndpoint`
**Package:** `com.extractor.unraveldocs.brokers.kafka.health`  
**Actuator ID:** `kafka`  
**Path:** `GET /actuator/kafka`

Custom Spring Boot Actuator endpoint exposing the `KafkaHealthIndicator.checkHealth()` result as a JSON response.

**Example response:**
```json
{
  "status": "UP",
  "clusterId": "abc123",
  "brokersAvailable": 1,
  "brokers": "localhost:9092 (id=0)",
  "controllerId": 0,
  "controllerHost": "localhost:9092"
}
```

---

## Configuration Reference

Minimum required `application.properties` to enable Kafka:

```properties
# Required — activates all @ConditionalOnProperty Kafka beans
spring.kafka.bootstrap-servers=localhost:9092

# Consumer group
spring.kafka.consumer.group-id=unraveldocs-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false

# Producer tuning (all have defaults in KafkaProducerConfig)
spring.kafka.producer.retries=3
spring.kafka.producer.acks=all
spring.kafka.producer.batch-size=16384
spring.kafka.producer.linger-ms=5
spring.kafka.producer.buffer-memory=33554432

# Messaging abstraction layer
messaging.enabled=true
messaging.defaultBroker=KAFKA

messaging.kafka.consumer.concurrency=3
messaging.kafka.consumer.maxPollRecords=500
messaging.kafka.consumer.sessionTimeoutMs=30000
messaging.kafka.consumer.heartbeatIntervalMs=10000

messaging.kafka.producer.sendTimeoutSeconds=30
messaging.kafka.producer.transactionalEnabled=false

messaging.kafka.retry.maxAttempts=3
messaging.kafka.retry.initialIntervalMs=1000
messaging.kafka.retry.multiplier=2.0
messaging.kafka.retry.maxIntervalMs=30000
messaging.kafka.retry.retryTopicsEnabled=true
```

To **disable Kafka** (e.g., local dev without a broker): simply omit `spring.kafka.bootstrap-servers` — all Kafka beans will be skipped and the application will start without messaging.

---

## Flow Diagrams

### Message Send Flow (Async)

```
Domain Service (e.g., ChangePasswordImpl)
  │
  ├─ Build BaseEvent<PasswordChangedEvent>
  ├─ eventPublisherService.publishUserEvent(event)
  │
  ▼
EventPublisherService
  │
  ├─ Extract payload + eventType + correlationId from BaseEvent
  ├─ Build Message.of(payload, TOPIC_USERS, correlationId, {"event-type": eventType})
  │
  ▼
MessageBrokerFactory.getProducer(KAFKA)
  │
  ▼
KafkaMessageProducer.send(message)
  │
  ├─ Build ProducerRecord (topic, key=correlationId, value=payload)
  ├─ Add "message-id" header
  ├─ Add "event-type" header
  ├─ kafkaMetrics.startSendTimer()
  ├─ kafkaTemplate.send(record)  ← async CompletableFuture
  │
  ├─ On success:
  │     ├─ kafkaMetrics.stopSendTimer()
  │     ├─ kafkaMetrics.recordMessageSent()
  │     └─ return MessageResult.success(id, topic, partition, offset)
  │
  └─ On failure:
        ├─ kafkaMetrics.recordMessageFailed()
        └─ return MessageResult.failure(id, topic, exception)
```

---

### Email Pipeline Flow

```
Any Service (e.g., UserRegisteredEventHandler)
  │
  ├─ emailMessageProducerService.queueEmail(to, subject, template, vars)
  │
  ▼
EmailMessageProducerService
  │
  ├─ Build EmailNotificationMessage.of(...)
  ├─ Wrap in Message<EmailNotificationMessage>
  ├─ messageBrokerFactory.getProducer(KAFKA).send(message)
  │
  ▼
Kafka Topic: unraveldocs-emails
  │
  ▼
EmailKafkaConsumer (@KafkaListener, group: email-consumer-group)
  │
  ├─ Convert EmailNotificationMessage → EmailMessage
  ├─ emailOrchestratorService.sendEmail(emailMessage)
  │
  ├─ On success: acknowledgment.acknowledge()
  └─ On failure: rethrow → DefaultErrorHandler → retry → DLQ
```

---

### Event Handler Dispatch Flow

```
Kafka Topic: unraveldocs-users
  │
  ▼
UserEventListener (@KafkaListener, group: user-events-group)
  │
  ├─ Extract "event-type" header from ConsumerRecord
  ├─ normalizeEventType(eventType) → e.g., "PasswordChangedEvent"
  │
  ├─ payload instanceof Map? → objectMapper.convertValue(payload, targetClass)
  │
  ├─ eventHandlers.get("PasswordChangedEvent")   ← O(1) lookup in Spring bean map
  │
  ├─ Handler found?
  │     └─ YES → handler.handleEvent(typedPayload)
  │               └─ PasswordChangedEventHandler.handleEvent(event)
  │                     └─ userEmailTemplateService.sendSuccessfulPasswordChange(...)
  │
  ├─ acknowledgment.acknowledge()
  └─ On failure: rethrow → DefaultErrorHandler → retry → unraveldocs-users-dlq
```

---

### Error Handling & DLQ Flow

```
Consumer receives message
  │
  └─ Throws exception during processing
         │
         ├─ Is exception type in non-retryable list?
         │   (IllegalArgumentException, NullPointerException, ClassCastException)
         │     └─ YES → skip retries → DeadLetterPublishingRecoverer → <topic>-dlq
         │
         └─ NO → DefaultErrorHandler with ExponentialBackOff
                  │
                  ├─ Retry 1: wait 1s
                  ├─ Retry 2: wait 2s
                  ├─ Retry 3: wait 4s (max 30s per attempt)
                  │
                  └─ All retries exhausted
                         │
                         └─ DeadLetterPublishingRecoverer
                               └─ Route to <topic>-dlq with failure headers:
                                     original-topic: unraveldocs-xxx
                                     exception-message: ...
                                     exception-class: ...
                                     failure-timestamp: ...
```

---

### Health Check Flow (Actuator)

```
GET /actuator/kafka
  │
  ▼
KafkaHealthEndpoint.kafkaHealth()
  │
  ▼
KafkaHealthIndicator.checkHealth()
  │
  ├─ Create AdminClient from KafkaAdmin config
  ├─ adminClient.describeCluster() — timeout: 10s
  │
  ├─ Success:
  │     └─ {status: "UP", clusterId: ..., brokersAvailable: N, brokers: ..., controllerId: ..., controllerHost: ...}
  │
  └─ Failure (timeout or connectivity error):
        └─ {status: "DOWN", error: "...", errorType: "..."}
```

