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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.accesscontrol.entity.GroupMembership;
import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.repository.GroupMembershipJpaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionCacheServiceImplTest {

    private final GroupMembershipJpaRepository membershipRepository = mock(GroupMembershipJpaRepository.class);
    private final PermissionCacheServiceImpl cache = new PermissionCacheServiceImpl(membershipRepository);

    @Test
    void getCachedReturnsEmptyWhenNothingWasStored() {
        var key = new CacheKey(1L, 1L, ResourceLevel.SCHEMA, "public", null, null);

        assertThat(cache.getCached(key)).isEmpty();
    }

    @Test
    void putThenGetCachedReturnsTheStoredValue() {
        var key = new CacheKey(1L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var value = new EffectivePermission(PrimaryLevel.READ, false, false);

        cache.put(key, value);

        assertThat(cache.getCached(key)).contains(value);
    }

    @Test
    void invalidateByUserRemovesOnlyThatUsersEntries() {
        var keyForUser1 = new CacheKey(1L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var keyForUser2 = new CacheKey(2L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var value = new EffectivePermission(PrimaryLevel.READ, false, false);
        cache.put(keyForUser1, value);
        cache.put(keyForUser2, value);

        cache.invalidateByUser(1L);

        assertThat(cache.getCached(keyForUser1)).isEmpty();
        assertThat(cache.getCached(keyForUser2)).contains(value);
    }

    @Test
    void invalidateByConnectionRemovesOnlyThatConnectionsEntries() {
        var keyForConnection1 = new CacheKey(1L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var keyForConnection2 = new CacheKey(1L, 2L, ResourceLevel.SCHEMA, "public", null, null);
        var value = new EffectivePermission(PrimaryLevel.READ, false, false);
        cache.put(keyForConnection1, value);
        cache.put(keyForConnection2, value);

        cache.invalidateByConnection(1L);

        assertThat(cache.getCached(keyForConnection1)).isEmpty();
        assertThat(cache.getCached(keyForConnection2)).contains(value);
    }

    @Test
    void invalidateByGroupRemovesCacheEntriesForCurrentMembersOnly() {
        when(membershipRepository.findAllByGroupId(9L))
                .thenReturn(List.of(new GroupMembership(9L, 1L), new GroupMembership(9L, 2L)));
        var keyForMember = new CacheKey(1L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var keyForOtherUser = new CacheKey(3L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var value = new EffectivePermission(PrimaryLevel.READ, false, false);
        cache.put(keyForMember, value);
        cache.put(keyForOtherUser, value);

        cache.invalidateByGroup(9L);

        assertThat(cache.getCached(keyForMember)).isEmpty();
        assertThat(cache.getCached(keyForOtherUser)).contains(value);
    }
}
