package com.extractor.unraveldocs.messaging.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MailgunWebhookFilter extends OncePerRequestFilter {
    @Value("${mailgun.http.webhook.signingin-key}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws IOException, ServletException {
       if ("/api/webhook/mailgun".equals(request.getRequestURI())) {
            String token = request.getHeader("X-Mailgun-Signature");
            if (token == null || !token.equals(expectedToken)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Mailgun signature");
                return;
            }
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
