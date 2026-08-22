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

package cherry.mastermeister5.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * tech-stack-decisions.md (Unit 6) Question 3: shared "bulk data access"
 * audit threshold referenced by both Unit 5's {@code listRecords} and Unit
 * 6's {@code executeQuery}. Kept in the shared {@code platform} package
 * (not in either unit's own package) so neither unit depends on the other.
 */
@ConfigurationProperties(prefix = "mastermeister5.bulk-access")
public record BulkAccessProperties(int threshold) {
}
