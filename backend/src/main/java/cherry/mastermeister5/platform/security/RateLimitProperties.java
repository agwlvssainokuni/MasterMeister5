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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * nfr-design-patterns.md: bucket4j token bucket, IP-address scoped
 * (default: 10 requests / minute, refilled once per minute).
 */
@ConfigurationProperties(prefix = "mastermeister5.security.rate-limit")
public record RateLimitProperties(int capacity, int refillTokens, int refillPeriodSeconds) {
}
