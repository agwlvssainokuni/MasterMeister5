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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.accesscontrol.cache.PermissionCacheService;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.ConnectionStatus;
import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.connectionschema.repository.DbColumnJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbSchemaJpaRepository;
import cherry.mastermeister5.connectionschema.repository.DbTableJpaRepository;
import cherry.mastermeister5.connectionschema.repository.ForeignKeyConstraintJpaRepository;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.platform.security.ConnectionSecretCipher;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Mocks and {@code service} are plain field initializers (not {@code @Mock}/
 * {@code @BeforeEach}) for the same reason as Unit 2's
 * UserAccountServiceImplTest: this class mixes {@code @Test} and jqwik
 * {@code @Property} methods, and jqwik does not process JUnit Jupiter
 * extensions.
 */
class ConnectionSchemaServiceImplTest {

    private final TargetConnectionJpaRepository connectionRepository =
            mock(TargetConnectionJpaRepository.class);
    private final DbSchemaJpaRepository schemaRepository = mock(DbSchemaJpaRepository.class);
    private final DbTableJpaRepository tableRepository = mock(DbTableJpaRepository.class);
    private final DbColumnJpaRepository columnRepository = mock(DbColumnJpaRepository.class);
    private final ForeignKeyConstraintJpaRepository foreignKeyRepository =
            mock(ForeignKeyConstraintJpaRepository.class);
    private final ConnectionSecretCipher secretCipher = mock(ConnectionSecretCipher.class);
    private final ConnectionPoolRegistry poolRegistry = mock(ConnectionPoolRegistry.class);
    private final SchemaMetadataReader metadataReader = mock(SchemaMetadataReader.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final PermissionCacheService permissionCacheService = mock(PermissionCacheService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private static final PlatformTransactionManager NOOP_TX_MANAGER =
            new PlatformTransactionManager() {
                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) {
                    return new SimpleTransactionStatus();
                }

                @Override
                public void commit(TransactionStatus status) {
                }

                @Override
                public void rollback(TransactionStatus status) {
                }
            };

    private final ConnectionSchemaServiceImpl service =
            new ConnectionSchemaServiceImpl(
                    connectionRepository,
                    schemaRepository,
                    tableRepository,
                    columnRepository,
                    foreignKeyRepository,
                    secretCipher,
                    poolRegistry,
                    metadataReader,
                    auditLogService,
                    NOOP_TX_MANAGER,
                    permissionCacheService,
                    eventPublisher);

    // --- registerConnection ---

    @Test
    void registerConnectionEncryptsThePasswordAndSaves() throws SQLException {
        when(connectionRepository.findByName("conn1")).thenReturn(Optional.empty());
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.transientDataSourceFor(RdbmsType.MYSQL, "localhost", 3306, "db", null, "user", "pass"))
                .thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        when(secretCipher.encrypt("pass")).thenReturn("encrypted-pass");
        when(connectionRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            var connection = invocation.getArgument(0, TargetConnection.class);
                            setId(connection, 1L);
                            return connection;
                        });

        var id =
                service.registerConnection(
                        new RegisterConnectionCommand(
                                "conn1", RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "pass"),
                        99L);

