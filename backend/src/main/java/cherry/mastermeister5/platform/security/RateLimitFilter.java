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

import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * nfr-design-patterns.md: SECURITY-11. Applied only to the handful of
 * unauthenticated public endpoints (login, refresh, logout, register,
 * password reset — the exact {@code permitAll()} list in
 * {@link SecurityConfig}), since those "rely on this filter [for throttling]
 * since they have no other [mechanism]" (this class's original intent, per
 * its own now-corrected Javadoc below).
 *
 * <p>An earlier version applied this to every request, then to every
 * {@code /api/**} request. Both were found to be wrong via E2E testing
 * (main-journey.spec.ts) actually driving the real app end to end for the
 * first time in the project's life (every prior manual test used Vite's dev
 * server, which never exercises this filter chain at all):
 * <ul>
 *   <li>Every request: the bundled SPA shell's first page load alone pulls in
 *   ~19 static files (JS, CSS, and the Noto font family's many
 *   weight/format variants, frontend-summary.md/build output), exceeding the
 *   capacity=10-per-60s budget before the page even finishes loading.
 *   <li>Every {@code /api/**} request: a single admin session — log in, open
 *   the users screen, invite someone, open the groups screen, create a
 *   group — legitimately fires far more than 10 authenticated API calls
 *   (each screen mount alone issues 1-3 GET calls to populate dropdowns/
 *   tables) well within 60 seconds, throttling normal use, not abuse.
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** Kept in sync with {@link SecurityConfig}'s permitAll list for these same paths. */
    private static final List<String> RATE_LIMITED_PATTERNS =
            List.of(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/logout",
                    "/api/auth/register",
                    "/api/auth/password/**");

    private final RateLimitBucketSource bucketSource;
    private final ErrorResponseFactory errorResponseFactory;
    // Self-instantiated, not Spring-injected: Spring Boot 4's JacksonAutoConfiguration
    // only registers a tools.jackson.databind.ObjectMapper (Jackson 3) bean, not this
    // com.fasterxml.jackson.databind.ObjectMapper (Jackson 2) type — same pattern
    // already used by CustomizationYamlMapper/PermissionYamlMapper/JsonMapConverter.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitBucketSource bucketSource, ErrorResponseFactory errorResponseFactory) {
        this.bucketSource = bucketSource;
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return RATE_LIMITED_PATTERNS.stream().noneMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var clientIp = resolveClientIp(request);
        var bucket = bucketSource.bucketFor(clientIp);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var errorResponse =
                errorResponseFactory.create(
                        "RATE_LIMITED", "errors.rate_limited", request.getLocale());
        // response.getWriter() uses the Servlet spec's ISO-8859-1 default character
        // encoding unless setCharacterEncoding() is called first, garbling non-ASCII
        // (Japanese) error messages. Writing to the byte OutputStream instead lets
        // Jackson encode as UTF-8 (its own default), sidestepping the Writer pitfall
        // entirely. Found via E2E testing (main-journey.spec.ts) — no test in this
        // project previously asserted on the actual bytes of a security error response.
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private String resolveClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
