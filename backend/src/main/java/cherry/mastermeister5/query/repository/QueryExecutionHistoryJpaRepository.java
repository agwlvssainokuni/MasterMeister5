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

package cherry.mastermeister5.query.repository;

import cherry.mastermeister5.query.entity.QueryExecutionHistory;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueryExecutionHistoryJpaRepository extends JpaRepository<QueryExecutionHistory, Long> {

    /** US-4.6: filter by execution date, actor, target schema, and SQL text substring. */
    @Query(
            "SELECT h FROM QueryExecutionHistory h WHERE "
                    + "(:executedByUserId IS NULL OR h.executedByUserId = :executedByUserId) AND "
                    + "(:connectionId IS NULL OR h.connectionId = :connectionId) AND "
                    + "(:schemaName IS NULL OR h.schemaName = :schemaName) AND "
                    + "(:sqlTextContains IS NULL OR LOWER(h.sqlText) LIKE LOWER(CONCAT('%', :sqlTextContains, '%'))) AND "
                    + "(:fromDate IS NULL OR h.executedAt >= :fromDate) AND "
                    + "(:toDate IS NULL OR h.executedAt <= :toDate) "
                    + "ORDER BY h.executedAt DESC")
    Page<QueryExecutionHistory> search(
            @Param("executedByUserId") Long executedByUserId,
            @Param("connectionId") Long connectionId,
            @Param("schemaName") String schemaName,
            @Param("sqlTextContains") String sqlTextContains,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
