package com.extractor.unraveldocs.brokers.rabbitmq.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMetadata {
    private String eventType;
    private String eventSource;
    private long eventTimestamp;
    private String correlationId;
}
