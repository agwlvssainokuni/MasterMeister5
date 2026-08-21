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

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verifySucceedsForTheSamePassword() {
        var hash = hasher.hash("correct horse battery staple");

        assertThat(hasher.verify("correct horse battery staple", hash)).isTrue();
    }

    @Test
    void verifyFailsForADifferentPassword() {
        var hash = hasher.hash("correct horse battery staple");

        assertThat(hasher.verify("wrong password", hash)).isFalse();
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": hashing then
     * verifying the same password always succeeds, for any password.
     */
    @Property
    void hashThenVerifyRoundTripsForAnyPassword(
            @ForAll @AlphaChars @StringLength(min = 1, max = 64) String password) {
        var hash = hasher.hash(password);

        assertThat(hasher.verify(password, hash)).isTrue();
    }
}
