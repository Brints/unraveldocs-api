package com.extractor.unraveldocs.coupon.service.impl;

import com.extractor.unraveldocs.coupon.dto.request.BulkCouponGenerationRequest;
import com.extractor.unraveldocs.coupon.dto.response.BulkGenerationJobResponse;
import com.extractor.unraveldocs.coupon.kafka.BulkGenerationCommand;
import com.extractor.unraveldocs.coupon.kafka.CouponGenerationConsumer;
import com.extractor.unraveldocs.coupon.kafka.CouponGenerationProducer;
import com.extractor.unraveldocs.coupon.service.BulkCouponGenerationService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.shared.response.ResponseBuilderService;
import com.extractor.unraveldocs.shared.response.UnravelDocsResponse;
import com.extractor.unraveldocs.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class BulkCouponGenerationServiceImpl implements BulkCouponGenerationService {

        private final SanitizeLogging sanitizer;
        private final CouponGenerationProducer producer;
        private final CouponGenerationConsumer consumer;
        private final ResponseBuilderService responseBuilder;

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public UnravelDocsResponse<BulkGenerationJobResponse> generateBulkCoupons(
                        BulkCouponGenerationRequest request, User createdBy) {

                log.info("Initiating bulk coupon generation. Quantity: {}, User: {}",
                                sanitizer.sanitizeLoggingInteger(request.getQuantity()),
                                sanitizer.sanitizeLogging(createdBy.getEmail()));

                // Validate request
                if (request.getQuantity() == null || request.getQuantity() < 1) {
                        return responseBuilder.buildUserResponse(
                                        null, HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
                }
                if (request.getQuantity() > 1000) {
                        return responseBuilder.buildUserResponse(
                                        null, HttpStatus.BAD_REQUEST, "Maximum quantity is 1000 per request");
                }

                // Generate job ID
                String jobId = UUID.randomUUID().toString();

                // Create command
                BulkGenerationCommand command = BulkGenerationCommand.builder()
                                .jobId(jobId)
                                .userId(createdBy.getId())
                                .request(request)
                                .timestamp(OffsetDateTime.now())
                                .retryCount(0)
                                .build();

                // Publish to Kafka
                try {
                        producer.publishBulkGenerationCommand(command);
                } catch (Exception e) {
                        log.error("Failed to publish bulk generation command", e);
                        return responseBuilder.buildUserResponse(
                                        null, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to initiate bulk generation");
                }

                // Return job response
                BulkGenerationJobResponse response = BulkGenerationJobResponse.builder()
                                .jobId(jobId)
                                .status("PENDING")
                                .totalRequested(request.getQuantity())
                                .successfullyCreated(0)
                                .failed(0)
                                .createdCouponCodes(new ArrayList<>())
                                .errors(new ArrayList<>())
                                .startedAt(OffsetDateTime.now().format(FORMATTER))
                                .progressPercentage(0)
                                .build();

                return responseBuilder.buildUserResponse(
                                response, HttpStatus.ACCEPTED, "Bulk generation job submitted");
        }

        @Override
        public UnravelDocsResponse<BulkGenerationJobResponse> getJobStatus(String jobId) {
                log.info("Getting status for job: {}", sanitizer.sanitizeLogging(jobId));

                CouponGenerationConsumer.JobProgress progress = consumer.getJobProgress(jobId);
                if (progress == null) {
                        return responseBuilder.buildUserResponse(
                                        null, HttpStatus.NOT_FOUND, "Job not found");
                }

                int progressPct = progress.totalCount() > 0
                                ? (int) ((double) progress.successCount() / progress.totalCount() * 100)
                                : 0;

                BulkGenerationJobResponse response = BulkGenerationJobResponse.builder()
                                .jobId(jobId)
                                .status(progress.status())
                                .totalRequested(progress.totalCount())
                                .successfullyCreated(progress.successCount())
                                .failed(progress.errors().size())
                                .createdCouponCodes(progress.createdCodes())
                                .errors(progress.errors())
                                .progressPercentage(progressPct)
                                .build();

                return responseBuilder.buildUserResponse(response, HttpStatus.OK, "Job status retrieved");
        }

        @Override
        public UnravelDocsResponse<Void> cancelJob(String jobId, User user) {
                log.info("User {} attempting to cancel job: {}", user.getEmail(), jobId);

                // Note: Kafka doesn't support message cancellation directly
                // TODO: Implement additional logic for checking a cancellation flag in Redis

                return responseBuilder.buildVoidResponse(
                                HttpStatus.NOT_IMPLEMENTED, "Job cancellation not yet supported");
        }
}
