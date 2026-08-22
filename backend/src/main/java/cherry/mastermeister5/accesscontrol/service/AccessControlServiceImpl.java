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

import cherry.mastermeister5.accesscontrol.cache.CacheKey;
import cherry.mastermeister5.accesscontrol.cache.EffectivePermission;
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
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.useraccount.entity.User;
import cherry.mastermeister5.useraccount.repository.UserJpaRepository;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * business-logic-model.md / business-rules.md (BR-1〜BR-21).
 */
@Service
class AccessControlServiceImpl implements AccessControlService {

    /** nfr-design-patterns.md Question 4: same allowlist as Unit 3's validateIdentifier. */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final UserGroupJpaRepository groupRepository;
    private final GroupMembershipJpaRepository membershipRepository;
    private final PermissionEntryJpaRepository permissionRepository;
    private final PermissionCacheService cacheService;
    private final PermissionYamlMapper yamlMapper;
    private final AuditLogService auditLogService;
    private final UserJpaRepository userRepository;
    private final DbSchemaJpaRepository schemaRepository;
    private final DbTableJpaRepository tableRepository;
    private final DbColumnJpaRepository columnRepository;

    AccessControlServiceImpl(
            UserGroupJpaRepository groupRepository,
            GroupMembershipJpaRepository membershipRepository,
            PermissionEntryJpaRepository permissionRepository,
            PermissionCacheService cacheService,
            PermissionYamlMapper yamlMapper,
            AuditLogService auditLogService,
            UserJpaRepository userRepository,
            DbSchemaJpaRepository schemaRepository,
            DbTableJpaRepository tableRepository,
            DbColumnJpaRepository columnRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.permissionRepository = permissionRepository;
        this.cacheService = cacheService;
        this.yamlMapper = yamlMapper;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.schemaRepository = schemaRepository;
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
    }

    // --- Group management (US-2.7) ---

    @Override
    @Transactional
    public Long createGroup(String name, Long actorUserId) {
        if (groupRepository.findByName(name).isPresent()) {
            throw AccessControlException.groupNameAlreadyExists();
        }
        var group = groupRepository.save(new UserGroup(name));
        auditLogService.recordEvent(AuditEventType.GROUP_CREATED, actorUserId, null, Map.of("groupName", name));
        return group.getId();
    }

    @Override
    @Transactional
    public void renameGroup(Long groupId, String name, Long actorUserId) {
        var group = findGroupOrThrow(groupId);
        if (!group.getName().equals(name) && groupRepository.findByName(name).isPresent()) {
            throw AccessControlException.groupNameAlreadyExists();
        }
        group.rename(name);
        groupRepository.save(group);
        auditLogService.recordEvent(
                AuditEventType.GROUP_RENAMED, actorUserId, null, Map.of("groupId", groupId, "groupName", name));
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long actorUserId) {
        findGroupOrThrow(groupId);
        // BR-2: cascade. Cache invalidation reads current membership, so it
        // must run before the membership rows are deleted.
        cacheService.invalidateByGroup(groupId);
        membershipRepository.deleteAllByGroupId(groupId);
        permissionRepository.deleteAllBySubjectTypeAndSubjectId(SubjectType.GROUP, groupId);
        groupRepository.deleteById(groupId);
        auditLogService.recordEvent(AuditEventType.GROUP_DELETED, actorUserId, null, Map.of("groupId", groupId));
    }

    @Override
    @Transactional
    public void addUserToGroup(Long groupId, Long userId, Long actorUserId) {
        findGroupOrThrow(groupId);
        if (userRepository.findById(userId).isEmpty()) {
            throw UserAccountException.userNotFound();
        }
        if (membershipRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw AccessControlException.membershipAlreadyExists();
        }
        membershipRepository.save(new GroupMembership(groupId, userId));
        cacheService.invalidateByUser(userId);
        auditLogService.recordEvent(
                AuditEventType.GROUP_MEMBER_ADDED, actorUserId, userId, Map.of("groupId", groupId));
    }

