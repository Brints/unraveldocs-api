package com.extractor.unraveldocs.config;

import com.extractor.unraveldocs.auth.config.RoleEnumConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final RoleEnumConverter roleEnumConverter;

    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(roleEnumConverter);
    }
}
