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

package cherry.mastermeister5.accesscontrol.cache;

import cherry.mastermeister5.accesscontrol.repository.GroupMembershipJpaRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * nfr-design-patterns.md Question 2: single Caffeine cache, no TTL (only
 * event-driven invalidation), max 10,000 entries. invalidateByGroup/
 * invalidateByConnection scan {@link Cache#asMap()} rather than maintaining
 * a reverse index (acceptable at ~10 concurrent users' scale).
 */
@Service
class PermissionCacheServiceImpl implements PermissionCacheService {

    private final Cache<CacheKey, EffectivePermission> cache =
            Caffeine.newBuilder().maximumSize(10_000).build();

    private final GroupMembershipJpaRepository groupMembershipRepository;

    PermissionCacheServiceImpl(GroupMembershipJpaRepository groupMembershipRepository) {
        this.groupMembershipRepository = groupMembershipRepository;
    }

    @Override
    public Optional<EffectivePermission> getCached(CacheKey key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public void put(CacheKey key, EffectivePermission permission) {
        cache.put(key, permission);
    }

    @Override
    public void invalidateByUser(Long userId) {
        cache.asMap().keySet().removeIf(key -> key.userId().equals(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public void invalidateByGroup(Long groupId) {
        groupMembershipRepository.findAllByGroupId(groupId).stream()
                .map(membership -> membership.getUserId())
                .forEach(this::invalidateByUser);
    }

    @Override
    public void invalidateByConnection(Long connectionId) {
        cache.asMap().keySet().removeIf(key -> key.connectionId().equals(connectionId));
    }
}
