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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private ErrorResponseFactory errorResponseFactory;
    @Mock private FilterChain filterChain;

    @Test
    void requestBeyondCapacityIsRejectedWith429() throws Exception {
        // capacity=1: the first request consumes the only token, the second is throttled.
        var bucketSource = new SingleBucketSource(1, 1, Duration.ofMinutes(1));
        var filter = new RateLimitFilter(bucketSource, errorResponseFactory);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        // shouldNotFilter() scopes RateLimitFilter to the unauthenticated public
        // endpoints only (see RateLimitFilter's class Javadoc).
        request.setRequestURI("/api/auth/login");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        var secondResponse = new MockHttpServletResponse();
        when(errorResponseFactory.create(eq("RATE_LIMITED"), eq("errors.rate_limited"), any(Locale.class)))
                .thenReturn(new ErrorResponse("RATE_LIMITED", "too many requests", null));
        filter.doFilter(request, secondResponse, filterChain);

        assertThat(secondResponse.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(any(), eq(secondResponse));
    }

    @Test
    void staticAssetsAreNeverRateLimitedEvenAfterCapacityIsExhausted() throws Exception {
        // capacity=1, already exhausted by the first request below.
        var bucketSource = new SingleBucketSource(1, 1, Duration.ofMinutes(1));
        var filter = new RateLimitFilter(bucketSource, errorResponseFactory);
        var apiRequest = new MockHttpServletRequest();
        apiRequest.setRemoteAddr("203.0.113.10");
        apiRequest.setRequestURI("/api/auth/login");
        filter.doFilter(apiRequest, new MockHttpServletResponse(), filterChain);

        var assetRequest = new MockHttpServletRequest();
        assetRequest.setRemoteAddr("203.0.113.10");
        assetRequest.setRequestURI("/assets/index.js");
        var assetResponse = new MockHttpServletResponse();

        filter.doFilter(assetRequest, assetResponse, filterChain);

        assertThat(assetResponse.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(assetRequest, assetResponse);
    }

    @Test
    void authenticatedApiCallsAreNeverRateLimitedEvenAfterCapacityIsExhausted() throws Exception {
        // capacity=1, already exhausted by the first request below. Authenticated
        // /api/** traffic (e.g. /api/admin/users) is not one of the unauthenticated
        // public endpoints this filter protects, so it must never be throttled here
        // (a real admin session legitimately fires far more than 1-10 such calls
        // within 60 seconds just navigating between screens).
        var bucketSource = new SingleBucketSource(1, 1, Duration.ofMinutes(1));
        var filter = new RateLimitFilter(bucketSource, errorResponseFactory);
        var loginRequest = new MockHttpServletRequest();
        loginRequest.setRemoteAddr("203.0.113.10");
        loginRequest.setRequestURI("/api/auth/login");
        filter.doFilter(loginRequest, new MockHttpServletResponse(), filterChain);

        var adminRequest = new MockHttpServletRequest();
        adminRequest.setRemoteAddr("203.0.113.10");
        adminRequest.setRequestURI("/api/admin/users");
        var adminResponse = new MockHttpServletResponse();

        filter.doFilter(adminRequest, adminResponse, filterChain);

        assertThat(adminResponse.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(adminRequest, adminResponse);
    }

    private static final class SingleBucketSource implements RateLimitBucketSource {
        private final Bucket bucket;

        SingleBucketSource(int capacity, int refillTokens, Duration period) {
            this.bucket = Bucket.builder()
                    .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, period)))
                    .build();
        }

        @Override
        public Bucket bucketFor(String clientIp) {
            return bucket;
        }
    }
}
