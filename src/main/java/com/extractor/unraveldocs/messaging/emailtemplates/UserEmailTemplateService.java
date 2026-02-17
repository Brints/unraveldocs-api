package com.extractor.unraveldocs.messaging.emailtemplates;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserEmailTemplateService {

        private final EmailOrchestratorService emailOrchestratorService;
        private final com.extractor.unraveldocs.brokers.service.EmailMessageProducerService emailMessageProducerService;

        @Value("${app.base.url}")
        private String baseUrl;

        @Value("${spring.application.name}")
        private String appName;

        @Value("${app.support.email}")
        private String supportEmail;

        @Value("${app.unsubscribe.url}")
        private String unsubscribeUrl;

        @Value("${app.frontend.url}")
        private String frontendUrl;

        public void sendPasswordResetToken(String email, String firstName, String lastName, String token,
                        String expiration) {
                var resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/auth/reset-password")
                                .queryParam("token", token)
                                .queryParam("email", email)
                                .toUriString();

                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Password Reset Token")
                                .templateName("passwordResetToken")
                                .templateModel(Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "resetUrl", resetUrl,
                                                "expiration", expiration))
                                .build();
                emailOrchestratorService.sendEmail(message);
        }

        public void sendSuccessfulPasswordReset(String email, String firstName, String lastName) {
                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Password Reset Successful")
                                .templateName("successfulPasswordReset")
                                .templateModel(Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName))
                                .build();
                emailOrchestratorService.sendEmail(message);
        }

        public void sendSuccessfulPasswordChange(String email, String firstName, String lastName) {
                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Password Change Successful")
                                .templateName("changePassword")
                                .templateModel(Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName))
                                .build();
                emailOrchestratorService.sendEmail(message);
        }

        public void scheduleUserDeletion(String email, String firstName, String lastName, OffsetDateTime deletionDate) {

                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Urgent! Your Account is Scheduled for Deletion")
                                .templateName("scheduleDeletion")
                                .templateModel(Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "deletionDate",
                                                deletionDate.format(
                                                                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))))
                                .build();
                emailOrchestratorService.sendEmail(message);
        }

        public void sendDeletedAccountEmail(String email) {
                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Your Account Has Been Deleted. \uD83D\uDE22")
                                .templateName("accountDeleted")
                                .templateModel(Map.of())
                                .build();
                emailOrchestratorService.sendEmail(message);
        }

        public void sendWelcomeEmail(String email, String firstName, String lastName) {
                String appUrl = baseUrl;

                var dashboardUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/dashboard")
                                .toUriString();

                String recipientName = ((firstName != null ? firstName : "").trim() + " "
                                + (lastName != null ? lastName : "").trim()).trim();
                String finalUnsubscribeUrl = (unsubscribeUrl == null || unsubscribeUrl.isBlank())
                                ? baseUrl + "/unsubscribe"
                                : unsubscribeUrl;

                EmailMessage message = EmailMessage.builder()
                                .to(email)
                                .subject("Welcome to " + appName)
                                .templateName("welcome")
                                .templateModel(Map.of(
                                                "recipientName", recipientName.isEmpty() ? "there" : recipientName,
                                                "appName", appName,
                                                "appUrl", appUrl,
                                                "dashboardUrl", dashboardUrl,
                                                "supportEmail", supportEmail,
                                                "unsubscribeUrl", finalUnsubscribeUrl))
                                .build();

                emailOrchestratorService.sendEmail(message);
        }

        public void sendTrialActivatedEmail(String email, String firstName, String lastName, String planName,
                        String expiryDate) {
                var dashboardUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/dashboard")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Free Trial Activated!",
                                "trial-activated",
                                Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "planName", planName,
                                                "expiryDate", expiryDate,
                                                "dashboardUrl", dashboardUrl));
        }

        public void sendTrialExpiringSoonEmail(String email, String firstName, String lastName, String expiryDate) {
                var billingUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/billing")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Trial Expiring Soon",
                                "trial-expiring-soon",
                                Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "expiryDate", expiryDate,
                                                "billingUrl", billingUrl));
        }

        public void sendTrialExpiredEmail(String email, String firstName, String lastName, String planName) {
                var billingUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/billing")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Your Free Trial Has Expired",
                                "trial-expired",
                                Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "planName", planName,
                                                "billingUrl", billingUrl));
        }

        public void sendCreditPurchaseEmail(String email, String firstName, String lastName,
                        String packName, int creditsAdded, int newBalance) {
                var dashboardUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/dashboard")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Credit Pack Purchase Successful!",
                                "credit-purchase",
                                Map.of(
                                                "firstName", firstName,
                                                "lastName", lastName,
                                                "packName", packName,
                                                "creditsAdded", String.valueOf(creditsAdded),
                                                "newBalance", String.valueOf(newBalance),
                                                "dashboardUrl", dashboardUrl));
        }

        public void sendCreditTransferSentEmail(String email, String firstName, String lastName, int creditsTransferred, String recipientName, int newBalance) {
                var dashboardUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/dashboard")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Credit Transfer Sent",
                                "credit-transfer",
                                Map.of(
                                                "firstName", firstName,
                                                "headerTitle", "Credits Sent Successfully",
                                                "summaryMessage",
                                                "You have successfully transferred credits to another user.",
                                                "creditsAmount", String.valueOf(creditsTransferred),
                                                "otherPartyLabel", "Sent To:",
                                                "otherPartyName", recipientName,
                                                "newBalance", String.valueOf(newBalance),
                                                "dashboardUrl", dashboardUrl));
        }

        public void sendCreditTransferReceivedEmail(String email, String firstName, String lastName, int creditsReceived, String senderName, int newBalance) {
                var dashboardUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/dashboard")
                                .toUriString();

                emailMessageProducerService.queueEmail(
                                email,
                                "Credits Received!",
                                "credit-transfer",
                                Map.of(
                                                "firstName", firstName,
                                                "headerTitle", "Credits Received! \uD83C\uDF89",
                                                "summaryMessage", "You have received credits from another user.",
                                                "creditsAmount", String.valueOf(creditsReceived),
                                                "otherPartyLabel", "From:",
                                                "otherPartyName", senderName,
                                                "newBalance", String.valueOf(newBalance),
                                                "dashboardUrl", dashboardUrl));
        }
}