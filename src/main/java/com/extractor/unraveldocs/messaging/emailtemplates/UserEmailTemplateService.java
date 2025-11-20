package com.extractor.unraveldocs.messaging.emailtemplates;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserEmailTemplateService {

    private final EmailOrchestratorService emailOrchestratorService;

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

    public void sendPasswordResetToken(String email, String firstName, String lastName, String token, String expiration) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token + "&email=" + email;

        EmailMessage message = EmailMessage.builder()
                .to(email)
                .subject("Password Reset Token")
                .templateName("passwordResetToken")
                .templateModel(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "resetUrl", resetUrl,
                        "expiration", expiration
                ))
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
                        "lastName", lastName
                ))
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
                        "lastName", lastName
                ))
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
                        "deletionDate", deletionDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                ))
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
        String dashboardUrl = baseUrl + "/dashboard";
        String recipientName = ((firstName != null ? firstName : "").trim() + " " + (lastName != null ? lastName : "").trim()).trim();
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
                        "unsubscribeUrl", finalUnsubscribeUrl
                ))
                .build();

        emailOrchestratorService.sendEmail(message);
    }
}