package com.extractor.unraveldocs.messagequeuing.kafka;

import com.extractor.unraveldocs.messagequeuing.config.MessagingProperties;
import com.extractor.unraveldocs.messagequeuing.core.Message;
import com.extractor.unraveldocs.messagequeuing.core.MessageBrokerType;
import com.extractor.unraveldocs.messagequeuing.core.MessageResult;
import com.extractor.unraveldocs.messagequeuing.kafka.metrics.KafkaMetrics;
import com.extractor.unraveldocs.messagequeuing.kafka.producer.KafkaMessageProducer;
import com.extractor.unraveldocs.messagequeuing.messages.EmailNotificationMessage;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for KafkaMessageProducer.
 */
@ExtendWith(MockitoExtension.class)
class KafkaMessageProducerTest {
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private MessagingProperties messagingProperties;

    @Mock
    private KafkaMetrics kafkaMetrics;

    @Mock
    private Timer.Sample timerSample;

    private KafkaMessageProducer<EmailNotificationMessage> producer;
    
    @BeforeEach
    void setUp() {
        lenient().when(kafkaMetrics.startSendTimer()).thenReturn(timerSample);
        lenient().doNothing().when(kafkaMetrics).stopSendTimer(any(Timer.Sample.class), anyString());
        lenient().doNothing().when(kafkaMetrics).recordMessageSent(anyString());
        lenient().doNothing().when(kafkaMetrics).recordMessageFailed(anyString());
        producer = new KafkaMessageProducer<>(kafkaTemplate, messagingProperties, kafkaMetrics);
    }
    
    @Test
    @DisplayName("Should return KAFKA as broker type")
    void shouldReturnKafkaBrokerType() {
        assertThat(producer.getBrokerType()).isEqualTo(MessageBrokerType.KAFKA);
    }
    
    @Test
    @DisplayName("Should send message successfully")
    void shouldSendMessageSuccessfully() {
        // Given
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                "test@example.com",
                "Test Subject",
                "test-template",
                Map.of("name", "Test User")
        );
        
        Message<EmailNotificationMessage> message = Message.of(
                emailMessage,
                "unraveldocs-emails",
                "test@example.com"
        );
        
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("unraveldocs-emails", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(
                "unraveldocs-emails",
                "test@example.com",
                emailMessage
        );
        producerRecord.headers().add("message-id", message.id().getBytes());
        
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        
        // When
        CompletableFuture<MessageResult> resultFuture = producer.send(message);
        MessageResult result = resultFuture.join();
        
        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.topic()).isEqualTo("unraveldocs-emails");
        assertThat(result.partition()).isEqualTo(0);
        assertThat(result.offset()).isEqualTo(0L);
        
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }
    
    @Test
    @DisplayName("Should handle send failure")
    void shouldHandleSendFailure() {
        // Given
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                "test@example.com",
                "Test Subject",
                "test-template",
                Map.of()
        );
        
        Message<EmailNotificationMessage> message = Message.of(emailMessage, "unraveldocs-emails");
        
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(failedFuture);
        
        // When
        CompletableFuture<MessageResult> resultFuture = producer.send(message);
        MessageResult result = resultFuture.join();
        
        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Kafka unavailable");
    }
    
    @Test
    @DisplayName("Should include message headers in producer record")
    void shouldIncludeMessageHeaders() {
        // Given
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                "test@example.com",
                "Test Subject",
                "test-template",
                Map.of()
        );
        
        Message<EmailNotificationMessage> message = Message.of(
                emailMessage,
                "unraveldocs-emails",
                "test@example.com",
                Map.of("correlation-id", "test-123")
        );
        
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("unraveldocs-emails", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
                ArgumentCaptor.forClass(ProducerRecord.class);
        
        when(kafkaTemplate.send(recordCaptor.capture())).thenAnswer(invocation -> {
            ProducerRecord<String, Object> record = recordCaptor.getValue();
            SendResult<String, Object> result = new SendResult<>(record, metadata);
            return CompletableFuture.completedFuture(result);
        });
        
        // When
        producer.send(message).join();
        
        // Then
        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.headers().lastHeader("message-id")).isNotNull();
        assertThat(capturedRecord.headers().lastHeader("correlation-id")).isNotNull();
    }
    
    @Test
    @DisplayName("Should use convenience send method with topic only")
    void shouldUseConvenienceSendMethod() {
        // Given
        EmailNotificationMessage emailMessage = EmailNotificationMessage.of(
                "user@example.com",
                "Welcome",
                "welcome-template",
                Map.of("username", "newuser")
        );
        
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("unraveldocs-emails", 0),
                0L, 0, System.currentTimeMillis(), 0, 0
        );
        
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
            ProducerRecord<String, Object> record = invocation.getArgument(0);
            return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
        });
        
        // When
        CompletableFuture<MessageResult> resultFuture = producer.send(emailMessage, "unraveldocs-emails");
        MessageResult result = resultFuture.join();
        
        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.topic()).isEqualTo("unraveldocs-emails");
    }
}
