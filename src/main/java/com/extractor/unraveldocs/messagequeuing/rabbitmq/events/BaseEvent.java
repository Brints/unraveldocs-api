package com.extractor.unraveldocs.messagequeuing.rabbitmq.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> implements Serializable {
    private EventMetadata metadata;
    private T payload;
}
