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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * nfr-design-patterns.md Question 5: standard {@link YAMLMapper}
 * configuration only — no polymorphic type resolution — so parsing is
 * limited to type-safe binding into {@link PermissionYamlDocument}.
 */
@Component
class PermissionYamlMapper {

    private final ObjectMapper mapper =
            new YAMLMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    String write(PermissionYamlDocument document) {
        try {
            return mapper.writeValueAsString(document);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    PermissionYamlDocument read(String yamlContent) {
        try {
            return mapper.readValue(yamlContent, PermissionYamlDocument.class);
        } catch (IOException e) {
            throw AccessControlException.yamlParseFailed();
        }
    }
}
