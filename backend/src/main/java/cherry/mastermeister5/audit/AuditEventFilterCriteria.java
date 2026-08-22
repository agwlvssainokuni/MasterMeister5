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

package cherry.mastermeister5.audit;

import java.time.Instant;

/**
 * domain-entities.md (Unit 6) AuditEventFilterCriteria: filters limited to
 * {@link AuditEvent}'s real columns (nfr-design-patterns.md Question 7/BR-17).
 * Any field left {@code null} is not applied.
 */
public record AuditEventFilterCriteria(AuditEventType eventType, Long actorUserId, Instant fromDate, Instant toDate) {
}
