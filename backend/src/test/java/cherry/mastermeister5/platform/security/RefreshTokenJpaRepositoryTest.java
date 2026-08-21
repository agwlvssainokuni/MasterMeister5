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

package cherry.mastermeister5.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class RefreshTokenJpaRepositoryTest {

    @Autowired private RefreshTokenJpaRepository repository;

    @Test
    void findByTokenHashReturnsTheSavedToken() {
        repository.save(new RefreshToken(1L, "family-1", "hash-1", Instant.now().plusSeconds(3600)));

        assertThat(repository.findByTokenHash("hash-1")).isPresent();
    }

    @Test
    void revokeFamilyOnlyTouchesUnrevokedRowsInThatFamily() {
        var t1 = repository.save(new RefreshToken(1L, "family-1", "hash-1", Instant.now().plusSeconds(3600)));
        var t2 = repository.save(new RefreshToken(1L, "family-1", "hash-2", Instant.now().plusSeconds(3600)));
        var other = repository.save(new RefreshToken(1L, "family-2", "hash-3", Instant.now().plusSeconds(3600)));

        repository.revokeFamily("family-1", Instant.now());
        repository.flush();

        assertThat(repository.findById(t1.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(repository.findById(t2.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(repository.findById(other.getId()).orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void revokeAllForUserOnlyTouchesThatUsersRows() {
        var mine = repository.save(new RefreshToken(1L, "family-1", "hash-1", Instant.now().plusSeconds(3600)));
        var someoneElses =
                repository.save(new RefreshToken(2L, "family-2", "hash-2", Instant.now().plusSeconds(3600)));

        repository.revokeAllForUser(1L, Instant.now());
        repository.flush();

        assertThat(repository.findById(mine.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(repository.findById(someoneElses.getId()).orElseThrow().getRevokedAt()).isNull();
    }
}
