package com.extractor.unraveldocs.messaging.emailservice.mailgun.service;

import com.extractor.unraveldocs.messaging.config.MailgunConfig;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.exception.MailGunException;
import com.mailgun.model.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class MailgunEmailService {

    private final MailgunMessagesApi mailgunMessagesApi;
    private final MailgunConfig mailgunConfig;

    @Autowired
    public MailgunEmailService(
            MailgunMessagesApi mailgunMessagesApi,
            MailgunConfig mailgunConfig) {
        this.mailgunMessagesApi = mailgunMessagesApi;
        this.mailgunConfig = mailgunConfig;
    }

    public void sendWithAttachment(String to, String subject, String body, File attachment) {
        Message message = Message.builder()
                .from(mailgunConfig.getMailgunFromEmail())
                .to(to)
                .subject(subject)
                .text(body)
                .attachment(attachment)
                .build();

        try {
            mailgunMessagesApi.sendMessage(mailgunConfig.getMailgunDomain(), message);
        } catch (MailGunException e) {
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Send HTML email with file attachment
     */
    public void sendHtmlWithAttachment(String to, String subject, String htmlBody, File attachment) {
        Message message = Message.builder()
                .from(mailgunConfig.getMailgunFromEmail())
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .attachment(attachment)
                .build();

        try {
            mailgunMessagesApi.sendMessage(mailgunConfig.getMailgunDomain(), message);
        } catch (MailGunException e) {
            throw new RuntimeException("Failed to send HTML email with attachment: " + e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        Message message = Message.builder()
                .from(mailgunConfig.getMailgunFromEmail())
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();

        try {
            mailgunMessagesApi.sendMessage(mailgunConfig.getMailgunDomain(), message);
        } catch (MailGunException e) {
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }
}
