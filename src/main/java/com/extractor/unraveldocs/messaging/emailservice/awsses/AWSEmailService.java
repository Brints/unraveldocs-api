package com.extractor.unraveldocs.messaging.emailservice.awsses;

import com.extractor.unraveldocs.messaging.config.AwsConfig;
import com.extractor.unraveldocs.exceptions.custom.EmailException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class AWSEmailService {
    private final SesClient sesClient;
    private final AwsConfig awsConfig;

    public AWSEmailService(SesClient sesClient, AwsConfig awsConfig) {
        this.sesClient = sesClient;
        this.awsConfig = awsConfig;
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .destination(d -> d.toAddresses(to))
                .message(m -> m
                        .subject(s -> s.data(subject))
                        .body(b -> b
                                .text(t -> t.data(body))))
                .source(awsConfig.getAwsFromEmail())
                .build();

        try {
            sesClient.sendEmail(sendEmailRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .destination(d -> d.toAddresses(to))
                .message(m -> m
                        .subject(s -> s.data(subject))
                        .body(b -> b
                                .html(h -> h.data(htmlBody))))
                .source(awsConfig.getAwsFromEmail())
                .build();

        try {
            sesClient.sendEmail(sendEmailRequest);
        } catch (SesException e) {
            switch (e) {
                case InvalidRenderingParameterException _ ->
                        throw new EmailException("Invalid rendering parameter: " + e.getMessage(), HttpStatus.BAD_REQUEST, e);
                case MessageRejectedException _ ->
                        throw new EmailException("Message rejected: " + e.getMessage(), HttpStatus.BAD_REQUEST, e);
                case AccountSendingPausedException _ ->
                        throw new EmailException("Account sending paused: " + e.getMessage(), HttpStatus.FORBIDDEN, e);
                case LimitExceededException _ ->
                        throw new EmailException("Limit exceeded: " + e.getMessage(), HttpStatus.TOO_MANY_REQUESTS, e);
                default -> throw new EmailException("Failed to send email: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        }
    }
}
