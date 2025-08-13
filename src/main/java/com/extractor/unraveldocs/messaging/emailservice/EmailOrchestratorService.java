package com.extractor.unraveldocs.messaging.emailservice;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.mailgun.service.MailgunEmailService;
import com.extractor.unraveldocs.messaging.thymleafservice.ThymeleafEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailOrchestratorService {

    private final MailgunEmailService mailgunEmailService;
    private final ThymeleafEmailService thymeleafEmailService;

    public void sendEmail(EmailMessage emailMessage) {
        String htmlBody = thymeleafEmailService.createEmail(
                emailMessage.getTemplateName(),
                emailMessage.getTemplateModel()
        );

        mailgunEmailService.sendHtmlEmail(
                emailMessage.getTo(),
                emailMessage.getSubject(),
                htmlBody
        );
    }

    public void sendEmailWithAttachment(EmailMessage emailMessage, File attachment) {
        String htmlBody = thymeleafEmailService.createEmail(
                emailMessage.getTemplateName(),
                emailMessage.getTemplateModel()
        );

        mailgunEmailService.sendWithAttachment(
                emailMessage.getTo(),
                emailMessage.getSubject(),
                htmlBody,
                attachment
        );
    }
}