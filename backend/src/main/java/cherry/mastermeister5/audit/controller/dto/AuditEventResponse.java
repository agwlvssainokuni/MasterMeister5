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

package cherry.mastermeister5.audit.controller.dto;

import cherry.mastermeister5.audit.AuditEvent;
import cherry.mastermeister5.audit.AuditEventType;
import java.time.Instant;
import java.util.Map;

public record AuditEventResponse(
        Long id,
        AuditEventType eventType,
        Long actorUserId,
        Long targetUserId,
        Map<String, Object> details,
        String correlationId,
        Instant occurredAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActorUserId(),
                event.getTargetUserId(),
                event.getDetails(),
                event.getCorrelationId(),
                event.getOccurredAt());
    }
}
