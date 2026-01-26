package com.extractor.unraveldocs.coupon.kafka;

import com.extractor.unraveldocs.coupon.dto.request.BulkCouponGenerationRequest;
import com.extractor.unraveldocs.coupon.dto.request.CreateCouponRequest;
import com.extractor.unraveldocs.coupon.enums.RecipientCategory;
import com.extractor.unraveldocs.coupon.model.CouponTemplate;
import com.extractor.unraveldocs.coupon.repository.CouponTemplateRepository;
import com.extractor.unraveldocs.coupon.service.CouponService;
import com.extractor.unraveldocs.documents.utils.SanitizeLogging;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kafka consumer for processing bulk coupon generation commands.
 * Only loaded when Kafka is enabled via coupon.kafka.enabled=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class CouponGenerationConsumer {

    private static final String JOB_STATUS_PREFIX = "coupon:job:";
    private static final Duration JOB_STATUS_TTL = Duration.ofDays(1);

    private final CouponService couponService;
    private final CouponTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SanitizeLogging sanitizer;

    // In-memory tracking for job status (backed by Redis for persistence)
    private final ConcurrentMap<String, JobProgress> jobProgressMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${coupon.kafka.topic:coupon-generation-events}", groupId = "${coupon.kafka.consumer-group:coupon-generation-consumer-group}", concurrency = "${coupon.bulk.kafka.concurrency:3}")
    public void processBulkGenerationCommand(BulkGenerationCommand command, Acknowledgment ack) {
        String jobId = command.getJobId();
        log.info("Received bulk generation command. JobId: {}, Quantity: {}",
                sanitizer.sanitizeLogging(jobId),
                sanitizer.sanitizeLoggingInteger(command.getRequest().getQuantity()));

        // Check for idempotency
        if (isJobAlreadyProcessed(jobId)) {
            log.warn("Job {} already processed, skipping", sanitizer.sanitizeLogging(jobId));
            ack.acknowledge();
            return;
        }

        // Mark job as processing
        updateJobStatus(jobId, "PROCESSING", 0, command.getRequest().getQuantity(), new ArrayList<>(), null);

        try {
            User createdBy = userRepository.findById(command.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + command.getUserId()));

            BulkCouponGenerationRequest request = command.getRequest();
            List<String> createdCodes = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Get template if specified
            CouponTemplate template = null;
            if (request.getTemplateId() != null) {
                template = templateRepository.findById(request.getTemplateId()).orElse(null);
            }

            // Generate coupons
            for (int i = 0; i < request.getQuantity(); i++) {
                try {
                    CreateCouponRequest couponRequest = buildCouponRequest(request, template, i);
                    var response = couponService.createCoupon(couponRequest, createdBy);

                    if (response.getStatusCode() == 201) {
                        createdCodes.add(response.getData().getCode());
                    } else {
                        errors.add("Coupon " + (i + 1) + ": " + response.getMessage());
                    }

                    // Update progress every 10 coupons
                    if ((i + 1) % 10 == 0) {
                        updateJobStatus(jobId, "PROCESSING", createdCodes.size(),
                                request.getQuantity(), createdCodes, errors);
                    }
                } catch (Exception e) {
                    errors.add("Coupon " + (i + 1) + ": " + e.getMessage());
                    log.error("Error creating coupon {} in job {}: {}", i + 1, sanitizer.sanitizeLogging(jobId),
                            e.getMessage());
                }
            }

            // Mark job as completed
            String finalStatus = errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
            updateJobStatus(jobId, finalStatus, createdCodes.size(), request.getQuantity(), createdCodes, errors);

            log.info("Bulk generation job {} completed. Created: {}, Errors: {}",
                    sanitizer.sanitizeLogging(jobId),
                    sanitizer.sanitizeLoggingInteger(createdCodes.size()),
                    sanitizer.sanitizeLoggingInteger(errors.size()));

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process bulk generation job: {}", sanitizer.sanitizeLogging(jobId), e);
            updateJobStatus(jobId, "FAILED", 0, command.getRequest().getQuantity(),
                    new ArrayList<>(), List.of(e.getMessage()));

            // Still acknowledge to prevent infinite retries
            ack.acknowledge();
        }
    }

    private CreateCouponRequest buildCouponRequest(
            BulkCouponGenerationRequest request, CouponTemplate template, int index) {

        CreateCouponRequest.CreateCouponRequestBuilder builder = CreateCouponRequest.builder();

        if (template != null) {
            // Use template values
            builder.description(template.getDescription())
                    .discountPercentage(template.getDiscountPercentage())
                    .minPurchaseAmount(template.getMinPurchaseAmount())
                    .recipientCategory(template.getRecipientCategory())
                    .maxUsageCount(template.getMaxUsageCount())
                    .maxUsagePerUser(template.getMaxUsagePerUser())
                    .validFrom(request.getValidFrom() != null ? request.getValidFrom() : OffsetDateTime.now())
                    .validUntil(request.getValidUntil() != null ? request.getValidUntil()
                            : OffsetDateTime.now().plusDays(template.getValidityDays()));
        } else {
            // Use request values
            builder.description(request.getDescription())
                    .discountPercentage(request.getDiscountPercentage())
                    .minPurchaseAmount(request.getMinPurchaseAmount())
                    .recipientCategory(request.getRecipientCategory() != null ? request.getRecipientCategory()
                            : RecipientCategory.ALL_PAID_USERS)
                    .specificUserIds(request.getSpecificUserIds())
                    .maxUsageCount(request.getMaxUsageCount())
                    .maxUsagePerUser(request.getMaxUsagePerUser())
                    .validFrom(request.getValidFrom() != null ? request.getValidFrom() : OffsetDateTime.now())
                    .validUntil(request.getValidUntil());
        }

        // Don't send notifications during bulk generation to avoid spam
        builder.sendNotifications(false);

        return builder.build();
    }

    private boolean isJobAlreadyProcessed(String jobId) {
        String status = redisTemplate.opsForValue().get(JOB_STATUS_PREFIX + jobId + ":status");
        return "COMPLETED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status);
    }

    private void updateJobStatus(String jobId, String status, int successCount,
            int totalCount, List<String> createdCodes, List<String> errors) {
        String key = JOB_STATUS_PREFIX + jobId;
        redisTemplate.opsForValue().set(key + ":status", status, JOB_STATUS_TTL);
        redisTemplate.opsForValue().set(key + ":success", String.valueOf(successCount), JOB_STATUS_TTL);
        redisTemplate.opsForValue().set(key + ":total", String.valueOf(totalCount), JOB_STATUS_TTL);
        redisTemplate.opsForValue().set(key + ":progress",
                String.valueOf((int) ((double) successCount / totalCount * 100)), JOB_STATUS_TTL);

        // Update in-memory map too
        jobProgressMap.put(jobId, new JobProgress(status, successCount, totalCount, createdCodes, errors));
    }

    public JobProgress getJobProgress(String jobId) {
        // Check in-memory first
        JobProgress progress = jobProgressMap.get(jobId);
        if (progress != null) {
            return progress;
        }

        // Fall back to Redis
        String key = JOB_STATUS_PREFIX + jobId;
        String status = redisTemplate.opsForValue().get(key + ":status");
        if (status == null) {
            return null;
        }

        String successStr = redisTemplate.opsForValue().get(key + ":success");
        String totalStr = redisTemplate.opsForValue().get(key + ":total");

        int success = successStr != null ? Integer.parseInt(successStr) : 0;
        int total = totalStr != null ? Integer.parseInt(totalStr) : 0;

        return new JobProgress(status, success, total, new ArrayList<>(), new ArrayList<>());
    }

    public record JobProgress(
            String status,
            int successCount,
            int totalCount,
            List<String> createdCodes,
            List<String> errors) {
    }
}
