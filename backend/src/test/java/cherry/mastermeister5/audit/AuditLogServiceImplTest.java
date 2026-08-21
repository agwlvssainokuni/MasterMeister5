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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
