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

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query(
            "update RefreshToken t set t.revokedAt = :now"
                    + " where t.familyId = :familyId and t.revokedAt is null")
    void revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query(
            "update RefreshToken t set t.revokedAt = :now"
                    + " where t.userId = :userId and t.revokedAt is null")
    void revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
