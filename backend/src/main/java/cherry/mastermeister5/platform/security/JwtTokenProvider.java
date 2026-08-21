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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * component-methods.md: SecurityInfrastructureComponent#issueAccessToken.
 * nfr-design-plan.md Question 4: HS256 with a symmetric secret (environment
 * variable). The role claim is embedded (Functional Design Question 3 = B),
 * so a role change is reflected only on the next login/refresh.
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private OctetSequenceKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initSigningKey() {
        var secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "mastermeister5.security.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
        this.signingKey = new OctetSequenceKey.Builder(secretBytes).build();
    }

    public String issueAccessToken(Long userId, String role) {
        var now = Instant.now();
        var expiresAt = now.plus(Duration.ofMinutes(properties.accessTokenTtlMinutes()));
        var claims =
                new JWTClaimsSet.Builder()
                        .subject(String.valueOf(userId))
                        .claim("role", role)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(expiresAt))
                        .build();
        var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJwt.sign(new MACSigner(signingKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
        return signedJwt.serialize();
    }

    OctetSequenceKey signingKey() {
        return signingKey;
    }
}
