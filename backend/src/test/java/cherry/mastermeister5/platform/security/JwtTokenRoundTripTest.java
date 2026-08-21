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

class JwtTokenRoundTripTest {

    private JwtTokenProvider provider;
    private JwtTokenValidatorImpl validator;

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties("a".repeat(32), 10, 24);
        provider = new JwtTokenProvider(properties);
        provider.initSigningKey();
        validator = new JwtTokenValidatorImpl(provider);
    }

    @Test
    void issuedTokenValidatesToTheSameUserIdAndRole() {
        var token = provider.issueAccessToken(42L, "ADMIN");

        var authentication = validator.validate(token);

        assertThat(authentication).isPresent();
        assertThat(authentication.get().userId()).isEqualTo("42");
        assertThat(authentication.get().roles()).containsExactly("ADMIN");
    }

    @Test
    void tokenSignedWithADifferentKeyIsRejected() {
        var otherProperties = new JwtProperties("b".repeat(32), 10, 24);
        var otherProvider = new JwtTokenProvider(otherProperties);
        otherProvider.initSigningKey();
        var token = otherProvider.issueAccessToken(1L, "GENERAL");

        assertThat(validator.validate(token)).isEmpty();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThat(validator.validate("not-a-jwt")).isEmpty();
    }
}
