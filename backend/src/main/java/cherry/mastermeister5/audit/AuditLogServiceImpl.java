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

import cherry.mastermeister5.platform.logging.CorrelationIdFilter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * infrastructure-design.md Question 4: every event is written to the
 * {@code audit_event} table AND emitted to the structured application log
 * (SECURITY-03), so requirements.md 5-chapter's "log-based lightweight
 * detection" has something to alert on without waiting for Unit 6's viewer UI.
 */
@Service
class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final AuditEventJpaRepository repository;

    AuditLogServiceImpl(AuditEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void recordEvent(
            AuditEventType eventType, Long actorUserId, Long targetUserId, Map<String, Object> details) {
        var correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        var event = new AuditEvent(eventType, actorUserId, targetUserId, details, correlationId);
        repository.save(event);
        log.info(
                "auditEvent={} actorUserId={} targetUserId={}",
                eventType,
                actorUserId,
                targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> listEvents(Pageable pageable) {
        return repository.findAllByOrderByOccurredAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> listEvents(AuditEventFilterCriteria filterCriteria, Pageable pageable) {
        return repository.search(
                filterCriteria.eventType(),
                filterCriteria.actorUserId(),
                filterCriteria.fromDate(),
                filterCriteria.toDate(),
                pageable);
    }
}
