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

package cherry.mastermeister5.accesscontrol.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cherry.mastermeister5.accesscontrol.entity.GroupMembership;
import cherry.mastermeister5.accesscontrol.entity.PermissionEntry;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import cherry.mastermeister5.accesscontrol.entity.UserGroup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

/** Covers UserGroup/GroupMembership/PermissionEntry together (Unit 4's owned entities). */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class AccessControlRepositoriesTest {

    @Autowired private UserGroupJpaRepository groupRepository;
    @Autowired private GroupMembershipJpaRepository membershipRepository;
    @Autowired private PermissionEntryJpaRepository permissionRepository;

    @Test
    void groupNameIsUnique() {
        groupRepository.save(new UserGroup("sales"));
        groupRepository.flush();

        assertThatThrownBy(
                        () -> {
                            groupRepository.save(new UserGroup("sales"));
                            groupRepository.flush();
                        })
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void findByNameAndOrderedListingWork() {
        groupRepository.save(new UserGroup("b-team"));
        groupRepository.save(new UserGroup("a-team"));

        assertThat(groupRepository.findByName("a-team")).isPresent();
        assertThat(groupRepository.findAllByOrderByNameAsc()).extracting(UserGroup::getName)
                .containsExactly("a-team", "b-team");
    }

    @Test
    void membershipQueriesAndCascadeDeleteScopeCorrectly() {
        var group = groupRepository.save(new UserGroup("sales"));
        membershipRepository.save(new GroupMembership(group.getId(), 1L));
        membershipRepository.save(new GroupMembership(group.getId(), 2L));

        assertThat(membershipRepository.findAllByGroupId(group.getId())).hasSize(2);
        assertThat(membershipRepository.countByGroupId(group.getId())).isEqualTo(2);
        assertThat(membershipRepository.findByGroupIdAndUserId(group.getId(), 1L)).isPresent();

        membershipRepository.deleteByGroupIdAndUserId(group.getId(), 1L);
        assertThat(membershipRepository.findAllByGroupId(group.getId())).hasSize(1);

        membershipRepository.deleteAllByGroupId(group.getId());
        assertThat(membershipRepository.findAllByGroupId(group.getId())).isEmpty();
    }

    @Test
    void findByNaturalKeyMatchesNullTableAndColumnNames() {
        permissionRepository.save(
                new PermissionEntry(SubjectType.USER, 1L, 1L, ResourceLevel.SCHEMA, "public", null, null));

        var found =
                permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 1L, 1L, ResourceLevel.SCHEMA, "public", null, null);

        assertThat(found).isPresent();
    }

    @Test
    void findForResolutionCombinesTheUsersOwnAndGroupEntriesWithinTheSchema() {
        permissionRepository.save(
                new PermissionEntry(SubjectType.USER, 1L, 1L, ResourceLevel.SCHEMA, "public", null, null));
        permissionRepository.save(
                new PermissionEntry(SubjectType.GROUP, 9L, 1L, ResourceLevel.TABLE, "public", "t1", null));
        permissionRepository.save(
                new PermissionEntry(SubjectType.GROUP, 42L, 1L, ResourceLevel.TABLE, "public", "t1", null));
        // Different schema: must not be included.
        permissionRepository.save(
                new PermissionEntry(SubjectType.USER, 1L, 1L, ResourceLevel.SCHEMA, "other", null, null));

        var result = permissionRepository.findForResolution(1L, List.of(9L), 1L, "public");

        assertThat(result).hasSize(2);
    }

    @Test
    void deleteAllByConnectionIdAndBySubjectScopeToTheirTarget() {
        permissionRepository.save(
                new PermissionEntry(SubjectType.USER, 1L, 1L, ResourceLevel.SCHEMA, "public", null, null));
        permissionRepository.save(
                new PermissionEntry(SubjectType.GROUP, 9L, 1L, ResourceLevel.SCHEMA, "public", null, null));
        permissionRepository.save(
                new PermissionEntry(SubjectType.USER, 1L, 2L, ResourceLevel.SCHEMA, "public", null, null));

        permissionRepository.deleteAllBySubjectTypeAndSubjectId(SubjectType.GROUP, 9L);
        assertThat(permissionRepository.findAllByConnectionId(1L)).hasSize(1);

        permissionRepository.deleteAllByConnectionId(1L);
        assertThat(permissionRepository.findAllByConnectionId(1L)).isEmpty();
        assertThat(permissionRepository.findAllByConnectionId(2L)).hasSize(1);
    }
}
