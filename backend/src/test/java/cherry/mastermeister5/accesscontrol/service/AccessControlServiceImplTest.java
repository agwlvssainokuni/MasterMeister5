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

package cherry.mastermeister5.accesscontrol.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.accesscontrol.cache.CacheKey;
import cherry.mastermeister5.accesscontrol.cache.PermissionCacheService;
import cherry.mastermeister5.accesscontrol.entity.GroupMembership;
import cherry.mastermeister5.accesscontrol.entity.PermissionEntry;
import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import cherry.mastermeister5.accesscontrol.entity.UserGroup;
import cherry.mastermeister5.accesscontrol.repository.GroupMembershipJpaRepository;
import cherry.mastermeister5.accesscontrol.repository.PermissionEntryJpaRepository;
import cherry.mastermeister5.accesscontrol.repository.UserGroupJpaRepository;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.useraccount.entity.User;
import cherry.mastermeister5.useraccount.entity.UserRole;
import cherry.mastermeister5.useraccount.repository.UserJpaRepository;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

/**
 * Mocks and {@code service} are plain field initializers (not {@code @Mock}/
 * {@code @BeforeEach}) for the same reason as Unit 2/3's service tests: this
 * class mixes {@code @Test} and jqwik {@code @Property} methods, and jqwik
 * does not process JUnit Jupiter extensions.
 */
class AccessControlServiceImplTest {

