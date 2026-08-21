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

package cherry.mastermeister5.useraccount.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cherry.mastermeister5.useraccount.entity.PasswordResetToken;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class PasswordResetTokenJpaRepositoryTest {

    @Autowired private PasswordResetTokenJpaRepository repository;

    @Test
    void findByTokenHashReturnsTheSavedToken() {
        repository.save(new PasswordResetToken(1L, "hash-1", Instant.now().plusSeconds(3600)));

        assertThat(repository.findByTokenHash("hash-1")).isPresent();
        assertThat(repository.findByTokenHash("hash-unknown")).isEmpty();
    }

    @Test
    void findAllByUserIdAndUsedAtIsNullExcludesUsedTokens() {
        var unused = new PasswordResetToken(2L, "hash-unused", Instant.now().plusSeconds(3600));
        var used = new PasswordResetToken(2L, "hash-used", Instant.now().plusSeconds(3600));
        used.markUsed();
        repository.save(unused);
        repository.save(used);

        var result = repository.findAllByUserIdAndUsedAtIsNull(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTokenHash()).isEqualTo("hash-unused");
    }
}
