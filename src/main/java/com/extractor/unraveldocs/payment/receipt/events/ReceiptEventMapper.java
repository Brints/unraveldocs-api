package com.extractor.unraveldocs.payment.receipt.events;

import com.extractor.unraveldocs.payment.receipt.dto.ReceiptData;
import org.mapstruct.Mapper;

/**
 * Mapper for converting between ReceiptData and ReceiptRequestedEvent
 */
@Mapper(componentModel = "spring")
public interface ReceiptEventMapper {

    /**
     * Convert ReceiptData to ReceiptRequestedEvent for Kafka publishing
     */
    ReceiptRequestedEvent toReceiptRequestedEvent(ReceiptData receiptData);

    /**
     * Convert ReceiptRequestedEvent back to ReceiptData for processing
     */
    ReceiptData toReceiptData(ReceiptRequestedEvent event);
}

