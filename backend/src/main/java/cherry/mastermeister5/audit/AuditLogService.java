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

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * component-methods.md: AuditLogComponent. The REST/UI surface for
 * {@link #listEvents} is Unit 6's responsibility (unit-of-work.md); Unit 2
 * only builds the recording mechanism and the internal read method.
 */
public interface AuditLogService {

    void recordEvent(
            AuditEventType eventType, Long actorUserId, Long targetUserId, Map<String, Object> details);

    Page<AuditEvent> listEvents(Pageable pageable);

    /**
     * nfr-design-patterns.md (Unit 6) Question 3: non-breaking overload added
     * for Unit 6's audit log viewing API, filtered by
     * {@link AuditEventFilterCriteria}.
     */
    Page<AuditEvent> listEvents(AuditEventFilterCriteria filterCriteria, Pageable pageable);
}
