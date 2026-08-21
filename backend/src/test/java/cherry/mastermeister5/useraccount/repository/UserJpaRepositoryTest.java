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

import cherry.mastermeister5.useraccount.entity.User;
import cherry.mastermeister5.useraccount.entity.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class UserJpaRepositoryTest {

    @Autowired private UserJpaRepository repository;

    @Test
    void findByEmailReturnsTheSavedUser() {
        repository.save(new User("user@example.com", UserRole.GENERAL));

        assertThat(repository.findByEmail("user@example.com")).isPresent();
        assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void findByInvitationTokenHashReturnsTheMatchingUser() {
        var user = new User("invitee@example.com", UserRole.GENERAL);
        user.setInvitationToken("hash-abc", Instant.now().plusSeconds(3600));
        repository.save(user);

        assertThat(repository.findByInvitationTokenHash("hash-abc")).isPresent();
        assertThat(repository.findByInvitationTokenHash("hash-unknown")).isEmpty();
    }

    @Test
    void savingADuplicateEmailFails() {
        repository.save(new User("dup@example.com", UserRole.GENERAL));
        repository.flush();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> {
                            repository.save(new User("dup@example.com", UserRole.GENERAL));
                            repository.flush();
                        })
                .isInstanceOf(RuntimeException.class);
    }
}
