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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Replaces {@link NoopJwtTokenValidator} (via Spring's normal bean-override
 * resolution — {@code NoopJwtTokenValidator} is
 * {@code @ConditionalOnMissingBean}) with the real signature/expiration
 * check now that {@link JwtTokenProvider} exists to issue tokens.
 */
@Component
public class JwtTokenValidatorImpl implements JwtTokenValidator {

    private final JwtTokenProvider tokenProvider;

    public JwtTokenValidatorImpl(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Optional<JwtAuthentication> validate(String accessToken) {
        try {
            var signedJwt = SignedJWT.parse(accessToken);
            if (!signedJwt.verify(new MACVerifier(tokenProvider.signingKey()))) {
                return Optional.empty();
            }
            var claims = signedJwt.getJWTClaimsSet();
            var expiresAt = claims.getExpirationTime();
            if (expiresAt == null || expiresAt.toInstant().isBefore(Instant.now())) {
                return Optional.empty();
            }
            var role = claims.getStringClaim("role");
            if (role == null) {
                return Optional.empty();
            }
            return Optional.of(new JwtAuthentication(claims.getSubject(), Set.of(role)));
        } catch (ParseException | JOSEException e) {
            return Optional.empty();
        }
    }
}
