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

package cherry.mastermeister5.mastermaintenance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cherry.mastermeister5.mastermaintenance.entity.ColumnCustomization;
import cherry.mastermeister5.mastermaintenance.entity.TableCustomization;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRule;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRuleType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

/** Covers TableCustomization/ColumnCustomization/ValidationRule together (Unit 5's owned entities). */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class MasterMaintenanceRepositoriesTest {

    @Autowired private TableCustomizationJpaRepository tableCustomizationRepository;
    @Autowired private ColumnCustomizationJpaRepository columnCustomizationRepository;
    @Autowired private ValidationRuleJpaRepository validationRuleRepository;

    @Test
    void findByConnectionIdAndSchemaNameAndTableNameFindsTheMatchingRow() {
        tableCustomizationRepository.save(new TableCustomization(1L, "public", "t1"));

        assertThat(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", "t1"))
                .isPresent();
        assertThat(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", "other"))
                .isEmpty();
        assertThat(tableCustomizationRepository.findAllByConnectionId(1L)).hasSize(1);
    }

    @Test
    void deleteAllByConnectionIdScopesToThatConnectionOnly() {
        tableCustomizationRepository.save(new TableCustomization(1L, "public", "t1"));
        tableCustomizationRepository.save(new TableCustomization(2L, "public", "t1"));

        tableCustomizationRepository.deleteAllByConnectionId(1L);

        assertThat(tableCustomizationRepository.findAllByConnectionId(1L)).isEmpty();
        assertThat(tableCustomizationRepository.findAllByConnectionId(2L)).hasSize(1);
    }

    @Test
    void columnCustomizationQueriesAndCascadeDeleteScopeCorrectly() {
        var tableCustomization = tableCustomizationRepository.save(new TableCustomization(1L, "public", "t1"));
        var columnA = columnCustomizationRepository.save(new ColumnCustomization(tableCustomization.getId(), "a"));
        columnCustomizationRepository.save(new ColumnCustomization(tableCustomization.getId(), "b"));
        validationRuleRepository.save(new ValidationRule(columnA.getId(), ValidationRuleType.REGEX, "^[0-9]+$", null, null));

        assertThat(columnCustomizationRepository.findAllByTableCustomizationId(tableCustomization.getId())).hasSize(2);
        assertThat(columnCustomizationRepository.findByTableCustomizationIdAndColumnName(tableCustomization.getId(), "a"))
                .isPresent();
        assertThat(validationRuleRepository.findAllByColumnCustomizationId(columnA.getId())).hasSize(1);

        validationRuleRepository.deleteAllByColumnCustomizationIdIn(List.of(columnA.getId()));
        columnCustomizationRepository.deleteAllByTableCustomizationId(tableCustomization.getId());

        assertThat(columnCustomizationRepository.findAllByTableCustomizationId(tableCustomization.getId())).isEmpty();
        assertThat(validationRuleRepository.findAllByColumnCustomizationId(columnA.getId())).isEmpty();
    }
}