        assertThat(id).isEqualTo(1L);
    }

    @Test
    void registerConnectionRejectsADuplicateName() {
        when(connectionRepository.findByName("conn1"))
                .thenReturn(Optional.of(activeConnection(1L, "conn1")));

        assertThatThrownBy(
                        () ->
                                service.registerConnection(
                                        new RegisterConnectionCommand(
                                                "conn1", RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "pass"),
                                        99L))
                .isInstanceOf(ConnectionException.class)
                .extracting("errorCode")
                .isEqualTo("CONNECTION_NAME_ALREADY_EXISTS");
    }

    @Test
    void registerConnectionRejectsAnInvalidHostIdentifier() {
        assertThatThrownBy(
                        () ->
                                service.registerConnection(
                                        new RegisterConnectionCommand(
                                                "conn1", RdbmsType.MYSQL, "bad host!", 3306, "db", null, null, "user", "pass"),
                                        99L))
                .isInstanceOf(ConnectionException.class)
                .extracting("errorCode")
                .isEqualTo("CONNECTION_INVALID_IDENTIFIER");
    }

    @Test
    void registerConnectionClassifiesAConnectionTestFailure() throws SQLException {
        when(connectionRepository.findByName("conn1")).thenReturn(Optional.empty());
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.transientDataSourceFor(RdbmsType.MYSQL, "localhost", 3306, "db", null, "user", "pass"))
                .thenReturn(dataSource);
        when(dataSource.getConnection())
                .thenThrow(new SQLException("Connection refused by host", "08001"));

        assertThatThrownBy(
                        () ->
                                service.registerConnection(
                                        new RegisterConnectionCommand(
                                                "conn1", RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "pass"),
                                        99L))
                .isInstanceOf(ConnectionException.class)
                .extracting("errorCode")
                .isEqualTo("CONNECTION_TEST_FAILED_UNREACHABLE_HOST");
    }

    // --- deactivate / reactivate: state transition guards ---

    @Test
    void deactivateConnectionEvictsThePool() {
        var connection = activeConnection(2L, "conn2");
        when(connectionRepository.findById(2L)).thenReturn(Optional.of(connection));

        service.deactivateConnection(2L, 1L);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.DEACTIVATED);
    }

    @Test
    void reactivateConnectionFlipsStatusBackToActive() {
        var connection = activeConnection(3L, "conn3");
        connection.setStatus(ConnectionStatus.DEACTIVATED);
        when(connectionRepository.findById(3L)).thenReturn(Optional.of(connection));

        service.reactivateConnection(3L, 1L);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": only
     * ACTIVE⇄DEACTIVATED is a valid transition.
     */
    @Property
    void deactivateIsRejectedFromAnyNonActiveStatus(@ForAll ConnectionStatus status) {
        if (status == ConnectionStatus.ACTIVE) {
            return;
        }
        var connection = activeConnection(4L, "prop-" + status);
        connection.setStatus(status);
        when(connectionRepository.findById(4L)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.deactivateConnection(4L, 1L))
                .isInstanceOf(ConnectionException.class);
    }

    @Property
    void reactivateIsRejectedFromAnyNonDeactivatedStatus(@ForAll ConnectionStatus status) {
        if (status == ConnectionStatus.DEACTIVATED) {
            return;
        }
        var connection = activeConnection(4L, "prop2-" + status);
        connection.setStatus(status);
        when(connectionRepository.findById(4L)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.reactivateConnection(4L, 1L))
                .isInstanceOf(ConnectionException.class);
    }

    // --- importSchema ---

    @Test
    void importSchemaOnAFirstImportCreatesTablesAndColumns() throws SQLException {
        var connection = activeConnection(5L, "conn5");
        when(connectionRepository.findById(5L)).thenReturn(Optional.of(connection));
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.dataSourceFor(connection)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));

        var discovered =
                new DiscoveredSchema(
                        "public",
                        List.of(
                                new DiscoveredTable(
                                        "t1",
                                        DbTable.Type.TABLE,
                                        null,
                                        List.of(new DiscoveredColumn("id", 1, "BIGINT", false, true, null)))),
                        List.of());
        when(metadataReader.readSchemas(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(discovered));
        when(schemaRepository.findByConnectionIdAndSchemaName(5L, "public")).thenReturn(Optional.empty());
        when(schemaRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            var schema = invocation.getArgument(0, DbSchema.class);
                            setId(schema, 10L);
                            return schema;
                        });
        when(tableRepository.findAllBySchemaId(10L)).thenReturn(List.of());
        when(columnRepository.findAllByTableIdIn(List.of())).thenReturn(List.of());
        var tableIdSequence = new AtomicLong(100L);
        when(tableRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            var table = invocation.getArgument(0, DbTable.class);
                            setId(table, tableIdSequence.incrementAndGet());
                            return table;
                        });

        var result = service.importSchema(5L, 1L);

        assertThat(result.schemasImported()).isEqualTo(1);
        assertThat(result.tablesImported()).isEqualTo(1);
        assertThat(result.columnsImported()).isEqualTo(1);
        assertThat(result.removedTableRefs()).isEmpty();
        assertThat(result.failures()).isEmpty();
    }

    /** nfr-design/logical-components.md "Unit 3との連携": Unit 4 relies on this call. */
    @Test
    void importSchemaInvalidatesThePermissionCacheForTheConnection() throws SQLException {
        var connection = activeConnection(5L, "conn5");
        when(connectionRepository.findById(5L)).thenReturn(Optional.of(connection));
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.dataSourceFor(connection)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        when(metadataReader.readSchemas(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(new DiscoveredSchema("public", List.of(), List.of())));
        when(schemaRepository.findByConnectionIdAndSchemaName(5L, "public")).thenReturn(Optional.empty());
        when(schemaRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            var schema = invocation.getArgument(0, DbSchema.class);
                            setId(schema, 10L);
                            return schema;
                        });
        when(tableRepository.findAllBySchemaId(10L)).thenReturn(List.of());
        when(columnRepository.findAllByTableIdIn(List.of())).thenReturn(List.of());

        service.importSchema(5L, 1L);

        org.mockito.Mockito.verify(permissionCacheService).invalidateByConnection(5L);
    }

    /** Unit 5 nfr-design-plan.md Question 1: event-driven prune, no direct dependency on Unit 5. */
    @Test
    void importSchemaPublishesASchemaImportedEvent() throws SQLException {
        var connection = activeConnection(5L, "conn5");
        when(connectionRepository.findById(5L)).thenReturn(Optional.of(connection));
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.dataSourceFor(connection)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        when(metadataReader.readSchemas(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(new DiscoveredSchema("public", List.of(), List.of())));
        when(schemaRepository.findByConnectionIdAndSchemaName(5L, "public")).thenReturn(Optional.empty());
        when(schemaRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            var schema = invocation.getArgument(0, DbSchema.class);
                            setId(schema, 10L);
                            return schema;
                        });
        when(tableRepository.findAllBySchemaId(10L)).thenReturn(List.of());
        when(columnRepository.findAllByTableIdIn(List.of())).thenReturn(List.of());

        service.importSchema(5L, 1L);

        org.mockito.Mockito.verify(eventPublisher)
                .publishEvent(org.mockito.ArgumentMatchers.isA(SchemaImportedEvent.class));
    }

    @Test
    void importSchemaDetectsARemovedTable() throws SQLException {
        var connection = activeConnection(6L, "conn6");
        when(connectionRepository.findById(6L)).thenReturn(Optional.of(connection));
        var dataSource = mock(HikariDataSource.class);
        when(poolRegistry.dataSourceFor(connection)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));

        // No tables discovered this time around.
        var discovered = new DiscoveredSchema("public", List.of(), List.of());
        when(metadataReader.readSchemas(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(discovered));

        var existingSchema = new DbSchema(6L, "public");
        setId(existingSchema, 20L);
        when(schemaRepository.findByConnectionIdAndSchemaName(6L, "public"))
                .thenReturn(Optional.of(existingSchema));
        when(schemaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var oldTable = new DbTable(20L, "old_table", DbTable.Type.TABLE, null);
        setId(oldTable, 200L);
        when(tableRepository.findAllBySchemaId(20L)).thenReturn(List.of(oldTable));
        when(columnRepository.findAllByTableIdIn(List.of(200L))).thenReturn(List.of());

        var result = service.importSchema(6L, 1L);

        assertThat(result.removedTableRefs()).containsExactly("public.old_table");
    }

    // --- getSchema / isSchemaAllowed ---

    @Test
    void isSchemaAllowedReflectsWhetherTheSchemaWasImported() {
        when(schemaRepository.findByConnectionIdAndSchemaName(7L, "public"))
                .thenReturn(Optional.of(new DbSchema(7L, "public")));
        when(schemaRepository.findByConnectionIdAndSchemaName(7L, "unknown")).thenReturn(Optional.empty());

        assertThat(service.isSchemaAllowed(7L, "public")).isTrue();
        assertThat(service.isSchemaAllowed(7L, "unknown")).isFalse();
    }

    // --- fixtures ---

    private TargetConnection activeConnection(Long id, String name) {
        var connection =
                new TargetConnection(
                        name, RdbmsType.MYSQL, "localhost", 3306, "db", null, null, "user", "encrypted");
        setId(connection, id);
        return connection;
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
