package com.extractor.unraveldocs.messaging.config;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MailgunConfig {
    @Value("${mailgun.api-key}")
    String mailgunApiKey;

    @Getter
    @Value("${mailgun.domain}")
    String mailgunDomain;

    @Getter
    @Value("${mailgun.from-email}")
    String mailgunFromEmail;

    @Bean
    public MailgunMessagesApi mailgunApi() {
        return MailgunClient.config(mailgunApiKey)
                .createApi(MailgunMessagesApi.class);
    }

}
