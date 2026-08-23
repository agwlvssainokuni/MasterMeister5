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

package cherry.mastermeister5.mastermaintenance.service;

import cherry.mastermeister5.accesscontrol.cache.EffectivePermission;
import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.connectionschema.service.ConnectionPoolRegistry;
import cherry.mastermeister5.connectionschema.service.SchemaImportedEvent;
import cherry.mastermeister5.mastermaintenance.entity.ColumnCustomization;
import cherry.mastermeister5.mastermaintenance.entity.InputWidget;
import cherry.mastermeister5.mastermaintenance.entity.TableCustomization;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRule;
import cherry.mastermeister5.mastermaintenance.repository.ColumnCustomizationJpaRepository;
import cherry.mastermeister5.mastermaintenance.repository.TableCustomizationJpaRepository;
import cherry.mastermeister5.mastermaintenance.repository.ValidationRuleJpaRepository;
import cherry.mastermeister5.platform.BulkAccessProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * business-logic-model.md / business-rules.md (BR-1〜BR-18).
 */
@Service
class MasterMaintenanceServiceImpl implements MasterMaintenanceService {

    /** nfr-design-patterns.md (Unit 5) Question 3: same allowlist as Unit 3's validateIdentifier. */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    /** nfr-design-patterns.md (Unit 5) Question 3: blocklist for hand-entered WHERE/ORDER BY. */
    private static final Pattern UNSAFE_RAW_CLAUSE_PATTERN = Pattern.compile(";|--|/\\*");

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final TableCustomizationJpaRepository tableCustomizationRepository;
    private final ColumnCustomizationJpaRepository columnCustomizationRepository;
    private final ValidationRuleJpaRepository validationRuleRepository;
    private final DbSchemaJpaRepository schemaRepository;
    private final DbTableJpaRepository tableRepository;
    private final DbColumnJpaRepository columnRepository;
    private final TargetConnectionJpaRepository connectionRepository;
    private final ConnectionPoolRegistry poolRegistry;
    private final AccessControlService accessControlService;
    private final CustomizationYamlMapper yamlMapper;
    private final AuditLogService auditLogService;
    private final BulkAccessProperties bulkAccessProperties;

    MasterMaintenanceServiceImpl(
            TableCustomizationJpaRepository tableCustomizationRepository,
            ColumnCustomizationJpaRepository columnCustomizationRepository,
            ValidationRuleJpaRepository validationRuleRepository,
            DbSchemaJpaRepository schemaRepository,
            DbTableJpaRepository tableRepository,
            DbColumnJpaRepository columnRepository,
            TargetConnectionJpaRepository connectionRepository,
            ConnectionPoolRegistry poolRegistry,
            AccessControlService accessControlService,
            CustomizationYamlMapper yamlMapper,
            AuditLogService auditLogService,
            BulkAccessProperties bulkAccessProperties) {
        this.tableCustomizationRepository = tableCustomizationRepository;
        this.columnCustomizationRepository = columnCustomizationRepository;
        this.validationRuleRepository = validationRuleRepository;
        this.schemaRepository = schemaRepository;
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
        this.connectionRepository = connectionRepository;
        this.poolRegistry = poolRegistry;
        this.accessControlService = accessControlService;
        this.yamlMapper = yamlMapper;
        this.auditLogService = auditLogService;
        this.bulkAccessProperties = bulkAccessProperties;
    }

    // --- table listing (US-3.1) ---

    @Override
    @Transactional(readOnly = true)
    public List<TableSummary> listTables(Long connectionId, Long userId) {
        var result = new ArrayList<TableSummary>();
        for (var schema : schemaRepository.findAllByConnectionId(connectionId)) {
            for (var table : tableRepository.findAllBySchemaId(schema.getId())) {
                var columnNames =
                        columnRepository.findAllByTableId(table.getId()).stream().map(DbColumn::getColumnName).toList();
                var permissions =
                        accessControlService.resolveEffectivePermissionsForTable(
                                userId, connectionId, schema.getSchemaName(), table.getTableName(), columnNames);
                var anyReadable = permissions.values().stream().anyMatch(p -> p.primaryLevel() != PrimaryLevel.NONE);
                if (anyReadable) {
                    result.add(
                            new TableSummary(
                                    schema.getSchemaName(), table.getTableName(), table.getTableType(), table.getComment()));
                }
            }
        }
        return result;
    }

