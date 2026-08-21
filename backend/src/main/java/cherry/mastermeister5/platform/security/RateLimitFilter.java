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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * nfr-design-patterns.md: SECURITY-11. Applied to every request; unauthenticated
 * public endpoints (login, password reset, invitation acceptance) rely on this
 * filter since they have no other throttling mechanism.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitBucketSource bucketSource;
    private final ErrorResponseFactory errorResponseFactory;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitBucketSource bucketSource,
            ErrorResponseFactory errorResponseFactory,
            ObjectMapper objectMapper) {
        this.bucketSource = bucketSource;
        this.errorResponseFactory = errorResponseFactory;
        this.objectMapper = objectMapper;
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
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String resolveClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
