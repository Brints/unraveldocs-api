package com.extractor.unraveldocs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws IOException {

        try {
            String token = resolveToken(request);

            if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if token is blacklisted first
            String jti = jwtTokenProvider.getJtiFromToken(token);
            if (jti != null && tokenBlacklistService.isTokenBlacklisted(jti)) {
                sendErrorResponse(
                        request,
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Token has been revoked",
                        "TOKEN_REVOKED");
                return;
            }

            jwtTokenProvider.validateToken(token);

            String email = jwtTokenProvider.getEmailFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            sendErrorResponse(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Token has expired",
                    "EXPIRED_TOKEN");
        } catch (MalformedJwtException | UnsupportedJwtException ex) {
            sendErrorResponse(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token",
                    "INVALID_TOKEN");
        } catch (SignatureException ex) {
            sendErrorResponse(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Token signature invalid",
                    "INVALID_SIGNATURE");
        } catch (Exception ex) {
            logger.error("Authentication failed in JWT filter", ex);
            sendErrorResponse(
                    request,
                    response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Authentication processing error",
                    "AUTH_PROCESSING_ERROR");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message,
            String errorCode) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", System.currentTimeMillis());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (errorCode != null) {
            body.put("errorCode", errorCode);
        }
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
