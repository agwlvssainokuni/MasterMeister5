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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenJpaRepository repository;

    private final SecureTokenGenerator tokenGenerator = new SecureTokenGenerator();
    private final JwtProperties jwtProperties = new JwtProperties("secret", 10, 24);

    private RefreshTokenService service;

    private RefreshTokenService newService() {
        return new RefreshTokenService(repository, tokenGenerator, jwtProperties);
    }

    @Test
    void issueSavesANewTokenWithAFreshFamilyId() {
        service = newService();

        service.issue(7L);

        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getFamilyId()).isNotBlank();
    }

    @Test
    void rotateRevokesTheOldTokenAndKeepsTheSameFamilyId() {
        service = newService();
        var rawToken = tokenGenerator.generate();
        var existing =
                new RefreshToken(7L, "family-1", tokenGenerator.hash(rawToken), Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(tokenGenerator.hash(rawToken))).thenReturn(java.util.Optional.of(existing));

        var rotated = service.rotate(rawToken);

        assertThat(rotated).isPresent();
        assertThat(existing.getRevokedAt()).isNotNull();
        var captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo("family-1");
    }

    @Test
    void rotateOnAnAlreadyRevokedTokenDetectsReuseAndRevokesTheFamily() {
        service = newService();
        var rawToken = tokenGenerator.generate();
        var existing =
                new RefreshToken(7L, "family-1", tokenGenerator.hash(rawToken), Instant.now().plusSeconds(60));
        existing.revoke();
        when(repository.findByTokenHash(tokenGenerator.hash(rawToken))).thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> service.rotate(rawToken))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(repository).revokeFamily(eq("family-1"), any(Instant.class));
    }

    @Test
    void rotateOnAnExpiredTokenFailsWithoutThrowing() {
        service = newService();
        var rawToken = tokenGenerator.generate();
        var expired =
                new RefreshToken(
                        7L, "family-1", tokenGenerator.hash(rawToken), Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByTokenHash(tokenGenerator.hash(rawToken))).thenReturn(java.util.Optional.of(expired));

        assertThat(service.rotate(rawToken)).isEmpty();
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": revoking a
     * family that is already fully revoked is a no-op (the underlying bulk
     * update only touches rows where revokedAt is still null).
     */
    @Test
    void detectReuseAndRevokeFamilyIsIdempotent() {
        service = newService();

        service.detectReuseAndRevokeFamily("family-1");
        service.detectReuseAndRevokeFamily("family-1");

        verify(repository, times(2)).revokeFamily(eq("family-1"), any(Instant.class));
    }
}
