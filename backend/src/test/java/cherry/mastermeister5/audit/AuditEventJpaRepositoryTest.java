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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class AuditEventJpaRepositoryTest {

    @Autowired private AuditEventJpaRepository repository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void detailsMapRoundTripsThroughTheJsonColumn() {
        var saved =
                repository.save(
                        new AuditEvent(
                                AuditEventType.LOGIN_SUCCEEDED,
                                1L,
                                1L,
                                Map.of("ip", "127.0.0.1", "attempt", 1),
                                "corr-1"));
        entityManager.flush();
        entityManager.clear();

        var reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getDetails()).containsEntry("ip", "127.0.0.1");
    }

    @Test
    void findAllByOrderByOccurredAtDescReturnsNewestFirst() {
        repository.save(new AuditEvent(AuditEventType.LOGIN_SUCCEEDED, 1L, 1L, Map.of(), "corr-1"));
        repository.save(new AuditEvent(AuditEventType.LOGOUT, 1L, 1L, Map.of(), "corr-2"));

        var page = repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getEventType()).isEqualTo(AuditEventType.LOGOUT);
    }

    /** nfr-design-patterns.md (Unit 6) Question 3/BR-17: filter by real columns only. */
    @Test
    void searchFiltersByEventTypeAndActorAndDateRange() {
        repository.save(new AuditEvent(AuditEventType.LOGIN_SUCCEEDED, 1L, 1L, Map.of(), "corr-1"));
        repository.save(new AuditEvent(AuditEventType.LOGOUT, 1L, 1L, Map.of(), "corr-2"));
        repository.save(new AuditEvent(AuditEventType.LOGIN_SUCCEEDED, 2L, 2L, Map.of(), "corr-3"));

        var page =
                repository.search(
                        AuditEventType.LOGIN_SUCCEEDED, 1L, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getActorUserId()).isEqualTo(1L);
    }
}
