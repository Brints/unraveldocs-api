package com.extractor.unraveldocs.messaging.emailservice;

import com.extractor.unraveldocs.messaging.dto.EmailMessage;
import com.extractor.unraveldocs.messaging.emailservice.mailgun.service.MailgunEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;

@Service
@RequiredArgsConstructor
public class EmailOrchestratorService {

    private final MailgunEmailService mailgunEmailService;
    private final TemplateEngine templateEngine;

    public void sendEmail(EmailMessage emailMessage) {
        Context context = new Context();
        context.setVariables(emailMessage.getTemplateModel());

        String htmlBody = templateEngine.process(emailMessage.getTemplateName(), context);

        mailgunEmailService.sendHtmlEmail(
                emailMessage.getTo(),
                emailMessage.getSubject(),
                htmlBody
        );
    }

    public void sendEmailWithAttachment(EmailMessage emailMessage, File attachment) {
        Context context = new Context();
        context.setVariables(emailMessage.getTemplateModel());

        String htmlBody = templateEngine.process(emailMessage.getTemplateName(), context);

        mailgunEmailService.sendWithAttachment(
                emailMessage.getTo(),
                emailMessage.getSubject(),
                htmlBody,
                attachment
        );
    }
}