    // --- record listing (US-3.1〜US-3.3) ---

    @Override
    @Transactional(readOnly = true)
    public RecordPage listRecords(ListRecordsCommand command) {
        var schema = findSchemaOrThrow(command.connectionId(), command.schemaName());
        var table = findTableOrThrow(schema.getId(), command.tableName());
        var dbColumns = columnRepository.findAllByTableId(table.getId());
        var columnNames = dbColumns.stream().map(DbColumn::getColumnName).toList();

        var permissions =
                accessControlService.resolveEffectivePermissionsForTable(
                        command.userId(), command.connectionId(), command.schemaName(), command.tableName(), columnNames);

        var tableCustomization =
                tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(
                        command.connectionId(), command.schemaName(), command.tableName());

        var resolved =
                resolveVisibleColumns(
                        command.connectionId(), command.schemaName(), command.tableName(), dbColumns, permissions);
        var selectColumnNames = resolved.selectColumnNames();
        var visibleColumnNames = resolved.visibleColumns().stream().map(ColumnDef::columnName).collect(Collectors.toSet());

        var paramSource = new MapSqlParameterSource();
        String whereClause = null;
        if (command.filterCriteria() != null) {
            if (isBlank(command.filterCriteria().rawWhereClause())) {
                if (command.filterCriteria().conditions() != null && !command.filterCriteria().conditions().isEmpty()) {
                    var parts = new ArrayList<String>();
                    var index = 0;
                    for (var condition : command.filterCriteria().conditions()) {
                        if (!visibleColumnNames.contains(condition.columnName())) {
                            throw MasterMaintenanceException.permissionDenied();
                        }
                        parts.add(renderCondition(condition, "f" + index++, paramSource));
                    }
                    whereClause = String.join(" AND ", parts);
                }
            } else {
                validateRawClause(command.filterCriteria().rawWhereClause());
                whereClause = command.filterCriteria().rawWhereClause();
            }
        }

        String orderByClause = null;
        var sort = command.sortCriteria();
        if (sort != null && !isBlank(sort.rawOrderByClause())) {
            validateRawClause(sort.rawOrderByClause());
            orderByClause = sort.rawOrderByClause();
        } else if (sort != null && sort.columnName() != null) {
            if (!visibleColumnNames.contains(sort.columnName())) {
                throw MasterMaintenanceException.permissionDenied();
            }
            orderByClause = sort.columnName() + " " + sort.direction();
        } else if (tableCustomization.isPresent() && tableCustomization.get().getDefaultSortColumn() != null) {
            var tc = tableCustomization.get();
            orderByClause = tc.getDefaultSortColumn() + " " + (tc.getDefaultSortDirection() != null ? tc.getDefaultSortDirection() : "ASC");
        }

        var qualifiedTable = command.schemaName() + "." + command.tableName();
        var selectSql = new StringBuilder("SELECT ").append(String.join(", ", selectColumnNames)).append(" FROM ").append(qualifiedTable);
        var countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(qualifiedTable);
        if (whereClause != null) {
            selectSql.append(" WHERE ").append(whereClause);
            countSql.append(" WHERE ").append(whereClause);
        }
        if (orderByClause != null) {
            selectSql.append(" ORDER BY ").append(orderByClause);
        }
        // nfr-design-patterns.md (Unit 5) Question 5: OFFSET/LIMIT paging, default 50 rows.
        paramSource.addValue("pageSize", command.pageSize());
        paramSource.addValue("offset", (long) command.page() * command.pageSize());
        selectSql.append(" LIMIT :pageSize OFFSET :offset");

        var connection = findConnectionOrThrow(command.connectionId());
        var jdbcTemplate = new NamedParameterJdbcTemplate(poolRegistry.dataSourceFor(connection));
        var totalCount = jdbcTemplate.queryForObject(countSql.toString(), paramSource, Long.class);
        var rows =
                jdbcTemplate.query(
                        selectSql.toString(),
                        paramSource,
                        (rs, rowNum) -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (var columnName : selectColumnNames) {
                                row.put(columnName, rs.getObject(columnName));
                            }
                            return row;
                        });

