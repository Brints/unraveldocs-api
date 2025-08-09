package com.extractor.unraveldocs.ocrprocessing.service;

import com.extractor.unraveldocs.config.RabbitMQConfig;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.ocrprocessing.dto.request.OcrRequestMessage;
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

    @RabbitListener(queues = RabbitMQConfig.OCR_QUEUE_NAME)
    public void receiveMessage(OcrRequestMessage message) {
        log.info("Received OCR request for collection ID: {}, document ID: {}",
                s.sanitizeLogging(message.getCollectionId()),
                s.sanitizeLogging(message.getDocumentId()));

        try {
            ocrService.processOcrRequest(message.getCollectionId(), message.getDocumentId());
        } catch (Exception e) {
            log.error("Error processing OCR request for collection ID: {}, document ID: {}. Error: {}",
                    s.sanitizeLogging(message.getCollectionId()),
                    s.sanitizeLogging(message.getDocumentId()),
                    e.getMessage(), e);
        }
    }
}
