package com.extractor.unraveldocs.messaging.emailtemplates;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.EmailOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthEmailTemplateService {

    private final EmailOrchestratorService emailOrchestratorService;

    @Value("${app.base.url}")
    private String baseUrl;

    public void sendVerificationEmail(String email, String firstName, String lastName, String token, String expiration) {
        String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + token;

        EmailMessage message = EmailMessage.builder()
                .to(email)
                .subject("Email Verification Token")
                .templateName("emailVerificationToken")
                .templateModel(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "verificationUrl", verificationUrl,
                        "expiration", expiration
                ))
                .build();

        emailOrchestratorService.sendEmail(message);
    }
}