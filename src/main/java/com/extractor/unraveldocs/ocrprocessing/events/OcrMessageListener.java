package com.extractor.unraveldocs.ocrprocessing.events;

import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.events.BaseEvent;
import com.extractor.unraveldocs.ocrprocessing.interfaces.ProcessOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrMessageListener {
    private final ProcessOcrService ocrService;
    private final SanitizeLogging s;

    @RabbitListener(queues = RabbitMQConfig.OCR_EVENTS_QUEUE)
    public void receiveOcrRequestedEvent(BaseEvent<OcrRequestedEvent> event) {
        OcrRequestedEvent payload = event.getPayload();
        String correlationId = event.getMetadata().getCorrelationId();

        log.info("Received OCR request for collection ID: {}, document ID: {}. CorrelationId: {}",
                s.sanitizeLogging(payload.getCollectionId()),
                s.sanitizeLogging(payload.getDocumentId()),
                s.sanitizeLogging(correlationId));

        try {
            ocrService.processOcrRequest(payload.getCollectionId(), payload.getDocumentId());
        } catch (Exception e) {
            log.error("Error processing OCR request for collection ID: {}, document ID: {}. CorrelationId: {}. Error: {}",
                    s.sanitizeLogging(payload.getCollectionId()),
                    s.sanitizeLogging(payload.getDocumentId()),
                    s.sanitizeLogging(correlationId),
                    e.getMessage(), e);
            // Re-throw the exception to trigger the retry/DLQ mechanism
            throw e;
        }
    }
}