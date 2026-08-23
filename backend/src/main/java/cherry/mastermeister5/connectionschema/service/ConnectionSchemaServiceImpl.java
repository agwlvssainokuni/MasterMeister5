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

package cherry.mastermeister5.connectionschema.service;

import cherry.mastermeister5.accesscontrol.cache.PermissionCacheService;
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.ConnectionStatus;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.entity.ForeignKeyConstraint;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.connectionschema.repository.ForeignKeyConstraintJpaRepository;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.platform.security.ConnectionSecretCipher;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * business-logic-model.md / business-rules.md (BR-1〜BR-18).
 */
@Service
class ConnectionSchemaServiceImpl implements ConnectionSchemaService {

    /** nfr-design-patterns.md Question 2: JDBC identifier allowlist. */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final TargetConnectionJpaRepository connectionRepository;
    private final DbSchemaJpaRepository schemaRepository;
    private final DbTableJpaRepository tableRepository;
    private final DbColumnJpaRepository columnRepository;
    private final ForeignKeyConstraintJpaRepository foreignKeyRepository;
    private final ConnectionSecretCipher secretCipher;
    private final ConnectionPoolRegistry poolRegistry;
    private final SchemaMetadataReader metadataReader;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private final PermissionCacheService permissionCacheService;
    private final ApplicationEventPublisher eventPublisher;

    ConnectionSchemaServiceImpl(
            TargetConnectionJpaRepository connectionRepository,
            DbSchemaJpaRepository schemaRepository,
            DbTableJpaRepository tableRepository,
            DbColumnJpaRepository columnRepository,
            ForeignKeyConstraintJpaRepository foreignKeyRepository,
            ConnectionSecretCipher secretCipher,
            ConnectionPoolRegistry poolRegistry,
            SchemaMetadataReader metadataReader,
            AuditLogService auditLogService,
            PlatformTransactionManager transactionManager,
            PermissionCacheService permissionCacheService,
            ApplicationEventPublisher eventPublisher) {
        this.connectionRepository = connectionRepository;
        this.schemaRepository = schemaRepository;
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
        this.foreignKeyRepository = foreignKeyRepository;
        this.secretCipher = secretCipher;
        this.poolRegistry = poolRegistry;
        this.metadataReader = metadataReader;
        this.auditLogService = auditLogService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.permissionCacheService = permissionCacheService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Long registerConnection(RegisterConnectionCommand command, Long actorUserId) {
        validateIdentifier(command.host());
        validateIdentifier(command.databaseName());
        if (command.schemaNameHint() != null && !command.schemaNameHint().isBlank()) {
            validateIdentifier(command.schemaNameHint());
        }
        if (connectionRepository.findByName(command.name()).isPresent()) {
            throw ConnectionException.nameAlreadyExists();
        }

        try (var dataSource =
                poolRegistry.transientDataSourceFor(
                        command.rdbmsType(),
                        command.host(),
                        command.port(),
                        command.databaseName(),
                        command.extraParams(),
                        command.username(),
                        command.password())) {
            try (var ignored = dataSource.getConnection()) {
                // BR-2: connection test only; nothing else to do on success.
            }
        } catch (SQLException e) {
            throw ConnectionException.connectionTestFailed(classifyFailure(e));
        }

        var connection =
                new TargetConnection(
                        command.name(),
                        command.rdbmsType(),
                        command.host(),
                        command.port(),
                        command.databaseName(),
                        command.schemaNameHint(),
                        command.extraParams(),
                        command.username(),
                        secretCipher.encrypt(command.password()));
        connectionRepository.save(connection);

        auditLogService.recordEvent(
                AuditEventType.CONNECTION_REGISTERED,
                actorUserId,
                null,
                Map.of("connectionName", command.name()));
        return connection.getId();
    }

    @Override
    @Transactional
    public void updateConnection(Long connectionId, UpdateConnectionCommand command, Long actorUserId) {
        validateIdentifier(command.host());
        validateIdentifier(command.databaseName());
        if (command.schemaNameHint() != null && !command.schemaNameHint().isBlank()) {
            validateIdentifier(command.schemaNameHint());
        }
        var connection = findConnectionOrThrow(connectionId);
        connectionRepository
                .findByName(command.name())
                .filter(existing -> !existing.getId().equals(connectionId))
                .ifPresent(
                        existing -> {
                            throw ConnectionException.nameAlreadyExists();
                        });

        var rawPassword =
                (command.password() == null || command.password().isBlank())
                        ? secretCipher.decrypt(connection.getEncryptedPassword())
                        : command.password();

        try (var dataSource =
                poolRegistry.transientDataSourceFor(
                        command.rdbmsType(),
                        command.host(),
                        command.port(),
                        command.databaseName(),
                        command.extraParams(),
                        command.username(),
                        rawPassword)) {
            try (var ignored = dataSource.getConnection()) {
                // BR-2: connection test only; nothing else to do on success.
            }
        } catch (SQLException e) {
            throw ConnectionException.connectionTestFailed(classifyFailure(e));
        }

        connection.update(
                command.name(),
                command.rdbmsType(),
                command.host(),
                command.port(),
                command.databaseName(),
                command.schemaNameHint(),
                command.extraParams(),
                command.username(),
                secretCipher.encrypt(rawPassword));
        connectionRepository.save(connection);
        poolRegistry.evict(connectionId);

        auditLogService.recordEvent(
                AuditEventType.CONNECTION_UPDATED,
                actorUserId,
                null,
                Map.of("connectionId", connectionId, "connectionName", command.name()));
    }

    @Override
    @Transactional
    public void deactivateConnection(Long connectionId, Long actorUserId) {
        var connection = findConnectionOrThrow(connectionId);
        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw ConnectionException.notActive();
        }
        connection.setStatus(ConnectionStatus.DEACTIVATED);
        connectionRepository.save(connection);
        poolRegistry.evict(connectionId);
        auditLogService.recordEvent(
                AuditEventType.CONNECTION_DEACTIVATED, actorUserId, null, Map.of("connectionId", connectionId));
    }