    private final UserGroupJpaRepository groupRepository = mock(UserGroupJpaRepository.class);
    private final GroupMembershipJpaRepository membershipRepository = mock(GroupMembershipJpaRepository.class);
    private final PermissionEntryJpaRepository permissionRepository = mock(PermissionEntryJpaRepository.class);
    private final PermissionCacheService cacheService = mock(PermissionCacheService.class);
    private final PermissionYamlMapper yamlMapper = mock(PermissionYamlMapper.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final UserJpaRepository userRepository = mock(UserJpaRepository.class);
    private final DbSchemaJpaRepository schemaRepository = mock(DbSchemaJpaRepository.class);
    private final DbTableJpaRepository tableRepository = mock(DbTableJpaRepository.class);
    private final DbColumnJpaRepository columnRepository = mock(DbColumnJpaRepository.class);

    private final AccessControlServiceImpl service =
            new AccessControlServiceImpl(
                    groupRepository,
                    membershipRepository,
                    permissionRepository,
                    cacheService,
                    yamlMapper,
                    auditLogService,
                    userRepository,
                    schemaRepository,
                    tableRepository,
                    columnRepository);

    // --- group management ---

    @Test
    void createGroupRejectsADuplicateName() {
        when(groupRepository.findByName("sales")).thenReturn(Optional.of(group(1L, "sales")));

        assertThatThrownBy(() -> service.createGroup("sales", 99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("GROUP_NAME_ALREADY_EXISTS");
    }

    @Test
    void deleteGroupCascadesMembershipsAndPermissionEntries() {
        when(groupRepository.findById(2L)).thenReturn(Optional.of(group(2L, "sales")));

        service.deleteGroup(2L, 99L);

        verify(membershipRepository).deleteAllByGroupId(2L);
        verify(permissionRepository).deleteAllBySubjectTypeAndSubjectId(SubjectType.GROUP, 2L);
        verify(groupRepository).deleteById(2L);
        verify(cacheService).invalidateByGroup(2L);
    }

    @Test
    void addUserToGroupRejectsWhenAlreadyAMember() {
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group(3L, "sales")));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L, "a@example.com")));
        when(membershipRepository.findByGroupIdAndUserId(3L, 10L))
                .thenReturn(Optional.of(new GroupMembership(3L, 10L)));

        assertThatThrownBy(() -> service.addUserToGroup(3L, 10L, 99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("MEMBERSHIP_ALREADY_EXISTS");
    }

    @Test
    void addUserToGroupRejectsWhenTheUserDoesNotExist() {
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group(3L, "sales")));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addUserToGroup(3L, 10L, 99L)).isInstanceOf(UserAccountException.class);
    }

    @Test
    void removeUserFromGroupRejectsWhenNotAMember() {
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group(3L, "sales")));
        when(membershipRepository.findByGroupIdAndUserId(3L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeUserFromGroup(3L, 10L, 99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("MEMBERSHIP_NOT_FOUND");
    }

    // --- permission settings ---

    @Test
    void setPrimaryPermissionCreatesANewEntryAndInvalidatesTheUserCache() {
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.empty());

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(
                        SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, PrimaryLevel.READ),
                99L);

        verify(permissionRepository).save(any(PermissionEntry.class));
        verify(cacheService).invalidateByUser(5L);
    }

    @Test
    void setPrimaryPermissionUpdatesAnExistingEntryRatherThanDuplicatingIt() {
        var existing =
                new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.of(existing));

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(
                        SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, PrimaryLevel.UPDATE),
                99L);

        assertThat(existing.getPrimaryLevel()).isEqualTo(PrimaryLevel.UPDATE);
    }

    /**
     * Regression test: the UI's "-" option for the primary level select must
     * clear an existing override back to unset (falls back to the enclosing
     * schema/table, BR-11) rather than being rejected — found live when
     * reverting NONE/READ/UPDATE back to "-" silently did nothing.
     */
    @Test
    void setPrimaryPermissionClearsAnExistingEntryBackToUnsetWhenGivenNull() {
        var existing =
                new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        existing.setPrimaryLevel(PrimaryLevel.READ);
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.of(existing));

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, null),
                99L);

        assertThat(existing.getPrimaryLevel()).isNull();
    }

    /**
     * A row with nothing left set carries no information — once cleared back
     * to "-" with no auxiliary flags set either, the row itself should be
     * deleted rather than left behind as an all-null entry.
     */
    @Test
    void setPrimaryPermissionDeletesAnExistingEntryOnceItHasNothingLeftSet() {
        var existing =
                new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        existing.setPrimaryLevel(PrimaryLevel.READ);
        setId(existing, 42L);
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.of(existing));

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, null),
                99L);

        verify(permissionRepository).delete(existing);
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void setPrimaryPermissionKeepsAnExistingEntryWhenAuxiliaryFlagsAreStillSet() {
        var existing =
                new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        existing.setPrimaryLevel(PrimaryLevel.READ);
        existing.setAuxiliary(true, null);
        setId(existing, 42L);
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.of(existing));

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, null),
                99L);

        verify(permissionRepository).save(existing);
        verify(permissionRepository, never()).delete(any());
    }

    @Test
    void setPrimaryPermissionSkipsPersistingABrandNewEntryWhenGivenNull() {
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null))
                .thenReturn(Optional.empty());

        service.setPrimaryPermission(
                new SetPrimaryPermissionCommand(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null, null),
                99L);

        verify(permissionRepository, never()).save(any());
        verify(permissionRepository, never()).delete(any());
    }

    @Test
    void setAuxiliaryPermissionDeletesAnExistingEntryOnceItHasNothingLeftSet() {
        var existing =
                new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null);
        existing.setAuxiliary(true, false);
        setId(existing, 43L);
        when(permissionRepository
                        .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                                SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null))
                .thenReturn(Optional.of(existing));

        service.setAuxiliaryPermission(
                new SetAuxiliaryPermissionCommand(SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null, null),
                99L);

        verify(permissionRepository).delete(existing);
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void setAuxiliaryPermissionRejectsColumnLevel() {
        assertThatThrownBy(
                        () ->
                                service.setAuxiliaryPermission(
                                        new SetAuxiliaryPermissionCommand(
                                                SubjectType.USER,
                                                5L,
                                                1L,
                                                ResourceLevel.COLUMN,
                                                "public",
                                                "t1",
                                                true,
                                                false),
                                        99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("PERMISSION_AUXILIARY_NOT_APPLICABLE");
    }

    // --- resolveEffectivePermission ---

    @Test
    void resolveEffectivePermissionReturnsTheCachedValueWithoutQueryingRepositories() {
        var cacheKey = new CacheKey(5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        var cached = new cherry.mastermeister5.accesscontrol.cache.EffectivePermission(PrimaryLevel.UPDATE, false, false);
        when(cacheService.getCached(cacheKey)).thenReturn(Optional.of(cached));

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.SCHEMA, "public", null, null);

        assertThat(result).isEqualTo(cached);
        verify(membershipRepository, never()).findAllByUserId(anyLong());
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ": user setting always wins. */
    @Property
    void userOwnSettingAlwaysOverridesGroupComposition(
            @ForAll PrimaryLevel userLevel, @ForAll PrimaryLevel groupLevel) {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L)).thenReturn(List.of(new GroupMembership(7L, 5L)));
        var userEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        userEntry.setPrimaryLevel(userLevel);
        var groupEntry = new PermissionEntry(SubjectType.GROUP, 7L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        groupEntry.setPrimaryLevel(groupLevel);
        when(permissionRepository.findForResolution(5L, List.of(7L), 1L, "public"))
                .thenReturn(List.of(userEntry, groupEntry));

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.SCHEMA, "public", null, null);

        assertThat(result.primaryLevel()).isEqualTo(userLevel);
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ": group composition picks the most permissive value. */
    @Property
    void groupCompositionPicksTheMostPermissiveValue(
            @ForAll PrimaryLevel level1, @ForAll PrimaryLevel level2) {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L))
                .thenReturn(List.of(new GroupMembership(7L, 5L), new GroupMembership(8L, 5L)));
        var group1Entry = new PermissionEntry(SubjectType.GROUP, 7L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        group1Entry.setPrimaryLevel(level1);
        var group2Entry = new PermissionEntry(SubjectType.GROUP, 8L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        group2Entry.setPrimaryLevel(level2);
        when(permissionRepository.findForResolution(5L, List.of(7L, 8L), 1L, "public"))
                .thenReturn(List.of(group1Entry, group2Entry));

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.SCHEMA, "public", null, null);

        var expected = level1.compareTo(level2) >= 0 ? level1 : level2;
        assertThat(result.primaryLevel()).isEqualTo(expected);
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ": COLUMN beats TABLE regardless of value. */
    @Property
    void columnLevelSettingAlwaysWinsOverTableLevel(
            @ForAll PrimaryLevel columnLevel, @ForAll PrimaryLevel tableLevel) {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L)).thenReturn(List.of());
        var columnEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.COLUMN, "public", "t1", "c1");
        columnEntry.setPrimaryLevel(columnLevel);
        var tableEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null);
        tableEntry.setPrimaryLevel(tableLevel);
        when(permissionRepository.findForResolution(5L, List.of(-1L), 1L, "public"))
                .thenReturn(List.of(columnEntry, tableEntry));

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.COLUMN, "public", "t1", "c1");

        assertThat(result.primaryLevel()).isEqualTo(columnLevel);
    }

    @Test
    void resolveEffectivePermissionAllowsCreateOnlyWhenAllPrimaryKeyColumnsAreAtUpdateLevel() {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L)).thenReturn(List.of());
        var tableEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null);
        tableEntry.setAuxiliary(true, false);
        var pkColumnEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.COLUMN, "public", "t1", "id");
        pkColumnEntry.setPrimaryLevel(PrimaryLevel.UPDATE);
        when(permissionRepository.findForResolution(5L, List.of(-1L), 1L, "public"))
                .thenReturn(List.of(tableEntry, pkColumnEntry));

        var schema = new DbSchema(1L, "public");
        setId(schema, 100L);
        when(schemaRepository.findByConnectionIdAndSchemaName(1L, "public")).thenReturn(Optional.of(schema));
        var table = new DbTable(100L, "t1", DbTable.Type.TABLE, null);
        setId(table, 200L);
        when(tableRepository.findBySchemaIdAndTableName(100L, "t1")).thenReturn(Optional.of(table));
        when(columnRepository.findAllByTableId(200L))
                .thenReturn(List.of(new DbColumn(200L, "id", 1, "BIGINT", false, true, null)));

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.TABLE, "public", "t1", null);

        assertThat(result.canCreate()).isTrue();
    }

    @Test
    void resolveEffectivePermissionNeverAllowsDeleteWhenTheTableHasNoPrimaryKey() {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L)).thenReturn(List.of());
        var tableEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.TABLE, "public", "t1", null);
        tableEntry.setAuxiliary(true, true);
        when(permissionRepository.findForResolution(5L, List.of(-1L), 1L, "public")).thenReturn(List.of(tableEntry));
        when(schemaRepository.findByConnectionIdAndSchemaName(1L, "public")).thenReturn(Optional.empty());

        var result = service.resolveEffectivePermission(5L, 1L, ResourceLevel.TABLE, "public", "t1", null);

        assertThat(result.canDelete()).isFalse();
    }

    @Test
    void resolveEffectivePermissionsForTableResolvesAllColumnsFromASingleQuery() {
        when(cacheService.getCached(any())).thenReturn(Optional.empty());
        when(membershipRepository.findAllByUserId(5L)).thenReturn(List.of());
        var columnAEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.COLUMN, "public", "t1", "a");
        columnAEntry.setPrimaryLevel(PrimaryLevel.READ);
        var columnBEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.COLUMN, "public", "t1", "b");
        columnBEntry.setPrimaryLevel(PrimaryLevel.UPDATE);
        when(permissionRepository.findForResolution(5L, List.of(-1L), 1L, "public"))
                .thenReturn(List.of(columnAEntry, columnBEntry));

        var result =
                service.resolveEffectivePermissionsForTable(5L, 1L, "public", "t1", List.of("a", "b", "c"));

        assertThat(result.get("a").primaryLevel()).isEqualTo(PrimaryLevel.READ);
        assertThat(result.get("b").primaryLevel()).isEqualTo(PrimaryLevel.UPDATE);
        assertThat(result.get("c").primaryLevel()).isEqualTo(PrimaryLevel.NONE);
        verify(permissionRepository, org.mockito.Mockito.times(1)).findForResolution(5L, List.of(-1L), 1L, "public");
    }

    // --- YAML export/import ---

    @Test
    void importPermissionsReplacesExistingEntriesInASingleTransaction() {
        var yamlEntry =
                new PermissionYamlEntry(
                        SubjectType.USER, "a@example.com", ResourceLevel.SCHEMA, "public", null, null, PrimaryLevel.READ, null, null);
        when(yamlMapper.read("yaml-content")).thenReturn(new PermissionYamlDocument(List.of(yamlEntry)));
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user(5L, "a@example.com")));

        var result = service.importPermissions(1L, "yaml-content", 99L);

        assertThat(result.importedCount()).isEqualTo(1);
        verify(permissionRepository).deleteAllByConnectionId(1L);
        verify(permissionRepository).saveAll(any());
        verify(cacheService).invalidateByConnection(1L);
    }

    @Test
    void importPermissionsRejectsDuplicateEntries() {
        var entry1 =
                new PermissionYamlEntry(
                        SubjectType.USER, "a@example.com", ResourceLevel.SCHEMA, "public", null, null, PrimaryLevel.READ, null, null);
        var entry2 =
                new PermissionYamlEntry(
                        SubjectType.USER, "a@example.com", ResourceLevel.SCHEMA, "public", null, null, PrimaryLevel.UPDATE, null, null);
        when(yamlMapper.read("yaml-content")).thenReturn(new PermissionYamlDocument(List.of(entry1, entry2)));
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user(5L, "a@example.com")));

        assertThatThrownBy(() -> service.importPermissions(1L, "yaml-content", 99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("PERMISSION_DUPLICATE_ENTRY");
        verify(permissionRepository, never()).deleteAllByConnectionId(any());
    }

    @Test
    void importPermissionsRejectsAnUnresolvedSubject() {
        var yamlEntry =
                new PermissionYamlEntry(
                        SubjectType.USER,
                        "missing@example.com",
                        ResourceLevel.SCHEMA,
                        "public",
                        null,
                        null,
                        PrimaryLevel.READ,
                        null,
                        null);
        when(yamlMapper.read("yaml-content")).thenReturn(new PermissionYamlDocument(List.of(yamlEntry)));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importPermissions(1L, "yaml-content", 99L))
                .isInstanceOf(AccessControlException.class)
                .extracting("errorCode")
                .isEqualTo("PERMISSION_SUBJECT_NOT_RESOLVED");
    }

    @Test
    void exportPermissionsIdentifiesSubjectsByEmailAndGroupName() {
        var userEntry = new PermissionEntry(SubjectType.USER, 5L, 1L, ResourceLevel.SCHEMA, "public", null, null);
        when(permissionRepository.findAllByConnectionId(1L)).thenReturn(List.of(userEntry));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, "a@example.com")));
        when(yamlMapper.write(any())).thenReturn("yaml-output");

        var result = service.exportPermissions(1L, 99L);

        assertThat(result).isEqualTo("yaml-output");
    }

    // --- fixtures ---

    private UserGroup group(Long id, String name) {
        var group = new UserGroup(name);
        setId(group, id);
        return group;
    }

    private User user(Long id, String email) {
        var user = new User(email, UserRole.GENERAL);
        setId(user, id);
        return user;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
