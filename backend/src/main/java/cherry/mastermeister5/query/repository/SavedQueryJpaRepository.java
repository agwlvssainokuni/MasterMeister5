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

import cherry.mastermeister5.query.entity.QueryStatus;
import cherry.mastermeister5.query.entity.SavedQuery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedQueryJpaRepository extends JpaRepository<SavedQuery, Long> {

    /** BR-4: PUBLIC queries are visible to everyone, PRIVATE only to their creator. */
    @Query(
            "SELECT q FROM SavedQuery q WHERE q.status = :status AND "
                    + "(q.visibility = cherry.mastermeister5.query.entity.QueryVisibility.PUBLIC "
                    + "OR q.creatorUserId = :userId) "
                    + "ORDER BY q.name ASC")
    List<SavedQuery> findVisibleTo(@Param("status") QueryStatus status, @Param("userId") Long userId);
}
