/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.platform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * nfr-design-plan.md Question 3: the refresh token travels only as an
 * HttpOnly/Secure/SameSite=Strict cookie, never in a JSON response body or
 * readable by JavaScript. infrastructure-design.md Question 3: Secure is
 * always set — {@code http://localhost} is treated as a secure context by
 * major browsers, so local development is unaffected.
 */
@Component
public class AuthCookieSupport {

    static final String COOKIE_NAME = "mm5_refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    public void setRefreshTokenCookie(HttpServletResponse response, String rawToken, Instant expiresAt) {
        var maxAge = Duration.between(Instant.now(), expiresAt);
        var cookie =
                ResponseCookie.from(COOKIE_NAME, rawToken)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path(COOKIE_PATH)
                        .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        var cookie =
                ResponseCookie.from(COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path(COOKIE_PATH)
                        .maxAge(Duration.ZERO)
                        .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> readRefreshTokenCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
