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
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;

/**
 * {@code initKey()} runs in an instance initializer block, not
 * {@code @BeforeEach}, so it also executes for jqwik's {@code @Property}
 * methods (see UserAccountServiceImplTest for why {@code @BeforeEach} alone
 * is not enough when a class mixes {@code @Test} and {@code @Property}).
 */
class ConnectionSecretCipherTest {

    private final ConnectionSecretCipher cipher =
            new ConnectionSecretCipher(new ConnectionSecretProperties("a".repeat(32)));

    {
        cipher.initKey();
    }

    @Test
    void decryptReturnsTheOriginalPlaintext() {
        var encrypted = cipher.encrypt("s3cret-password");

        assertThat(cipher.decrypt(encrypted)).isEqualTo("s3cret-password");
    }

    @Test
    void encryptingTheSamePlaintextTwiceProducesDifferentCiphertext() {
        var first = cipher.encrypt("s3cret-password");
        var second = cipher.encrypt("s3cret-password");

        assertThat(first).isNotEqualTo(second);
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": decrypting an
     * encrypted value always yields the original plaintext, for any password.
     */
    @Property
    void encryptThenDecryptRoundTripsForAnyPassword(
            @ForAll @StringLength(min = 1, max = 200) String plaintext) {
        var encrypted = cipher.encrypt(plaintext);

        assertThat(cipher.decrypt(encrypted)).isEqualTo(plaintext);
    }
}
