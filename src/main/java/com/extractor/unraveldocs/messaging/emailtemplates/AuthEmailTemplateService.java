package com.extractor.unraveldocs.messaging.emailtemplates;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthEmailTemplateService {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailMessage prepareVerificationEmail(String email, String firstName, String lastName, String token, String expiration) {
        String verificationUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/verify-email")
                .queryParam("email", email)
                .queryParam("token", token)
                .toUriString();

        return EmailMessage.builder()
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
    }
}