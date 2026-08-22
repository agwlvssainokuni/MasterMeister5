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

package cherry.mastermeister5.accesscontrol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionYamlMapperTest {

    private final PermissionYamlMapper mapper = new PermissionYamlMapper();

    @Test
    void writeThenReadRoundTripsAllFields() {
        var document =
                new PermissionYamlDocument(
                        List.of(
                                new PermissionYamlEntry(
                                        SubjectType.USER,
                                        "a@example.com",
                                        ResourceLevel.TABLE,
                                        "public",
                                        "employee",
                                        null,
                                        PrimaryLevel.UPDATE,
                                        true,
                                        false),
                                new PermissionYamlEntry(
                                        SubjectType.GROUP,
                                        "sales-team",
                                        ResourceLevel.COLUMN,
                                        "public",
                                        "employee",
                                        "salary",
                                        PrimaryLevel.READ,
                                        null,
                                        null)));

        var yaml = mapper.write(document);
        var roundTripped = mapper.read(yaml);

        assertThat(roundTripped).isEqualTo(document);
    }

    @Test
    void readRejectsMalformedYaml() {
        assertThatThrownBy(() -> mapper.read("entries: [this is not valid: :"))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("PERMISSION_YAML_PARSE_FAILED");
    }
}
