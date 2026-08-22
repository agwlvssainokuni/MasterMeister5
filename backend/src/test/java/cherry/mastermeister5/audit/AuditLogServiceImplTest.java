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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * infrastructure-design.md Question 4: recordEvent must persist AND emit a
 * structured log line. The log side is exercised via the "AUDIT" logger name
 * (not asserted here — Logback wiring is covered at integration-test level);
 * this test focuses on the persistence contract.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private AuditEventJpaRepository repository;

    @Test
    void recordEventSavesAnAuditEventRow() {
        var service = new AuditLogServiceImpl(repository);

        service.recordEvent(AuditEventType.LOGIN_SUCCEEDED, 1L, 1L, Map.of("ip", "127.0.0.1"));

        verify(repository).save(any(AuditEvent.class));
    }

    /** nfr-design-patterns.md (Unit 6) Question 3: non-breaking overload delegates to the new search query. */
    @Test
    void listEventsWithFilterCriteriaDelegatesToRepositorySearch() {
        var service = new AuditLogServiceImpl(repository);
        var pageable = PageRequest.of(0, 50);
        var criteria = new AuditEventFilterCriteria(AuditEventType.LOGIN_SUCCEEDED, 1L, Instant.EPOCH, Instant.now());
        var expected = Page.<AuditEvent>empty();
        when(repository.search(
                        eq(AuditEventType.LOGIN_SUCCEEDED), eq(1L), any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(expected);

        var result = service.listEvents(criteria, pageable);

        assertThat(result).isSameAs(expected);
    }
}
