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

package cherry.mastermeister5.connectionschema.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class TargetConnectionJpaRepositoryTest {

    @Autowired private TargetConnectionJpaRepository repository;

    @Test
    void findByNameReturnsTheSavedConnection() {
        repository.save(
                new TargetConnection(
                        "conn1", RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "encrypted"));

        assertThat(repository.findByName("conn1")).isPresent();
        assertThat(repository.findByName("unknown")).isEmpty();
    }

    @Test
    void savingADuplicateNameFails() {
        repository.save(
                new TargetConnection(
                        "dup", RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "encrypted"));
        repository.flush();

        assertThatThrownBy(
                        () -> {
                            repository.save(
                                    new TargetConnection(
                                            "dup", RdbmsType.POSTGRESQL, "otherhost", 5432, "db2", null, null, "user2", "encrypted2"));
                            repository.flush();
                        })
                .isInstanceOf(RuntimeException.class);
    }
}
