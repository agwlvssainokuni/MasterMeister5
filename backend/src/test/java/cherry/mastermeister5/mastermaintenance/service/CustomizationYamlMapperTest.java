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

package cherry.mastermeister5.mastermaintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cherry.mastermeister5.mastermaintenance.entity.InputWidget;
import cherry.mastermeister5.mastermaintenance.entity.SortDirection;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRuleType;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomizationYamlMapperTest {

    private final CustomizationYamlMapper mapper = new CustomizationYamlMapper();

    @Test
    void writeThenReadRoundTripsAllFields() {
        var document =
                new CustomizationYamlDocument(
                        List.of(
                                new CustomizationYamlEntry(
                                        "public",
                                        "employee",
                                        "name",
                                        SortDirection.ASC,
                                        List.of(
                                                new CustomizationYamlColumn(
                                                        "name",
                                                        "氏名",
                                                        1,
                                                        false,
                                                        false,
                                                        InputWidget.TEXT,
                                                        null,
                                                        List.of(new CustomizationYamlValidationRule(ValidationRuleType.REGEX, "^[A-Za-z]+$", null, null))),
                                                new CustomizationYamlColumn(
                                                        "department",
                                                        "部署",
                                                        2,
                                                        false,
                                                        true,
                                                        InputWidget.SELECT,
                                                        List.of(new SelectOption("eng", "Engineering")),
                                                        List.of())))));

        var yaml = mapper.write(document);
        var roundTripped = mapper.read(yaml);

        assertThat(roundTripped).isEqualTo(document);
    }

    @Test
    void readRejectsMalformedYaml() {
        assertThatThrownBy(() -> mapper.read("tables: [this is not valid: :"))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("CUSTOMIZATION_YAML_PARSE_FAILED");
    }
}
