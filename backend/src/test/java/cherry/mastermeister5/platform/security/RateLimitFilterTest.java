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
import com.fasterxml.jackson.databind.ObjectMapper;
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
        var filter =
                new RateLimitFilter(bucketSource, errorResponseFactory, new ObjectMapper());
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        var secondResponse = new MockHttpServletResponse();
        when(errorResponseFactory.create(eq("RATE_LIMITED"), eq("errors.rate_limited"), any(Locale.class)))
                .thenReturn(new ErrorResponse("RATE_LIMITED", "too many requests", null));
        filter.doFilter(request, secondResponse, filterChain);

        assertThat(secondResponse.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(any(), eq(secondResponse));
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
