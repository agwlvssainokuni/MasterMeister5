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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventJpaRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    /** nfr-design-patterns.md (Unit 6) Question 3/BR-17: filter by real columns only. */
    @Query(
            "SELECT e FROM AuditEvent e WHERE "
                    + "(:eventType IS NULL OR e.eventType = :eventType) AND "
                    + "(:actorUserId IS NULL OR e.actorUserId = :actorUserId) AND "
                    + "(:fromDate IS NULL OR e.occurredAt >= :fromDate) AND "
                    + "(:toDate IS NULL OR e.occurredAt <= :toDate) "
                    + "ORDER BY e.occurredAt DESC")
    Page<AuditEvent> search(
            @Param("eventType") AuditEventType eventType,
            @Param("actorUserId") Long actorUserId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
