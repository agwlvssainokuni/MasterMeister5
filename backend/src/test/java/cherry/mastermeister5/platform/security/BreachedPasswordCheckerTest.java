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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BreachedPasswordCheckerTest {

    private final BreachedPasswordChecker checker = new BreachedPasswordChecker();

    @BeforeEach
    void loadList() {
        checker.loadList();
    }

    @Test
    void detectsAKnownCommonPassword() {
        assertThat(checker.isBreached("123456")).isTrue();
    }

    @Test
    void doesNotFlagAnUncommonPassword() {
        assertThat(checker.isBreached("xk7#mQ2!zP9$wR4^bN6&")).isFalse();
    }
}