    @Override
    @Transactional
    public void reactivateConnection(Long connectionId, Long actorUserId) {
        var connection = findConnectionOrThrow(connectionId);
        if (connection.getStatus() != ConnectionStatus.DEACTIVATED) {
            throw ConnectionException.notDeactivated();
        }
        connection.setStatus(ConnectionStatus.ACTIVE);
        connectionRepository.save(connection);
        auditLogService.recordEvent(
                AuditEventType.CONNECTION_REACTIVATED, actorUserId, null, Map.of("connectionId", connectionId));
    }

    @Override
    public SchemaImportResult importSchema(Long connectionId, Long actorUserId) {
        var connection = findConnectionOrThrow(connectionId);
        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw ConnectionException.notActive();
        }

        List<DiscoveredSchema> discoveredSchemas;
        try (var jdbcConnection = poolRegistry.dataSourceFor(connection).getConnection()) {
            discoveredSchemas = metadataReader.readSchemas(jdbcConnection, connection.getSchemaNameHint());
        } catch (SQLException e) {
            throw ConnectionException.connectionTestFailed(classifyFailure(e));
        }

        var totalTables = 0;
        var totalColumns = 0;
        var removedTableRefs = new ArrayList<String>();
        var removedColumnRefs = new ArrayList<String>();
        var failures = new ArrayList<SchemaImportResult.SchemaImportFailure>();
        var schemasImported = 0;

        for (var discovered : discoveredSchemas) {
            try {
                var replaced =
                        transactionTemplate.execute(
                                status -> replaceSchemaData(connectionId, discovered));
                schemasImported++;
                totalTables += replaced.tablesImported();
                totalColumns += replaced.columnsImported();
                removedTableRefs.addAll(replaced.removedTableRefs());
                removedColumnRefs.addAll(replaced.removedColumnRefs());
            } catch (RuntimeException e) {
                // nfr-design-plan.md Question 6: per-schema transaction, partial success.
                failures.add(new SchemaImportResult.SchemaImportFailure(discovered.schemaName(), "UNKNOWN"));
            }
        }