    @Override
    @Transactional
    public void removeUserFromGroup(Long groupId, Long userId, Long actorUserId) {
        findGroupOrThrow(groupId);
        if (membershipRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw AccessControlException.membershipNotFound();
        }
        membershipRepository.deleteByGroupIdAndUserId(groupId, userId);
        cacheService.invalidateByUser(userId);
        auditLogService.recordEvent(
                AuditEventType.GROUP_MEMBER_REMOVED, actorUserId, userId, Map.of("groupId", groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummary> listGroups() {
        return groupRepository.findAllByOrderByNameAsc().stream()
                .map(g -> new GroupSummary(g.getId(), g.getName(), membershipRepository.countByGroupId(g.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberView> listMembers(Long groupId) {
        return membershipRepository.findAllByGroupId(groupId).stream()
                .map(m -> userRepository.findById(m.getUserId()).orElseThrow(UserAccountException::userNotFound))
                .map(u -> new GroupMemberView(u.getId(), u.getEmail(), u.getName()))
                .toList();
    }

    // --- Permission settings (US-2.4) ---

    @Override
    @Transactional
    public void setPrimaryPermission(SetPrimaryPermissionCommand command, Long actorUserId) {
        validateResourcePath(
                command.resourceLevel(), command.schemaName(), command.tableName(), command.columnName());
        var entry =
                findOrCreateEntry(
                        command.subjectType(),
                        command.subjectId(),
                        command.connectionId(),
                        command.resourceLevel(),
                        command.schemaName(),
                        command.tableName(),
                        command.columnName());
        entry.setPrimaryLevel(command.primaryLevel());
        permissionRepository.save(entry);
        invalidateForSubject(command.subjectType(), command.subjectId());
        recordPermissionChanged(command.subjectType(), command.subjectId(), command.connectionId(), actorUserId);
    }

    @Override
    @Transactional
    public void setAuxiliaryPermission(SetAuxiliaryPermissionCommand command, Long actorUserId) {
        if (command.resourceLevel() == ResourceLevel.COLUMN) {
            throw AccessControlException.auxiliaryNotApplicable();
        }
        validateResourcePath(command.resourceLevel(), command.schemaName(), command.tableName(), null);
        var entry =
                findOrCreateEntry(
                        command.subjectType(),
                        command.subjectId(),
                        command.connectionId(),
                        command.resourceLevel(),
                        command.schemaName(),
                        command.tableName(),
                        null);
        entry.setAuxiliary(command.auxCreate(), command.auxDelete());
        permissionRepository.save(entry);
        invalidateForSubject(command.subjectType(), command.subjectId());
        recordPermissionChanged(command.subjectType(), command.subjectId(), command.connectionId(), actorUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionEntryView> listPermissionEntries(Long connectionId, SubjectType subjectType, Long subjectId) {
        return permissionRepository
                .findAllByConnectionIdAndSubjectTypeAndSubjectId(connectionId, subjectType, subjectId)
                .stream()
                .map(
                        e ->
                                new PermissionEntryView(
                                        e.getSubjectType(),
                                        e.getSubjectId(),
                                        e.getResourceLevel(),
                                        e.getSchemaName(),
                                        e.getTableName(),
                                        e.getColumnName(),
                                        e.getPrimaryLevel(),
                                        e.getAuxCreate(),
                                        e.getAuxDelete()))
                .toList();
    }

    // --- Effective permission resolution (US-2.4) ---

    @Override
    @Transactional(readOnly = true)
    public EffectivePermission resolveEffectivePermission(
            Long userId,
            Long connectionId,
            ResourceLevel resourceLevel,
            String schemaName,
            String tableName,
            String columnName) {
        var cacheKey = new CacheKey(userId, connectionId, resourceLevel, schemaName, tableName, columnName);
        var cached = cacheService.getCached(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        var groupIds = membershipRepository.findAllByUserId(userId).stream().map(GroupMembership::getGroupId).toList();
        var queryGroupIds = groupIds.isEmpty() ? List.of(-1L) : groupIds;
        var entries = permissionRepository.findForResolution(userId, queryGroupIds, connectionId, schemaName);

        var primaryLevel = resolvePrimaryLevel(entries, userId, groupIds, resourceLevel, tableName, columnName);

        var canCreate = false;
        var canDelete = false;
        if (resourceLevel == ResourceLevel.TABLE) {
            var auxCreate = resolveAux(entries, userId, groupIds, tableName, true);
            var auxDelete = resolveAux(entries, userId, groupIds, tableName, false);
            var pkColumns = findPrimaryKeyColumns(connectionId, schemaName, tableName);
            if (pkColumns.isEmpty()) {
                // business-rules.md BR-12: no primary key => create needs only the aux flag.
                canCreate = auxCreate;
                canDelete = false;
            } else {
                var allAtLeastUpdate =
                        pkColumns.stream()
                                .allMatch(
                                        col ->
                                                resolvePrimaryLevel(entries, userId, groupIds, ResourceLevel.COLUMN, tableName, col)
                                                        .compareTo(PrimaryLevel.UPDATE)
                                                        >= 0);
                var allAtLeastRead =
                        pkColumns.stream()
                                .allMatch(
                                        col ->
                                                resolvePrimaryLevel(entries, userId, groupIds, ResourceLevel.COLUMN, tableName, col)
                                                        .compareTo(PrimaryLevel.READ)
                                                        >= 0);
                canCreate = auxCreate && allAtLeastUpdate;
                canDelete = auxDelete && allAtLeastRead;
            }
        }

        var result = new EffectivePermission(primaryLevel, canCreate, canDelete);
        cacheService.put(cacheKey, result);
        return result;
    }

    private record ChainStep(ResourceLevel level, String tableName, String columnName) {
    }

    private List<ChainStep> buildChain(ResourceLevel level, String tableName, String columnName) {
        return switch (level) {
            case COLUMN ->
                    List.of(
                            new ChainStep(ResourceLevel.COLUMN, tableName, columnName),
                            new ChainStep(ResourceLevel.TABLE, tableName, null),
                            new ChainStep(ResourceLevel.SCHEMA, null, null));
            case TABLE ->
                    List.of(
                            new ChainStep(ResourceLevel.TABLE, tableName, null),
                            new ChainStep(ResourceLevel.SCHEMA, null, null));
            case SCHEMA -> List.of(new ChainStep(ResourceLevel.SCHEMA, null, null));
        };
    }

    private PermissionEntry findMatch(
            List<PermissionEntry> entries, SubjectType subjectType, Long subjectId, ChainStep step) {
        return entries.stream()
                .filter(
                        e ->
                                e.getSubjectType() == subjectType
                                        && e.getSubjectId().equals(subjectId)
                                        && e.getResourceLevel() == step.level()
                                        && Objects.equals(e.getTableName(), step.tableName())
                                        && Objects.equals(e.getColumnName(), step.columnName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * business-rules.md BR-9: the user's own hierarchy fallback (COLUMN→
     * TABLE→SCHEMA) is resolved first and, if present, wins outright over
     * any group composition.
     */
    private PrimaryLevel resolvePrimaryLevel(
            List<PermissionEntry> entries,
            Long userId,
            List<Long> groupIds,
            ResourceLevel level,
            String tableName,
            String columnName) {
        var chain = buildChain(level, tableName, columnName);

        var userOwn =
                chain.stream()
                        .map(step -> findMatch(entries, SubjectType.USER, userId, step))
                        .filter(Objects::nonNull)
                        .map(PermissionEntry::getPrimaryLevel)
                        .filter(Objects::nonNull)
                        .findFirst();
        if (userOwn.isPresent()) {
            return userOwn.get();
        }

        PrimaryLevel groupComposed = null;
        for (var groupId : groupIds) {
            var groupLevel =
                    chain.stream()
                            .map(step -> findMatch(entries, SubjectType.GROUP, groupId, step))
                            .filter(Objects::nonNull)
                            .map(PermissionEntry::getPrimaryLevel)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
            if (groupLevel != null && (groupComposed == null || groupLevel.compareTo(groupComposed) > 0)) {
                groupComposed = groupLevel;
            }
        }
        return groupComposed != null ? groupComposed : PrimaryLevel.NONE;
    }

    /** business-rules.md BR-11: same user-priority/group-OR principle, TABLE→SCHEMA fallback only. */
    private boolean resolveAux(
            List<PermissionEntry> entries, Long userId, List<Long> groupIds, String tableName, boolean isCreate) {
        var chain =
                List.of(new ChainStep(ResourceLevel.TABLE, tableName, null), new ChainStep(ResourceLevel.SCHEMA, null, null));

        var userOwn =
                chain.stream()
                        .map(step -> findMatch(entries, SubjectType.USER, userId, step))
                        .filter(Objects::nonNull)
                        .map(e -> isCreate ? e.getAuxCreate() : e.getAuxDelete())
                        .filter(Objects::nonNull)
                        .findFirst();
        if (userOwn.isPresent()) {
            return userOwn.get();
        }

        var anyGroupTrue = false;
        for (var groupId : groupIds) {
            var groupValue =
                    chain.stream()
                            .map(step -> findMatch(entries, SubjectType.GROUP, groupId, step))
                            .filter(Objects::nonNull)
                            .map(e -> isCreate ? e.getAuxCreate() : e.getAuxDelete())
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
            if (Boolean.TRUE.equals(groupValue)) {
                anyGroupTrue = true;
            }
        }
        return anyGroupTrue;
    }

    private List<String> findPrimaryKeyColumns(Long connectionId, String schemaName, String tableName) {
        return schemaRepository
                .findByConnectionIdAndSchemaName(connectionId, schemaName)
                .flatMap(schema -> tableRepository.findBySchemaIdAndTableName(schema.getId(), tableName))
                .map(
                        table ->
                                columnRepository.findAllByTableId(table.getId()).stream()
                                        .filter(DbColumn::isPrimaryKey)
                                        .map(DbColumn::getColumnName)
                                        .toList())
                .orElse(List.of());
    }

    // --- YAML export/import (US-2.5, US-2.6) ---

    @Override
    @Transactional(readOnly = true)
    public String exportPermissions(Long connectionId, Long actorUserId) {
        var entries = permissionRepository.findAllByConnectionId(connectionId);
        var yamlEntries = entries.stream().map(this::toYamlEntry).toList();
        auditLogService.recordEvent(
                AuditEventType.PERMISSIONS_EXPORTED,
                actorUserId,
                null,
                Map.of("connectionId", connectionId, "entryCount", yamlEntries.size()));
        return yamlMapper.write(new PermissionYamlDocument(yamlEntries));
    }

    private PermissionYamlEntry toYamlEntry(PermissionEntry entry) {
        var subject =
                entry.getSubjectType() == SubjectType.USER
                        ? userRepository
                                .findById(entry.getSubjectId())
                                .map(User::getEmail)
                                .orElseThrow(UserAccountException::userNotFound)
                        : groupRepository
                                .findById(entry.getSubjectId())
                                .map(UserGroup::getName)
                                .orElseThrow(AccessControlException::groupNotFound);
        return new PermissionYamlEntry(
                entry.getSubjectType(),
                subject,
                entry.getResourceLevel(),
                entry.getSchemaName(),
                entry.getTableName(),
                entry.getColumnName(),
                entry.getPrimaryLevel(),
                entry.getAuxCreate(),
                entry.getAuxDelete());
    }

    @Override
    @Transactional
    public ImportPermissionsResult importPermissions(Long connectionId, String yamlContent, Long actorUserId) {
        var document = yamlMapper.read(yamlContent);
        var yamlEntries = document.entries() != null ? document.entries() : List.<PermissionYamlEntry>of();

        var seenKeys = new HashSet<String>();
        var resolvedEntries = new ArrayList<PermissionEntry>();
        for (var yamlEntry : yamlEntries) {
            validateResourcePath(
                    yamlEntry.resourceLevel(), yamlEntry.schemaName(), yamlEntry.tableName(), yamlEntry.columnName());
            if (yamlEntry.resourceLevel() == ResourceLevel.COLUMN
                    && (yamlEntry.auxCreate() != null || yamlEntry.auxDelete() != null)) {
                throw AccessControlException.auxiliaryNotApplicable();
            }

            Long subjectId =
                    yamlEntry.subjectType() == SubjectType.USER
                            ? userRepository
                                    .findByEmail(yamlEntry.subject())
                                    .map(User::getId)
                                    .orElseThrow(AccessControlException::subjectNotResolved)
                            : groupRepository
                                    .findByName(yamlEntry.subject())
                                    .map(UserGroup::getId)
                                    .orElseThrow(AccessControlException::subjectNotResolved);

            var key =
                    yamlEntry.subjectType()
                            + ":"
                            + subjectId
                            + ":"
                            + yamlEntry.resourceLevel()
                            + ":"
                            + yamlEntry.schemaName()
                            + ":"
                            + yamlEntry.tableName()
                            + ":"
                            + yamlEntry.columnName();
            if (!seenKeys.add(key)) {
                throw AccessControlException.duplicateEntry();
            }

            var entry =
                    new PermissionEntry(
                            yamlEntry.subjectType(),
                            subjectId,
                            connectionId,
                            yamlEntry.resourceLevel(),
                            yamlEntry.schemaName(),
                            yamlEntry.tableName(),
                            yamlEntry.columnName());
            entry.setPrimaryLevel(yamlEntry.primaryLevel());
            entry.setAuxiliary(yamlEntry.auxCreate(), yamlEntry.auxDelete());
            resolvedEntries.add(entry);
        }

        // business-rules.md BR-19: full replace within this single transaction.
        permissionRepository.deleteAllByConnectionId(connectionId);
        permissionRepository.saveAll(resolvedEntries);
        cacheService.invalidateByConnection(connectionId);
        auditLogService.recordEvent(
                AuditEventType.PERMISSIONS_IMPORTED,
                actorUserId,
                null,
                Map.of("connectionId", connectionId, "importedCount", resolvedEntries.size()));
        return new ImportPermissionsResult(resolvedEntries.size());
    }

    // --- shared helpers ---

    private UserGroup findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(AccessControlException::groupNotFound);
    }

    private PermissionEntry findOrCreateEntry(
            SubjectType subjectType,
            Long subjectId,
            Long connectionId,
            ResourceLevel resourceLevel,
            String schemaName,
            String tableName,
            String columnName) {
        return permissionRepository
                .findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
                        subjectType, subjectId, connectionId, resourceLevel, schemaName, tableName, columnName)
                .orElseGet(
                        () ->
                                new PermissionEntry(
                                        subjectType, subjectId, connectionId, resourceLevel, schemaName, tableName, columnName));
    }

    private void invalidateForSubject(SubjectType subjectType, Long subjectId) {
        if (subjectType == SubjectType.USER) {
            cacheService.invalidateByUser(subjectId);
        } else {
            cacheService.invalidateByGroup(subjectId);
        }
    }

    private void recordPermissionChanged(SubjectType subjectType, Long subjectId, Long connectionId, Long actorUserId) {
        auditLogService.recordEvent(
                AuditEventType.PERMISSION_CHANGED,
                actorUserId,
                null,
                Map.of(
                        "subjectType", subjectType,
                        "subjectId", subjectId,
                        "connectionId", connectionId));
    }

    private void validateResourcePath(ResourceLevel level, String schemaName, String tableName, String columnName) {
        validateIdentifier(schemaName);
        if (level == ResourceLevel.SCHEMA) {
            if (tableName != null || columnName != null) {
                throw AccessControlException.invalidIdentifier();
            }
            return;
        }
        validateIdentifier(tableName);
        if (level == ResourceLevel.TABLE) {
            if (columnName != null) {
                throw AccessControlException.invalidIdentifier();
            }
            return;
        }
        validateIdentifier(columnName);
    }

    private void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw AccessControlException.invalidIdentifier();
        }
    }
}
