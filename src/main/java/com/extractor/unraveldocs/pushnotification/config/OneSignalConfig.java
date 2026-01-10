package com.extractor.unraveldocs.pushnotification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for OneSignal push notifications.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "onesignal")
@ConditionalOnProperty(name = "onesignal.enabled", havingValue = "true", matchIfMissing = false)
public class OneSignalConfig {

    private boolean enabled = false;
    private String appId;
    private String apiKey;
    private String apiUrl = "https://onesignal.com/api/v1";

    @Bean(name = "oneSignalRestTemplate")
    @ConditionalOnProperty(name = "onesignal.enabled", havingValue = "true")
    public RestTemplate oneSignalRestTemplate() {
        return new RestTemplate();
    }
}
