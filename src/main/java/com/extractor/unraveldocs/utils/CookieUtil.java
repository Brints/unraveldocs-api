package com.extractor.unraveldocs.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

/**
 * Utility class for managing the refresh token HttpOnly cookie.
 * Uses __Host- prefix in production (requires HTTPS) and falls back
 * to a plain name for local development.
 */
public final class CookieUtil {

    private static final String COOKIE_NAME_SECURE = "__Host-refresh_token";
    private static final String COOKIE_NAME_DEV = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private CookieUtil() {
        // utility class
    }

    /**
     * Adds the refresh token as an HttpOnly, Secure, SameSite=Strict cookie.
     *
     * @param response      the HTTP response to add the cookie to
     * @param refreshToken  the refresh token value
     * @param maxAgeSeconds the cookie max-age in seconds
     * @param secure        true for production (HTTPS), false for local dev (HTTP)
     */
    public static void addRefreshTokenCookie(HttpServletResponse response,
            String refreshToken,
            long maxAgeSeconds,
            boolean secure) {
        String cookieName = secure ? COOKIE_NAME_SECURE : COOKIE_NAME_DEV;

        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Clears the refresh token cookie by setting maxAge to 0.
     *
     * @param response the HTTP response
     * @param secure   true for production (HTTPS), false for local dev (HTTP)
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response, boolean secure) {
        String cookieName = secure ? COOKIE_NAME_SECURE : COOKIE_NAME_DEV;

        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Extracts the refresh token from the request cookies.
     *
     * @param request the HTTP request
     * @return the refresh token value, or null if not found
     */
    public static String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME_SECURE.equals(cookie.getName()) ||
                    COOKIE_NAME_DEV.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
