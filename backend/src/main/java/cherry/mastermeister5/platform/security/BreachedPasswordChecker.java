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

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * nfr-requirements.md Question 2 = A: an embedded static list, loaded once at
 * startup into memory, rather than an external API call. Matching is exact
 * (case-sensitive); no normalization is applied (BR-12).
 */
@Component
public class BreachedPasswordChecker {

    private static final String RESOURCE_PATH = "security/common-passwords.txt";

    private final Set<String> knownPasswords = new HashSet<>();

    @PostConstruct
    void loadList() {
        var resource = new ClassPathResource(RESOURCE_PATH);
        try (var reader =
                new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines().map(String::strip).filter(line -> !line.isEmpty()).forEach(knownPasswords::add);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load breached password list", e);
        }
    }

    /** @return true if the password appears in the known-breached/common list */
    public boolean isBreached(String rawPassword) {
        return knownPasswords.contains(rawPassword);
    }
}
