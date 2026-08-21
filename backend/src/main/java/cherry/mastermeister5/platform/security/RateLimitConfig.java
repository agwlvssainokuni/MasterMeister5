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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RateLimitBucketSource rateLimitBucketSource(RateLimitProperties properties) {
        var buckets = new ConcurrentHashMap<String, Bucket>();
        return clientIp -> buckets.computeIfAbsent(clientIp, ip -> newBucket(properties));
    }

    private Bucket newBucket(RateLimitProperties properties) {
        var refill =
                Refill.greedy(
                        properties.refillTokens(),
                        Duration.ofSeconds(properties.refillPeriodSeconds()));
        var limit = Bandwidth.classic(properties.capacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
