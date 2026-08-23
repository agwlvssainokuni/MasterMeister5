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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.accesscontrol.cache.EffectivePermission;
import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.connectionschema.service.ConnectionPoolRegistry;
import cherry.mastermeister5.connectionschema.service.SchemaImportedEvent;
import cherry.mastermeister5.mastermaintenance.entity.TableCustomization;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRule;
import cherry.mastermeister5.mastermaintenance.entity.ValidationRuleType;
import cherry.mastermeister5.mastermaintenance.repository.ColumnCustomizationJpaRepository;
import cherry.mastermeister5.mastermaintenance.repository.TableCustomizationJpaRepository;
import cherry.mastermeister5.mastermaintenance.repository.ValidationRuleJpaRepository;
import cherry.mastermeister5.platform.BulkAccessProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/**
 * Uses a real H2 database as the "target RDBMS" (via a real {@link JdbcDataSource})
 * for listRecords/applyChanges, since the whole point of those methods is
 * generating and executing correct SQL against an actual driver — mocking
 * JDBC internals would test nothing. Unit 5's own customization repositories
 * and Unit 4's AccessControlService are mocked (plain field initializers,
 * not {@code @Mock}/{@code @BeforeEach} — Unit 2/3/4's tests apply the same
 * lesson: jqwik-mixed classes don't process JUnit Jupiter extensions,
 * kept here for consistency even though this class has no {@code @Property}).
 */
class MasterMaintenanceServiceImplTest {

    private final TableCustomizationJpaRepository tableCustomizationRepository = mock(TableCustomizationJpaRepository.class);
    private final ColumnCustomizationJpaRepository columnCustomizationRepository = mock(ColumnCustomizationJpaRepository.class);
    private final ValidationRuleJpaRepository validationRuleRepository = mock(ValidationRuleJpaRepository.class);
    private final DbSchemaJpaRepository schemaRepository = mock(DbSchemaJpaRepository.class);
    private final DbTableJpaRepository tableRepository = mock(DbTableJpaRepository.class);
    private final DbColumnJpaRepository columnRepository = mock(DbColumnJpaRepository.class);
    private final TargetConnectionJpaRepository connectionRepository = mock(TargetConnectionJpaRepository.class);
    private final ConnectionPoolRegistry poolRegistry = mock(ConnectionPoolRegistry.class);
    private final AccessControlService accessControlService = mock(AccessControlService.class);
    private final CustomizationYamlMapper yamlMapper = mock(CustomizationYamlMapper.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final BulkAccessProperties bulkAccessProperties = new BulkAccessProperties(100);

    private final MasterMaintenanceServiceImpl service =
            new MasterMaintenanceServiceImpl(
                    tableCustomizationRepository,
                    columnCustomizationRepository,
                    validationRuleRepository,
                    schemaRepository,
                    tableRepository,
                    columnRepository,
                    connectionRepository,
                    poolRegistry,
                    accessControlService,
                    yamlMapper,
                    auditLogService,
                    bulkAccessProperties);

    private final Connection setupConnection;

    /**
     * Instance initializer, not {@code @BeforeEach}: this class mixes
     * {@code @Test} and jqwik {@code @Property} methods, and jqwik does not
     * run JUnit Jupiter's {@code @BeforeEach}/{@code @AfterEach} (same lesson
     * as Unit 2/3/4). Each test instance gets its own uniquely-named
     * in-memory H2 database that is never explicitly closed; it is harmless
     * to leak for the lifetime of the test JVM.
     */
    {
        try {
            var dbName = "masterdatatest_" + UUID.randomUUID().toString().replace("-", "");
            var jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
            setupConnection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = setupConnection.createStatement()) {
                statement.execute(
                        "CREATE TABLE t1 (id BIGINT PRIMARY KEY, name VARCHAR(50), secret VARCHAR(50))");
                statement.execute("INSERT INTO t1 VALUES (1, 'Alice', 'classified-a')");
                statement.execute("INSERT INTO t1 VALUES (2, 'Bob', 'classified-b')");
                statement.execute("CREATE TABLE no_pk_table (label VARCHAR(50))");
            }

            var dataSource = new JdbcDataSource();
            dataSource.setUrl(jdbcUrl);
            when(poolRegistry.dataSourceFor(any())).thenReturn(dataSource);
            when(connectionRepository.findById(1L))
                    .thenReturn(
                            Optional.of(new TargetConnection("conn1", RdbmsType.H2, "localhost", 9092, dbName, null, null, "sa", "enc")));

            var schema = new DbSchema(1L, "public");
            setId(schema, 100L);
            when(schemaRepository.findByConnectionIdAndSchemaName(1L, "public")).thenReturn(Optional.of(schema));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void mockTable(String tableName, List<DbColumn> columns, boolean hasPrimaryKey) {
        var table = new DbTable(100L, tableName, DbTable.Type.TABLE, null);
        setId(table, 200L);
        when(tableRepository.findBySchemaIdAndTableName(100L, tableName)).thenReturn(Optional.of(table));
        when(columnRepository.findAllByTableId(200L)).thenReturn(columns);
        when(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", tableName))
                .thenReturn(Optional.empty());
    }

    private DbColumn column(String name, boolean primaryKey) {
        return new DbColumn(200L, name, 1, "VARCHAR", true, primaryKey, null);
    }

    // --- listRecords ---

    @Test
    void listRecordsExcludesColumnsWithoutReadPermission() {
        mockTable("t1", List.of(column("id", true), column("name", false), column("secret", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "secret", new EffectivePermission(PrimaryLevel.NONE, false, false)));

        var page =
                service.listRecords(
                        new ListRecordsCommand(1L, "public", "t1", 9L, new FilterCriteria(List.of(), null), null, 0, 50));

        assertThat(page.columns()).extracting(ColumnDef::columnName).containsExactlyInAnyOrder("id", "name");
        assertThat(page.rows()).allSatisfy(row -> assertThat(row).doesNotContainKey("secret"));
        assertThat(page.totalCount()).isEqualTo(2);
    }

    @Test
    void listRecordsAppliesAFilterCondition() {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false)));

        var filter = new FilterCriteria(List.of(new FilterCondition("name", FilterOperator.EQ, "Alice")), null);
        var page = service.listRecords(new ListRecordsCommand(1L, "public", "t1", 9L, filter, null, 0, 50));

        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.rows()).singleElement().satisfies(row -> assertThat(row).containsEntry("name", "Alice"));
    }