        var effectiveTotalCount = totalCount == null ? 0 : totalCount;
        // tech-stack-decisions.md (Unit 6) Question 3 / business-rules.md BR-15:
        // "bulk data access" is measured against the total matching row count,
        // not just the page returned, since that is what reflects the scope of
        // data the caller has queried access to.
        if (effectiveTotalCount >= bulkAccessProperties.threshold()) {
            auditLogService.recordEvent(
                    AuditEventType.BULK_DATA_ACCESSED,
                    command.userId(),
                    null,
                    Map.of(
                            "source", "masterData",
                            "connectionId", command.connectionId(),
                            "schemaName", command.schemaName(),
                            "tableName", command.tableName(),
                            "totalCount", effectiveTotalCount));
        }

        return new RecordPage(rows, command.page(), command.pageSize(), effectiveTotalCount);
    }

    @Override
    @Transactional(readOnly = true)
    public TableMetadata resolveTableMetadata(Long connectionId, String schemaName, String tableName, Long userId) {
        var schema = findSchemaOrThrow(connectionId, schemaName);
        var table = findTableOrThrow(schema.getId(), tableName);
        var dbColumns = columnRepository.findAllByTableId(table.getId());
        var columnNames = dbColumns.stream().map(DbColumn::getColumnName).toList();
        var permissions =
                accessControlService.resolveEffectivePermissionsForTable(
                        userId, connectionId, schemaName, tableName, columnNames);
        var visibleColumns =
                resolveVisibleColumns(connectionId, schemaName, tableName, dbColumns, permissions).visibleColumns();
        // BR-1: primary key columns must stay identifiable for update/delete
        // even when the caller has no READ permission on them (so they're
        // excluded from visibleColumns above) — column *names* aren't
        // sensitive, only their values, which this list never carries.
        var primaryKeyColumns = dbColumns.stream().filter(DbColumn::isPrimaryKey).map(DbColumn::getColumnName).toList();
        var tablePermission =
                accessControlService.resolveEffectivePermission(
                        userId, connectionId, ResourceLevel.TABLE, schemaName, tableName, null);
        return new TableMetadata(
                visibleColumns, primaryKeyColumns, tablePermission.canCreate(), tablePermission.canDelete());
    }

    private record VisibleColumnsResult(List<ColumnDef> visibleColumns, List<String> selectColumnNames) {
    }

    /**
     * Shared by {@link #listRecords} (which also needs {@code
     * selectColumnNames} to build its SQL) and {@link #resolveTableMetadata}
     * (which only needs the display-facing {@code visibleColumns}).
     */
    private VisibleColumnsResult resolveVisibleColumns(
            Long connectionId,
            String schemaName,
            String tableName,
            List<DbColumn> dbColumns,
            Map<String, EffectivePermission> permissions) {
        var tableCustomization =
                tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(
                        connectionId, schemaName, tableName);
        var customizationByColumn =
                tableCustomization
                        .map(tc -> columnCustomizationRepository.findAllByTableCustomizationId(tc.getId()))
                        .orElse(List.<ColumnCustomization>of())
                        .stream()
                        .collect(Collectors.toMap(ColumnCustomization::getColumnName, c -> c));

        var selectColumnNames = new ArrayList<String>();
        var visibleColumns = new ArrayList<ColumnDef>();
        var displayOrderByColumn = new HashMap<String, Integer>();
        for (var dbColumn : dbColumns) {
            var level = permissions.get(dbColumn.getColumnName()).primaryLevel();
            // BR-1: primary key columns are always fetched (never displayed if
            // unauthorized) so a row remains identifiable for update/delete.
            if (level == PrimaryLevel.NONE && !dbColumn.isPrimaryKey()) {
                continue;
            }
            selectColumnNames.add(dbColumn.getColumnName());
            if (level == PrimaryLevel.NONE) {
                continue;
            }
            var customization = customizationByColumn.get(dbColumn.getColumnName());
            if (customization != null && customization.isHidden()) {
                continue;
            }
            var displayLabel =
                    customization != null && customization.getDisplayLabel() != null
                            ? customization.getDisplayLabel()
                            : dbColumn.getColumnName();
            var readOnly = level != PrimaryLevel.UPDATE || (customization != null && customization.isReadOnly());
            var widget =
                    customization != null && customization.getInputWidget() != null
                            ? customization.getInputWidget()
                            : defaultWidgetFor(dbColumn.getDataType());
            visibleColumns.add(
                    new ColumnDef(
                            dbColumn.getColumnName(),
                            displayLabel,
                            dbColumn.getDataType(),
                            dbColumn.isPrimaryKey(),
                            level,
                            readOnly,
                            widget,
                            customization != null ? customization.getSelectOptionsJson() : null));
            if (customization != null && customization.getDisplayOrder() != null) {
                displayOrderByColumn.put(dbColumn.getColumnName(), customization.getDisplayOrder());
            }
        }
        visibleColumns.sort(
                Comparator.comparing(c -> displayOrderByColumn.getOrDefault(c.columnName(), Integer.MAX_VALUE)));
        return new VisibleColumnsResult(visibleColumns, selectColumnNames);
    }

    private String renderCondition(FilterCondition condition, String paramName, MapSqlParameterSource paramSource) {
        return switch (condition.operator()) {
            case EQ -> bind(condition, paramName, paramSource, " = :");
            case NE -> bind(condition, paramName, paramSource, " <> :");
            case LT -> bind(condition, paramName, paramSource, " < :");
            case LE -> bind(condition, paramName, paramSource, " <= :");
            case GT -> bind(condition, paramName, paramSource, " > :");
            case GE -> bind(condition, paramName, paramSource, " >= :");
            case LIKE -> {
                paramSource.addValue(paramName, "%" + condition.value() + "%");
                yield condition.columnName() + " LIKE :" + paramName;
            }
            case IS_NULL -> condition.columnName() + " IS NULL";
            case IS_NOT_NULL -> condition.columnName() + " IS NOT NULL";
        };
    }

    private String bind(FilterCondition condition, String paramName, MapSqlParameterSource paramSource, String operatorSql) {
        paramSource.addValue(paramName, condition.value());
        return condition.columnName() + operatorSql + paramName;
    }

    private InputWidget defaultWidgetFor(String dataType) {
        var upper = dataType.toUpperCase(Locale.ROOT);
        if (upper.contains("BOOL") || upper.contains("BIT")) {
            return InputWidget.CHECKBOX;
        }
        if (upper.contains("DATE") || upper.contains("TIME")) {
            return InputWidget.DATE;
        }
        return InputWidget.TEXT;
    }

    // --- apply changes (US-3.4〜US-3.6) ---

    @Override
    @Transactional
    public ApplyResult applyChanges(Long connectionId, String schemaName, String tableName, Long userId, RecordChangeSet changeSet) {
        var schema = findSchemaOrThrow(connectionId, schemaName);
        var table = findTableOrThrow(schema.getId(), tableName);
        var dbColumns = columnRepository.findAllByTableId(table.getId());
        var columnByName = dbColumns.stream().collect(Collectors.toMap(DbColumn::getColumnName, c -> c));
        var hasPrimaryKey = dbColumns.stream().anyMatch(DbColumn::isPrimaryKey);
        var columnNames = dbColumns.stream().map(DbColumn::getColumnName).toList();

        var permissions =
                accessControlService.resolveEffectivePermissionsForTable(userId, connectionId, schemaName, tableName, columnNames);
        var needsTableLevel = changeSet.changes().stream().anyMatch(c -> c.operation() != ChangeOperation.UPDATE);
        EffectivePermission tablePermission =
                needsTableLevel
                        ? accessControlService.resolveEffectivePermission(
                                userId, connectionId, ResourceLevel.TABLE, schemaName, tableName, null)
                        : null;

        var hasUpdateOrDelete =
                changeSet.changes().stream().anyMatch(c -> c.operation() != ChangeOperation.CREATE);
        if (hasUpdateOrDelete && !hasPrimaryKey) {
            throw MasterMaintenanceException.noPrimaryKey();
        }

        var validationRulesByColumn = loadValidationRules(connectionId, schemaName, tableName);

        // --- validation phase: nothing is written to the target RDBMS until every change passes. ---
        for (var change : changeSet.changes()) {
            switch (change.operation()) {
                case CREATE -> {
                    if (tablePermission == null || !tablePermission.canCreate()) {
                        throw MasterMaintenanceException.permissionDenied();
                    }
                    validateColumnValues(change.columnValues(), permissions, columnByName, validationRulesByColumn);
                }
                case UPDATE -> {
                    if (isEmpty(change.primaryKeyValues())) {
                        throw MasterMaintenanceException.noPrimaryKey();
                    }
                    validateColumnValues(change.columnValues(), permissions, columnByName, validationRulesByColumn);
                }
                case DELETE -> {
                    if (tablePermission == null || !tablePermission.canDelete()) {
                        throw MasterMaintenanceException.permissionDenied();
                    }
                    if (isEmpty(change.primaryKeyValues())) {
                        throw MasterMaintenanceException.noPrimaryKey();
                    }
                }
            }
        }

        // --- execution phase: one JDBC transaction against the target RDBMS. ---
        var connection = findConnectionOrThrow(connectionId);
        var qualifiedTable = schemaName + "." + tableName;
        var createdCount = 0;
        var updatedCount = 0;
        var deletedCount = 0;
        try (var rawConnection = poolRegistry.dataSourceFor(connection).getConnection()) {
            var originalAutoCommit = rawConnection.getAutoCommit();
            rawConnection.setAutoCommit(false);
            // Not try-with-resources: SingleConnectionDataSource#close() closes the
            // wrapped connection outright regardless of suppressClose (that flag only
            // no-ops close() on proxies handed out via getConnection()). The outer
            // try-with-resources above is what must close `rawConnection`, exactly once.
            var singleConnectionDataSource = new SingleConnectionDataSource(rawConnection, true);
            var jdbcTemplate = new NamedParameterJdbcTemplate(singleConnectionDataSource);
            try {
                for (var change : changeSet.changes()) {
                    switch (change.operation()) {
                        case CREATE -> executeInsert(jdbcTemplate, qualifiedTable, change.columnValues());
                        case UPDATE -> executeUpdate(jdbcTemplate, qualifiedTable, change.columnValues(), change.primaryKeyValues());
                        case DELETE -> executeDelete(jdbcTemplate, qualifiedTable, change.primaryKeyValues());
                    }
                    switch (change.operation()) {
                        case CREATE -> createdCount++;
                        case UPDATE -> updatedCount++;
                        case DELETE -> deletedCount++;
                    }
                }
                rawConnection.commit();
            } catch (RuntimeException e) {
                rawConnection.rollback();
                throw e;
            } finally {
                rawConnection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }

        auditLogService.recordEvent(
                AuditEventType.RECORD_CHANGES_APPLIED,
                userId,
                null,
                Map.of(
                        "connectionId", connectionId,
                        "schemaName", schemaName,
                        "tableName", tableName,
                        "createdCount", createdCount,
                        "updatedCount", updatedCount,
                        "deletedCount", deletedCount));

        return new ApplyResult(createdCount, updatedCount, deletedCount);
    }

    private void validateColumnValues(
            Map<String, Object> columnValues,
            Map<String, EffectivePermission> permissions,
            Map<String, DbColumn> columnByName,
            Map<String, List<ValidationRule>> validationRulesByColumn) {
        if (columnValues == null) {
            return;
        }
        for (var entry : columnValues.entrySet()) {
            var columnName = entry.getKey();
            if (!columnByName.containsKey(columnName)) {
                throw MasterMaintenanceException.invalidIdentifier();
            }
            var permission = permissions.get(columnName);
            if (permission == null || permission.primaryLevel() != PrimaryLevel.UPDATE) {
                throw MasterMaintenanceException.permissionDenied();
            }
            for (var rule : validationRulesByColumn.getOrDefault(columnName, List.of())) {
                if (!ruleMatches(rule, entry.getValue())) {
                    throw MasterMaintenanceException.validationFailed();
                }
            }
        }
    }

    private boolean ruleMatches(ValidationRule rule, Object value) {
        if (value == null) {
            return true;
        }
        var stringValue = String.valueOf(value);
        return switch (rule.getType()) {
            case REGEX -> rule.getPattern() == null || Pattern.matches(rule.getPattern(), stringValue);
            case RANGE -> rangeMatches(rule, stringValue);
        };
    }

    private boolean rangeMatches(ValidationRule rule, String stringValue) {
        try {
            var numeric = new BigDecimal(stringValue);
            if (rule.getMinValue() != null && numeric.compareTo(new BigDecimal(rule.getMinValue())) < 0) {
                return false;
            }
            return rule.getMaxValue() == null || numeric.compareTo(new BigDecimal(rule.getMaxValue())) <= 0;
        } catch (NumberFormatException e) {
            if (rule.getMinValue() != null && stringValue.compareTo(rule.getMinValue()) < 0) {
                return false;
            }
            return rule.getMaxValue() == null || stringValue.compareTo(rule.getMaxValue()) <= 0;
        }
    }

    private void executeInsert(NamedParameterJdbcTemplate jdbcTemplate, String qualifiedTable, Map<String, Object> columnValues) {
        var columns = String.join(", ", columnValues.keySet());
        var placeholders = columnValues.keySet().stream().map(c -> ":" + c).collect(Collectors.joining(", "));
        var sql = "INSERT INTO " + qualifiedTable + " (" + columns + ") VALUES (" + placeholders + ")";
        jdbcTemplate.update(sql, new MapSqlParameterSource(columnValues));
    }

    private void executeUpdate(
            NamedParameterJdbcTemplate jdbcTemplate,
            String qualifiedTable,
            Map<String, Object> columnValues,
            Map<String, Object> primaryKeyValues) {
        var params = new MapSqlParameterSource();
        var setClause =
                columnValues.entrySet().stream()
                        .map(
                                e -> {
                                    params.addValue(e.getKey(), e.getValue());
                                    return e.getKey() + " = :" + e.getKey();
                                })
                        .collect(Collectors.joining(", "));
        var whereClause = buildPrimaryKeyWhereClause(primaryKeyValues, params);
        var sql = "UPDATE " + qualifiedTable + " SET " + setClause + " WHERE " + whereClause;
        jdbcTemplate.update(sql, params);
    }

    private void executeDelete(NamedParameterJdbcTemplate jdbcTemplate, String qualifiedTable, Map<String, Object> primaryKeyValues) {
        var params = new MapSqlParameterSource();
        var whereClause = buildPrimaryKeyWhereClause(primaryKeyValues, params);
        var sql = "DELETE FROM " + qualifiedTable + " WHERE " + whereClause;
        jdbcTemplate.update(sql, params);
    }

    private String buildPrimaryKeyWhereClause(Map<String, Object> primaryKeyValues, MapSqlParameterSource params) {
        return primaryKeyValues.entrySet().stream()
                .map(
                        e -> {
                            var paramName = "pk_" + e.getKey();
                            params.addValue(paramName, e.getValue());
                            return e.getKey() + " = :" + paramName;
                        })
                .collect(Collectors.joining(" AND "));
    }

    private Map<String, List<ValidationRule>> loadValidationRules(Long connectionId, String schemaName, String tableName) {
        var tableCustomization =
                tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(connectionId, schemaName, tableName);
        if (tableCustomization.isEmpty()) {
            return Map.of();
        }
        var columnCustomizations = columnCustomizationRepository.findAllByTableCustomizationId(tableCustomization.get().getId());
        var columnCustomizationIds = columnCustomizations.stream().map(ColumnCustomization::getId).toList();
        var rulesByCustomizationId =
                validationRuleRepository.findAllByColumnCustomizationIdIn(columnCustomizationIds).stream()
                        .collect(Collectors.groupingBy(ValidationRule::getColumnCustomizationId));
        var result = new HashMap<String, List<ValidationRule>>();
        for (var cc : columnCustomizations) {
            result.put(cc.getColumnName(), rulesByCustomizationId.getOrDefault(cc.getId(), List.of()));
        }
        return result;
    }

    // --- customization definitions (US-3.7) ---

    @Override
    @Transactional(readOnly = true)
    public CustomizationYamlEntry getCustomizationDefinition(Long connectionId, String schemaName, String tableName) {
        return tableCustomizationRepository
                .findByConnectionIdAndSchemaNameAndTableName(connectionId, schemaName, tableName)
                .map(this::toYamlEntry)
                .orElse(new CustomizationYamlEntry(schemaName, tableName, null, null, List.of()));
    }

    private CustomizationYamlEntry toYamlEntry(TableCustomization tableCustomization) {
        var columnCustomizations = columnCustomizationRepository.findAllByTableCustomizationId(tableCustomization.getId());
        var columnCustomizationIds = columnCustomizations.stream().map(ColumnCustomization::getId).toList();
        var rulesByCustomizationId =
                validationRuleRepository.findAllByColumnCustomizationIdIn(columnCustomizationIds).stream()
                        .collect(Collectors.groupingBy(ValidationRule::getColumnCustomizationId));
        var yamlColumns =
                columnCustomizations.stream()
                        .map(
                                cc ->
                                        new CustomizationYamlColumn(
                                                cc.getColumnName(),
                                                cc.getDisplayLabel(),
                                                cc.getDisplayOrder(),
                                                cc.isHidden(),
                                                cc.isReadOnly(),
                                                cc.getInputWidget(),
                                                parseSelectOptions(cc.getSelectOptionsJson()),
                                                rulesByCustomizationId.getOrDefault(cc.getId(), List.of()).stream()
                                                        .map(
                                                                r ->
                                                                        new CustomizationYamlValidationRule(
                                                                                r.getType(), r.getPattern(), r.getMinValue(), r.getMaxValue()))
                                                        .toList()))
                        .toList();
        return new CustomizationYamlEntry(
                tableCustomization.getSchemaName(),
                tableCustomization.getTableName(),
                tableCustomization.getDefaultSortColumn(),
                tableCustomization.getDefaultSortDirection(),
                yamlColumns);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCustomizationDefinition(Long connectionId, Long actorUserId) {
        var entries = tableCustomizationRepository.findAllByConnectionId(connectionId).stream().map(this::toYamlEntry).toList();
        auditLogService.recordEvent(
                AuditEventType.CUSTOMIZATIONS_EXPORTED,
                actorUserId,
                null,
                Map.of("connectionId", connectionId, "tableCount", entries.size()));
        return yamlMapper.write(new CustomizationYamlDocument(entries));
    }

    @Override
    @Transactional
    public ImportCustomizationResult importCustomizationDefinition(Long connectionId, String yamlContent, Long actorUserId) {
        var document = yamlMapper.read(yamlContent);
        var tables = document.tables() != null ? document.tables() : List.<CustomizationYamlEntry>of();

        for (var entry : tables) {
            validateIdentifier(entry.schemaName());
            validateIdentifier(entry.tableName());
            for (var column : entry.columns()) {
                validateIdentifier(column.columnName());
            }
        }

        // business-rules.md BR-16: full replace within a single transaction.
        var existingIds = tableCustomizationRepository.findAllByConnectionId(connectionId).stream().map(TableCustomization::getId).toList();
        if (!existingIds.isEmpty()) {
            var columnIds =
                    columnCustomizationRepository.findAllByTableCustomizationIdIn(existingIds).stream()
                            .map(ColumnCustomization::getId)
                            .toList();
            if (!columnIds.isEmpty()) {
                validationRuleRepository.deleteAllByColumnCustomizationIdIn(columnIds);
            }
            columnCustomizationRepository.deleteAllByTableCustomizationIdIn(existingIds);
        }
        tableCustomizationRepository.deleteAllByConnectionId(connectionId);

        for (var entry : tables) {
            var tableCustomization = new TableCustomization(connectionId, entry.schemaName(), entry.tableName());
            tableCustomization.setDefaultSort(entry.defaultSortColumn(), entry.defaultSortDirection());
            tableCustomization = tableCustomizationRepository.save(tableCustomization);
            for (var column : entry.columns()) {
                var columnCustomization = new ColumnCustomization(tableCustomization.getId(), column.columnName());
                columnCustomization.applySettings(
                        column.displayLabel(),
                        column.displayOrder(),
                        column.hidden(),
                        column.readOnly(),
                        column.inputWidget(),
                        writeSelectOptions(column.selectOptions()));
                columnCustomization = columnCustomizationRepository.save(columnCustomization);
                var rules = column.validationRules() != null ? column.validationRules() : List.<CustomizationYamlValidationRule>of();
                for (var rule : rules) {
                    validationRuleRepository.save(
                            new ValidationRule(columnCustomization.getId(), rule.type(), rule.pattern(), rule.minValue(), rule.maxValue()));
                }
            }
        }

        auditLogService.recordEvent(
                AuditEventType.CUSTOMIZATIONS_IMPORTED,
                actorUserId,
                null,
                Map.of("connectionId", connectionId, "tableCount", tables.size()));
        return new ImportCustomizationResult(tables.size());
    }

    // --- schema re-import pruning (US-3.7, event-driven: nfr-design-plan.md Question 1) ---

    @EventListener
    @Transactional
    public void onSchemaImported(SchemaImportedEvent event) {
        var prunedCount = 0;
        for (var ref : event.getRemovedTableRefs()) {
            var parts = ref.split("\\.", 2);
            if (parts.length != 2) {
                continue;
            }
            var found =
                    tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(
                            event.getConnectionId(), parts[0], parts[1]);
            if (found.isPresent()) {
                var tableCustomization = found.get();
                var columnIds =
                        columnCustomizationRepository.findAllByTableCustomizationId(tableCustomization.getId()).stream()
                                .map(ColumnCustomization::getId)
                                .toList();
                if (!columnIds.isEmpty()) {
                    validationRuleRepository.deleteAllByColumnCustomizationIdIn(columnIds);
                }
                columnCustomizationRepository.deleteAllByTableCustomizationId(tableCustomization.getId());
                tableCustomizationRepository.deleteById(tableCustomization.getId());
                prunedCount++;
            }
        }

        for (var ref : event.getRemovedColumnRefs()) {
            var parts = ref.split("\\.", 3);
            if (parts.length != 3) {
                continue;
            }
            var found =
                    tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(
                            event.getConnectionId(), parts[0], parts[1]);
            if (found.isEmpty()) {
                continue;
            }
            var columnFound = columnCustomizationRepository.findByTableCustomizationIdAndColumnName(found.get().getId(), parts[2]);
            if (columnFound.isPresent()) {
                validationRuleRepository.deleteAllByColumnCustomizationIdIn(List.of(columnFound.get().getId()));
                columnCustomizationRepository.delete(columnFound.get());
                prunedCount++;
            }
        }

        event.setPrunedCustomizationCount(prunedCount);
    }

    // --- shared helpers ---

    private DbSchema findSchemaOrThrow(Long connectionId, String schemaName) {
        return schemaRepository
                .findByConnectionIdAndSchemaName(connectionId, schemaName)
                .orElseThrow(MasterMaintenanceException::notFound);
    }

    private DbTable findTableOrThrow(Long schemaId, String tableName) {
        return tableRepository.findBySchemaIdAndTableName(schemaId, tableName).orElseThrow(MasterMaintenanceException::notFound);
    }

    private TargetConnection findConnectionOrThrow(Long connectionId) {
        return connectionRepository.findById(connectionId).orElseThrow(MasterMaintenanceException::notFound);
    }

    private void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw MasterMaintenanceException.invalidIdentifier();
        }
    }

    private void validateRawClause(String clause) {
        if (UNSAFE_RAW_CLAUSE_PATTERN.matcher(clause).find()) {
            throw MasterMaintenanceException.unsafeRawClause();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(Map<String, Object> map) {
        return map == null || map.isEmpty();
    }

    private List<SelectOption> parseSelectOptions(String json) {
        if (json == null) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, new TypeReference<List<SelectOption>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private String writeSelectOptions(List<SelectOption> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            return null;
        }
    }
}
