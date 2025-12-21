package com.extractor.unraveldocs.messagequeuing.rabbitmq.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventMetadata {
    private String eventType;
    private String eventSource;
    private long eventTimestamp;
    private String correlationId;
}
