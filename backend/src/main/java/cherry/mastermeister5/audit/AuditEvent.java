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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;

/**
 * SECURITY-13/14: append-only (no update/delete method is ever exposed —
 * see {@link AuditLogServiceImpl}). Single table with a JSON detail column
 * (tech-stack-decisions.md Question 7) so Units 3-6 can record their own
 * event types without a schema migration.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType eventType;

    private Long actorUserId;

    private Long targetUserId;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, Object> details;

    private String correlationId;

    @Column(nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            AuditEventType eventType,
            Long actorUserId,
            Long targetUserId,
            Map<String, Object> details,
            String correlationId) {
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.details = details;
        this.correlationId = correlationId;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
