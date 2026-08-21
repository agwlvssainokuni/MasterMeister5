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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * component-methods.md: SecurityInfrastructureComponent#issueRefreshToken /
 * #rotateRefreshToken / #revokeRefreshToken / #detectReuseAndRevokeFamily.
 * business-rules.md BR-19〜BR-22.
 */
@Component
public class RefreshTokenService {

    private final RefreshTokenJpaRepository repository;
    private final SecureTokenGenerator tokenGenerator;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            RefreshTokenJpaRepository repository,
            SecureTokenGenerator tokenGenerator,
            JwtProperties jwtProperties) {
        this.repository = repository;
        this.tokenGenerator = tokenGenerator;
        this.jwtProperties = jwtProperties;
    }

    /** Starts a new token family (login). */
    @Transactional
    public IssuedRefreshToken issue(Long userId) {
        return issueForFamily(userId, UUID.randomUUID().toString());
    }

    /**
     * Verifies and rotates a presented refresh token. Returns empty when the
     * token is unknown or expired (caller should require re-login). Throws
     * {@link RefreshTokenReuseDetectedException} when an already-revoked
     * token is presented again (BR-21) — the whole family has already been
     * revoked by the time this method returns.
     */
    @Transactional
    public Optional<IssuedRefreshToken> rotate(String rawToken) {
        var hash = tokenGenerator.hash(rawToken);
        var tokenOpt = repository.findByTokenHash(hash);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }
        var token = tokenOpt.get();
        if (token.getRevokedAt() != null) {
            detectReuseAndRevokeFamily(token.getFamilyId());
            throw new RefreshTokenReuseDetectedException(token.getUserId());
        }
        if (!token.isUsable(Instant.now())) {
            return Optional.empty();
        }
        token.revoke();
        return Optional.of(issueForFamily(token.getUserId(), token.getFamilyId()));
    }

    /** Logout: revokes exactly the presented token (BR-22, other devices unaffected). */
    @Transactional
    public void revoke(String rawToken) {
        var hash = tokenGenerator.hash(rawToken);
        repository.findByTokenHash(hash).ifPresent(RefreshToken::revoke);
    }

    /** Deactivation (BR-10): revokes every active token for the user, across all families. */
    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public void detectReuseAndRevokeFamily(String familyId) {
        repository.revokeFamily(familyId, Instant.now());
    }

    private IssuedRefreshToken issueForFamily(Long userId, String familyId) {
        var rawToken = tokenGenerator.generate();
        var expiresAt = Instant.now().plus(Duration.ofHours(jwtProperties.refreshTokenTtlHours()));
        var entity = new RefreshToken(userId, familyId, tokenGenerator.hash(rawToken), expiresAt);
        repository.save(entity);
        return new IssuedRefreshToken(userId, rawToken, expiresAt);
    }
}