        auditLogService.recordEvent(
                AuditEventType.SCHEMA_IMPORTED,
                actorUserId,
                null,
                Map.of(
                        "connectionId", connectionId,
                        "schemasImported", schemasImported,
                        "tablesImported", totalTables,
                        "removedTables", removedTableRefs.size()));

        // NFR Design logical-components.md "Unit 3との連携": requirements.md
        // requires the effective-permission cache to be invalidated whenever
        // a schema is re-imported, regardless of whether any permission
        // entries reference the changed tables/columns by name.
        permissionCacheService.invalidateByConnection(connectionId);

        // Unit 5 nfr-design-plan.md Question 1: publish rather than call
        // MasterMaintenanceService directly, so this package never imports
        // anything from `mastermaintenance` (avoids a package cycle, since
        // mastermaintenance already depends on connectionschema). Spring's
        // default synchronous @EventListener dispatch means the listener
        // has already set prunedCustomizationCount by the time this returns.
        var schemaImportedEvent =
                new SchemaImportedEvent(this, connectionId, removedTableRefs, removedColumnRefs);
        eventPublisher.publishEvent(schemaImportedEvent);

        return new SchemaImportResult(
                schemasImported,
                totalTables,
                totalColumns,
                removedTableRefs,
                removedColumnRefs,
                failures,
                schemaImportedEvent.getPrunedCustomizationCount());
    }

    private record SchemaReplaceResult(
            int tablesImported, int columnsImported, List<String> removedTableRefs, List<String> removedColumnRefs) {
    }

    private SchemaReplaceResult replaceSchemaData(Long connectionId, DiscoveredSchema discovered) {
        var schema =
                schemaRepository
                        .findByConnectionIdAndSchemaName(connectionId, discovered.schemaName())
                        .orElseGet(() -> schemaRepository.save(new DbSchema(connectionId, discovered.schemaName())));
        schema.markImported();
        schemaRepository.save(schema);

        var oldTables = tableRepository.findAllBySchemaId(schema.getId());
        var oldTableIds = oldTables.stream().map(DbTable::getId).toList();
        var oldTableNamesById =
                oldTables.stream().collect(Collectors.toMap(DbTable::getId, DbTable::getTableName));
        var oldColumnsByTableId =
                columnRepository.findAllByTableIdIn(oldTableIds).stream()
                        .collect(Collectors.groupingBy(DbColumn::getTableId));

        var newTableNames = discovered.tables().stream().map(DiscoveredTable::tableName).collect(Collectors.toSet());
        var removedTableRefs = new ArrayList<String>();
        var removedColumnRefs = new ArrayList<String>();
        for (var oldTableId : oldTableIds) {
            var oldTableName = oldTableNamesById.get(oldTableId);
            if (!newTableNames.contains(oldTableName)) {
                removedTableRefs.add(discovered.schemaName() + "." + oldTableName);
            } else {
                var newColumnNames =
                        discovered.tables().stream()
                                .filter(t -> t.tableName().equals(oldTableName))
                                .findFirst()
                                .map(t -> t.columns().stream().map(DiscoveredColumn::columnName).collect(Collectors.toSet()))
                                .orElse(Set.of());
                for (var oldColumn : oldColumnsByTableId.getOrDefault(oldTableId, List.of())) {
                    if (!newColumnNames.contains(oldColumn.getColumnName())) {
                        removedColumnRefs.add(
                                discovered.schemaName() + "." + oldTableName + "." + oldColumn.getColumnName());
                    }
                }
            }
        }

        foreignKeyRepository.deleteAllByFromTableIdIn(oldTableIds);
        columnRepository.deleteAllByTableIdIn(oldTableIds);
        tableRepository.deleteAllBySchemaId(schema.getId());

        var newTableIdByName = new HashMap<String, Long>();
        var columnsImported = 0;
        for (var discoveredTable : discovered.tables()) {
            var table =
                    tableRepository.save(
                            new DbTable(
                                    schema.getId(),
                                    discoveredTable.tableName(),
                                    discoveredTable.tableType(),
                                    discoveredTable.comment()));
            newTableIdByName.put(discoveredTable.tableName(), table.getId());
            for (var discoveredColumn : discoveredTable.columns()) {
                columnRepository.save(
                        new DbColumn(
                                table.getId(),
                                discoveredColumn.columnName(),
                                discoveredColumn.ordinalPosition(),
                                discoveredColumn.dataType(),
                                discoveredColumn.nullable(),
                                discoveredColumn.primaryKey(),
                                discoveredColumn.comment()));
                columnsImported++;
            }
        }
        for (var fk : discovered.foreignKeys()) {
            var fromTableId = newTableIdByName.get(fk.fromTableName());
            var toTableId = newTableIdByName.get(fk.toTableName());
            if (fromTableId != null && toTableId != null) {
                foreignKeyRepository.save(
                        new ForeignKeyConstraint(fromTableId, fk.fromColumnName(), toTableId, fk.toColumnName()));
            }
        }

        return new SchemaReplaceResult(
                discovered.tables().size(), columnsImported, removedTableRefs, removedColumnRefs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemaView> getSchema(Long connectionId) {
        var schemas = schemaRepository.findAllByConnectionId(connectionId);
        var result = new ArrayList<SchemaView>();
        for (var schema : schemas) {
            var tables = tableRepository.findAllBySchemaId(schema.getId());
            var tableIds = tables.stream().map(DbTable::getId).toList();
            var columnsByTableId =
                    columnRepository.findAllByTableIdIn(tableIds).stream()
                            .collect(Collectors.groupingBy(DbColumn::getTableId));
            var tableViews =
                    tables.stream()
                            .map(
                                    table ->
                                            new TableView(
                                                    table.getTableName(),
                                                    table.getTableType(),
                                                    table.getComment(),
                                                    columnsByTableId.getOrDefault(table.getId(), List.of()).stream()
                                                            .map(
                                                                    c ->
                                                                            new ColumnView(
                                                                                    c.getColumnName(),
                                                                                    c.getDataType(),
                                                                                    c.isNullable(),
                                                                                    c.isPrimaryKey(),
                                                                                    c.getComment()))
                                                            .toList()))
                            .toList();
            result.add(new SchemaView(schema.getSchemaName(), tableViews));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSchemaAllowed(Long connectionId, String schemaName) {
        return schemaRepository.findByConnectionIdAndSchemaName(connectionId, schemaName).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionSummary> listConnections() {
        return connectionRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(
                        c ->
                                new ConnectionSummary(
                                        c.getId(),
                                        c.getName(),
                                        c.getRdbmsType(),
                                        c.getHost(),
                                        c.getPort(),
                                        c.getDatabaseName(),
                                        c.getSchemaNameHint(),
                                        c.getExtraParams(),
                                        c.getUsername(),
                                        c.getStatus(),
                                        lastSchemaImportAt(c.getId())))
                .toList();
    }

    private Instant lastSchemaImportAt(Long connectionId) {
        return schemaRepository.findAllByConnectionId(connectionId).stream()
                .map(DbSchema::getImportedAt)
                .max(Instant::compareTo)
                .orElse(null);
    }

    private TargetConnection findConnectionOrThrow(Long connectionId) {
        return connectionRepository.findById(connectionId).orElseThrow(ConnectionException::notFound);
    }

    private void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw ConnectionException.invalidIdentifier();
        }
    }

    private String classifyFailure(SQLException e) {
        var sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("28")) {
            return "AUTHENTICATION_FAILED";
        }
        var message = String.valueOf(e.getMessage()).toLowerCase();
        if (message.contains("timeout")) {
            return "TIMEOUT";
        }
        if (message.contains("unknown host")
                || message.contains("connection refused")
                || message.contains("could not connect")) {
            return "UNREACHABLE_HOST";
        }
        return "UNKNOWN";
    }
}