    @Test
    void listRecordsRejectsAnUnsafeRawWhereClause() {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false)));
        var filter = new FilterCriteria(null, "1=1; DROP TABLE t1");

        assertThatThrownBy(() -> service.listRecords(new ListRecordsCommand(1L, "public", "t1", 9L, filter, null, 0, 50)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_UNSAFE_RAW_CLAUSE");
    }

    @Test
    void listRecordsRejectsAFilterOnANonReadableColumn() {
        mockTable("t1", List.of(column("id", true), column("name", false), column("secret", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "secret", new EffectivePermission(PrimaryLevel.NONE, false, false)));
        var filter = new FilterCriteria(List.of(new FilterCondition("secret", FilterOperator.EQ, "x")), null);

        assertThatThrownBy(() -> service.listRecords(new ListRecordsCommand(1L, "public", "t1", 9L, filter, null, 0, 50)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_PERMISSION_DENIED");
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ": no raw clause containing a stacked statement is ever accepted. */
    @Property
    void anyRawClauseContainingASemicolonIsAlwaysRejected(@ForAll @AlphaChars String prefix, @ForAll @AlphaChars String suffix) {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false)));
        var filter = new FilterCriteria(null, prefix + ";" + suffix);

        assertThatThrownBy(() -> service.listRecords(new ListRecordsCommand(1L, "public", "t1", 9L, filter, null, 0, 50)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_UNSAFE_RAW_CLAUSE");
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ": customization never widens what permission restricts. */
    @Property
    void aColumnWithoutReadPermissionIsNeverVisibleRegardlessOfCustomization(@ForAll PrimaryLevel level) {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(level, false, false)));

        var page = service.listRecords(new ListRecordsCommand(1L, "public", "t1", 9L, new FilterCriteria(List.of(), null), null, 0, 50));

        var nameVisible = page.columns().stream().anyMatch(c -> c.columnName().equals("name"));
        assertThat(nameVisible).isEqualTo(level != PrimaryLevel.NONE);
    }

    /**
     * tech-stack-decisions.md (Unit 6) Question 3 / business-rules.md BR-15:
     * Unit 5's listRecords must also record the "bulk data access" event once
     * the matching row count reaches MM5_BULK_ACCESS_THRESHOLD, even though
     * this test's page size (50) returns fewer rows per call.
     */
    @Test
    void listRecordsRecordsABulkDataAccessEventWhenTotalCountReachesTheThreshold() throws Exception {
        try (var statement = setupConnection.createStatement()) {
            statement.execute("CREATE TABLE bulk_table (id BIGINT PRIMARY KEY)");
            for (var i = 1; i <= 100; i++) {
                statement.execute("INSERT INTO bulk_table VALUES (" + i + ")");
            }
        }
        mockTable("bulk_table", List.of(column("id", true)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(Map.of("id", new EffectivePermission(PrimaryLevel.READ, false, false)));

        var page =
                service.listRecords(
                        new ListRecordsCommand(1L, "public", "bulk_table", 9L, new FilterCriteria(List.of(), null), null, 0, 50));

        assertThat(page.totalCount()).isEqualTo(100);
        verify(auditLogService)
                .recordEvent(eq(AuditEventType.BULK_DATA_ACCESSED), eq(9L), isNull(), any());
    }

    // --- listTables ---

    @Test
    void listTablesReflectsTheResolvedTableLevelCreateAndDeletePermissions() {
        var schema = new DbSchema(1L, "public");
        setId(schema, 100L);
        when(schemaRepository.findAllByConnectionId(1L)).thenReturn(List.of(schema));
        var table = new DbTable(100L, "t1", DbTable.Type.TABLE, null);
        setId(table, 200L);
        when(tableRepository.findAllBySchemaId(100L)).thenReturn(List.of(table));
        when(columnRepository.findAllByTableId(200L)).thenReturn(List.of(column("id", true)));
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(Map.of("id", new EffectivePermission(PrimaryLevel.READ, false, false)));
        when(accessControlService.resolveEffectivePermission(any(), any(), any(), any(), any(), any()))
                .thenReturn(new EffectivePermission(PrimaryLevel.READ, true, false));

        var tables = service.listTables(1L, 9L);

        assertThat(tables).singleElement().satisfies(
                t -> {
                    assertThat(t.canCreate()).isTrue();
                    assertThat(t.canDelete()).isFalse();
                });
    }

    // --- applyChanges ---

    @Test
    void applyChangesInsertsUpdatesAndDeletesInASingleCall() throws Exception {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.UPDATE, false, false),
                                "name", new EffectivePermission(PrimaryLevel.UPDATE, false, false)));
        when(accessControlService.resolveEffectivePermission(any(), any(), any(), any(), any(), any()))
                .thenReturn(new EffectivePermission(PrimaryLevel.UPDATE, true, true));

        var changes =
                List.of(
                        new RecordChange(ChangeOperation.CREATE, Map.of(), Map.of("id", 3L, "name", "Carol")),
                        new RecordChange(ChangeOperation.UPDATE, Map.of("id", 1L), Map.of("name", "Alicia")),
                        new RecordChange(ChangeOperation.DELETE, Map.of("id", 2L), Map.of()));

        var result = service.applyChanges(1L, "public", "t1", 9L, new RecordChangeSet(changes));

        assertThat(result).isEqualTo(new ApplyResult(1, 1, 1));
        try (var statement = setupConnection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT id, name FROM t1 ORDER BY id");
            resultSet.next();
            assertThat(resultSet.getLong("id")).isEqualTo(1L);
            assertThat(resultSet.getString("name")).isEqualTo("Alicia");
            resultSet.next();
            assertThat(resultSet.getLong("id")).isEqualTo(3L);
            assertThat(resultSet.getString("name")).isEqualTo("Carol");
            assertThat(resultSet.next()).isFalse();
        }
    }

    /** business-rules.md BR-7: one invalid change rejects the whole batch, no DB writes at all. */
    @Test
    void applyChangesLeavesTheDatabaseUntouchedWhenOneChangeFailsValidation() throws Exception {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.READ, false, false),
                                "name", new EffectivePermission(PrimaryLevel.READ, false, false)));

        var changes = List.of(new RecordChange(ChangeOperation.UPDATE, Map.of("id", 1L), Map.of("name", "Alicia")));

        assertThatThrownBy(() -> service.applyChanges(1L, "public", "t1", 9L, new RecordChangeSet(changes)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_PERMISSION_DENIED");

        try (var statement = setupConnection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT name FROM t1 WHERE id = 1");
            resultSet.next();
            assertThat(resultSet.getString("name")).isEqualTo("Alice");
        }
    }

    @Test
    void applyChangesRejectsUpdateOnATableWithoutAPrimaryKey() {
        mockTable("no_pk_table", List.of(column("label", false)), false);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(Map.of("label", new EffectivePermission(PrimaryLevel.UPDATE, false, false)));

        var changes = List.of(new RecordChange(ChangeOperation.UPDATE, Map.of(), Map.of("label", "x")));

        assertThatThrownBy(() -> service.applyChanges(1L, "public", "no_pk_table", 9L, new RecordChangeSet(changes)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_NO_PRIMARY_KEY");
    }

    @Test
    void applyChangesRejectsCreateWhenCanCreateIsFalse() {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.UPDATE, false, false),
                                "name", new EffectivePermission(PrimaryLevel.UPDATE, false, false)));
        when(accessControlService.resolveEffectivePermission(any(), any(), any(), any(), any(), any()))
                .thenReturn(new EffectivePermission(PrimaryLevel.UPDATE, false, false));

        var changes = List.of(new RecordChange(ChangeOperation.CREATE, Map.of(), Map.of("id", 3L, "name", "Carol")));

        assertThatThrownBy(() -> service.applyChanges(1L, "public", "t1", 9L, new RecordChangeSet(changes)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_PERMISSION_DENIED");
    }

    @Test
    void applyChangesEnforcesARegexValidationRule() {
        mockTable("t1", List.of(column("id", true), column("name", false)), true);
        when(accessControlService.resolveEffectivePermissionsForTable(any(), any(), any(), any(), anyList()))
                .thenReturn(
                        Map.of(
                                "id", new EffectivePermission(PrimaryLevel.UPDATE, false, false),
                                "name", new EffectivePermission(PrimaryLevel.UPDATE, false, false)));

        var tableCustomization = new TableCustomization(1L, "public", "t1");
        setId(tableCustomization, 300L);
        when(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", "t1"))
                .thenReturn(Optional.of(tableCustomization));
        var columnCustomization = new cherry.mastermeister5.mastermaintenance.entity.ColumnCustomization(300L, "name");
        setId(columnCustomization, 400L);
        when(columnCustomizationRepository.findAllByTableCustomizationId(300L)).thenReturn(List.of(columnCustomization));
        when(validationRuleRepository.findAllByColumnCustomizationIdIn(List.of(400L)))
                .thenReturn(List.of(new ValidationRule(400L, ValidationRuleType.REGEX, "^[0-9]+$", null, null)));

        var changes = List.of(new RecordChange(ChangeOperation.UPDATE, Map.of("id", 1L), Map.of("name", "not-a-number")));

        assertThatThrownBy(() -> service.applyChanges(1L, "public", "t1", 9L, new RecordChangeSet(changes)))
                .isInstanceOf(MasterMaintenanceException.class)
                .extracting("errorCode")
                .isEqualTo("MASTER_DATA_VALIDATION_FAILED");
    }

    // --- schema re-import pruning ---

    @Test
    void onSchemaImportedPrunesCustomizationsForRemovedTablesAndColumns() {
        var removedTable = new TableCustomization(1L, "public", "removed_table");
        setId(removedTable, 500L);
        when(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", "removed_table"))
                .thenReturn(Optional.of(removedTable));
        when(columnCustomizationRepository.findAllByTableCustomizationId(500L)).thenReturn(List.of());

        var keptTable = new TableCustomization(1L, "public", "kept_table");
        setId(keptTable, 600L);
        when(tableCustomizationRepository.findByConnectionIdAndSchemaNameAndTableName(1L, "public", "kept_table"))
                .thenReturn(Optional.of(keptTable));
        var removedColumn = new cherry.mastermeister5.mastermaintenance.entity.ColumnCustomization(600L, "removed_column");
        setId(removedColumn, 700L);
        when(columnCustomizationRepository.findByTableCustomizationIdAndColumnName(600L, "removed_column"))
                .thenReturn(Optional.of(removedColumn));

        var event =
                new SchemaImportedEvent(this, 1L, List.of("public.removed_table"), List.of("public.kept_table.removed_column"));

        service.onSchemaImported(event);

        assertThat(event.getPrunedCustomizationCount()).isEqualTo(2);
    }

    // --- fixtures ---

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
