package com.extractor.unraveldocs.messaging.emailtemplates;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserEmailTemplateService {

    private final EmailOrchestratorService emailOrchestratorService;

    public void sendPasswordResetToken(String email, String firstName, String lastName, String token, String expiration) {
        EmailMessage message = EmailMessage.builder()
                .to(email)
                .subject("Password Reset Token")
                .templateName("passwordResetToken")
                .templateModel(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "email", email,
                        "token", token,
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
                .templateName("successfulPasswordChange")
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
}
