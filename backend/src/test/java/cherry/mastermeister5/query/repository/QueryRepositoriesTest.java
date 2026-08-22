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

import static org.assertj.core.api.Assertions.assertThat;

import cherry.mastermeister5.query.entity.QueryExecutionHistory;
import cherry.mastermeister5.query.entity.QueryStatus;
import cherry.mastermeister5.query.entity.QueryVisibility;
import cherry.mastermeister5.query.entity.SavedQuery;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.data.domain.PageRequest;

/** Covers SavedQuery/QueryExecutionHistory together (Unit 6's owned entities). */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class QueryRepositoriesTest {

    @Autowired private SavedQueryJpaRepository savedQueryRepository;
    @Autowired private QueryExecutionHistoryJpaRepository historyRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    // --- SavedQueryJpaRepository ---

    @Test
    void findVisibleToReturnsPublicQueriesAndTheCallersOwnPrivateQueries() {
        savedQueryRepository.save(new SavedQuery("public one", "SELECT 1", QueryVisibility.PUBLIC, 1L));
        savedQueryRepository.save(new SavedQuery("my private one", "SELECT 2", QueryVisibility.PRIVATE, 2L));
        savedQueryRepository.save(new SavedQuery("someone else's private", "SELECT 3", QueryVisibility.PRIVATE, 3L));

        var visible = savedQueryRepository.findVisibleTo(QueryStatus.ACTIVE, 2L);

        assertThat(visible).extracting(SavedQuery::getName).containsExactlyInAnyOrder("public one", "my private one");
    }

    @Test
    void findVisibleToExcludesRetiredQueries() {
        var savedQuery = savedQueryRepository.save(new SavedQuery("q1", "SELECT 1", QueryVisibility.PUBLIC, 1L));
        savedQuery.retire();
        savedQueryRepository.save(savedQuery);

        assertThat(savedQueryRepository.findVisibleTo(QueryStatus.ACTIVE, 1L)).isEmpty();
    }

    // --- QueryExecutionHistoryJpaRepository ---

    @Test
    void paramsMapRoundTripsThroughTheJsonColumn() {
        var saved =
                historyRepository.save(
                        new QueryExecutionHistory(
                                null, "SELECT * FROM t1 WHERE id = :id", 1L, "PUBLIC", Map.of("id", 5), 1, 12L, 9L));
        entityManager.flush();
        entityManager.clear();

        var reloaded = historyRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getParams()).containsEntry("id", 5);
    }

    @Test
    void searchFiltersByExecutorConnectionSchemaSqlTextAndDateRange() {
        historyRepository.save(new QueryExecutionHistory(null, "SELECT 1", 1L, "PUBLIC", Map.of(), 1, 5L, 9L));
        historyRepository.save(new QueryExecutionHistory(null, "SELECT 2", 1L, "PUBLIC", Map.of(), 1, 5L, 8L));
        historyRepository.save(new QueryExecutionHistory(null, "SELECT 3", 2L, "OTHER", Map.of(), 1, 5L, 9L));

        var page =
                historyRepository.search(9L, 1L, "PUBLIC", "select", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(QueryExecutionHistory::getSqlText).containsExactly("SELECT 1");
    }

    @Test
    void searchAppliesTheDateRangeFilter() {
        historyRepository.save(new QueryExecutionHistory(null, "SELECT 1", 1L, "PUBLIC", Map.of(), 1, 5L, 9L));

        var future = Instant.now().plusSeconds(3600);
        var page = historyRepository.search(null, null, null, null, future, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }
}
