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

package cherry.mastermeister5.query.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.connectionschema.service.ConnectionPoolRegistry;
import cherry.mastermeister5.connectionschema.service.ConnectionSchemaService;
import cherry.mastermeister5.platform.BulkAccessProperties;
import cherry.mastermeister5.query.entity.QueryVisibility;
import cherry.mastermeister5.query.entity.SavedQuery;
import cherry.mastermeister5.query.repository.QueryExecutionHistoryJpaRepository;
import cherry.mastermeister5.query.repository.SavedQueryJpaRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Uses a real H2 database as the "target RDBMS" for executeQuery, since the
 * whole point of that method is generating and executing a real SELECT
 * through a real JDBC driver (same rationale as Unit 5's
 * MasterMaintenanceServiceImplTest). Mixes {@code @Test} and jqwik
 * {@code @Property} methods, so setup uses plain field initializers /
 * instance-initializer blocks, never {@code @BeforeEach}/{@code @Mock}
 * (jqwik does not run JUnit Jupiter's lifecycle — same lesson as Unit 2〜5).
 */
class QueryServiceImplTest {

    private final SavedQueryJpaRepository savedQueryRepository = mock(SavedQueryJpaRepository.class);
    private final QueryExecutionHistoryJpaRepository historyRepository = mock(QueryExecutionHistoryJpaRepository.class);
    private final TargetConnectionJpaRepository connectionRepository = mock(TargetConnectionJpaRepository.class);
    private final ConnectionPoolRegistry poolRegistry = mock(ConnectionPoolRegistry.class);
    private final ConnectionSchemaService connectionSchemaService = mock(ConnectionSchemaService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final BulkAccessProperties bulkAccessProperties = new BulkAccessProperties(100);

    private final QueryServiceImpl service =
            new QueryServiceImpl(
                    savedQueryRepository,
                    historyRepository,
                    connectionRepository,
                    poolRegistry,
                    connectionSchemaService,
                    auditLogService,
                    bulkAccessProperties);

    private final Connection setupConnection;

    /**
     * Instance initializer, not {@code @BeforeEach}: see class Javadoc. Each
     * test instance gets its own uniquely-named in-memory H2 database, never
     * explicitly closed (harmless to leak for the test JVM's lifetime).
     */
    {
        try {
            var dbName = "querytest_" + UUID.randomUUID().toString().replace("-", "");
            var jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
            setupConnection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = setupConnection.createStatement()) {
                statement.execute("CREATE TABLE t1 (id BIGINT PRIMARY KEY, name VARCHAR(50))");
                statement.execute("INSERT INTO t1 VALUES (1, 'Alice')");
                statement.execute("INSERT INTO t1 VALUES (2, 'Bob')");
            }

            var dataSource = new JdbcDataSource();
            dataSource.setUrl(jdbcUrl);
            when(poolRegistry.dataSourceFor(any())).thenReturn(dataSource);
            when(connectionRepository.findById(1L))
                    .thenReturn(
                            Optional.of(new TargetConnection("conn1", RdbmsType.H2, "localhost", 9092, dbName, null, "sa", "enc")));
            when(connectionSchemaService.isSchemaAllowed(1L, "PUBLIC")).thenReturn(true);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // --- saveQuery / retireQuery (US-4.3〜US-4.4) ---

    @Test
    void saveQueryCreatesANewQueryWhenNoIdIsGiven() {
        // Mimics Spring Data JPA's IDENTITY-generation behavior: save() flushes
        // the INSERT immediately and populates the id on the same instance.
        when(savedQueryRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            SavedQuery savedQuery = invocation.getArgument(0);
                            if (savedQuery.getId() == null) {
                                setId(savedQuery, 99L);
                            }
                            return savedQuery;
                        });

        service.saveQuery("q1", "SELECT 1", QueryVisibility.PUBLIC, null, 9L);

        verify(auditLogService)
                .recordEvent(eq(AuditEventType.QUERY_SAVED), eq(9L), isNull(), any());
    }

    @Test
    void saveQueryRejectsAnUpdateByANonCreator() {
        var savedQuery = new SavedQuery("q1", "SELECT 1", QueryVisibility.PUBLIC, 1L);
        setId(savedQuery, 10L);
        when(savedQueryRepository.findById(10L)).thenReturn(Optional.of(savedQuery));

        assertThatThrownBy(() -> service.saveQuery("q2", "SELECT 2", QueryVisibility.PUBLIC, 10L, 2L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_PERMISSION_DENIED");
    }

    @Test
    void retireQueryRejectsANonCreator() {
        var savedQuery = new SavedQuery("q1", "SELECT 1", QueryVisibility.PUBLIC, 1L);
        setId(savedQuery, 10L);
        when(savedQueryRepository.findById(10L)).thenReturn(Optional.of(savedQuery));

        assertThatThrownBy(() -> service.retireQuery(10L, 2L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_PERMISSION_DENIED");
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ" (PBT-01): creator-only edits, for any pair of distinct users. */
    @Property
    void nonCreatorCanNeverUpdateOrRetireASavedQuery(
            @ForAll @LongRange(min = 1, max = 1000) long creatorId, @ForAll @LongRange(min = 1, max = 1000) long callerId) {
        Assume.that(creatorId != callerId);
        var savedQuery = new SavedQuery("q1", "SELECT 1", QueryVisibility.PUBLIC, creatorId);
        setId(savedQuery, 10L);
        when(savedQueryRepository.findById(10L)).thenReturn(Optional.of(savedQuery));

        assertThatThrownBy(() -> service.saveQuery("q2", "SELECT 2", QueryVisibility.PUBLIC, 10L, callerId))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_PERMISSION_DENIED");
        assertThatThrownBy(() -> service.retireQuery(10L, callerId))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_PERMISSION_DENIED");
    }

    // --- detectParameters (US-4.5) ---

    @Test
    void detectParametersFindsEachDistinctPlaceholder() {
        var result = service.detectParameters("SELECT * FROM t1 WHERE id = :id AND name = :name AND id2 = :id");

        assertThat(result).extracting(ParameterDescriptor::name).containsExactly("id", "name");
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ" (PBT-01): every :paramName occurrence is detected. */
    @Property
    void detectParametersAlwaysIncludesEveryNamedPlaceholderInTheSql(
            @ForAll @AlphaChars @StringLength(min = 1, max = 10) String paramName) {
        var result = service.detectParameters("SELECT * FROM t1 WHERE col = :" + paramName);

        assertThat(result).extracting(ParameterDescriptor::name).contains(paramName);
    }

    // --- executeQuery (US-4.1〜US-4.5) ---

    @Test
    void executeQueryReturnsColumnsAndRows() {
        var result = service.executeQuery("SELECT id, name FROM t1 ORDER BY id", null, 1L, "PUBLIC", Map.of(), 9L);

        assertThat(result.columns()).containsExactly("ID", "NAME");
        assertThat(result.rowCount()).isEqualTo(2);
        verify(auditLogService)
                .recordEvent(eq(AuditEventType.QUERY_EXECUTED), eq(9L), isNull(), any());
        verify(historyRepository).save(any());
    }

    @Test
    void executeQueryUsesTheSavedQuerysSqlTextWhenSavedQueryIdIsGiven() {
        var savedQuery = new SavedQuery("q1", "SELECT id, name FROM t1 ORDER BY id", QueryVisibility.PUBLIC, 9L);
        setId(savedQuery, 10L);
        when(savedQueryRepository.findById(10L)).thenReturn(Optional.of(savedQuery));

        var result = service.executeQuery(null, 10L, 1L, "PUBLIC", Map.of(), 9L);

        assertThat(result.rowCount()).isEqualTo(2);
    }

    @Test
    void executeQueryRejectsAPrivateSavedQueryExecutedByANonCreator() {
        var savedQuery = new SavedQuery("q1", "SELECT id FROM t1", QueryVisibility.PRIVATE, 1L);
        setId(savedQuery, 10L);
        when(savedQueryRepository.findById(10L)).thenReturn(Optional.of(savedQuery));

        assertThatThrownBy(() -> service.executeQuery(null, 10L, 1L, "PUBLIC", Map.of(), 2L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_PERMISSION_DENIED");
    }

    @Test
    void executeQueryRejectsANonSelectStatement() {
        assertThatThrownBy(() -> service.executeQuery("DELETE FROM t1", null, 1L, "PUBLIC", Map.of(), 9L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_UNSAFE_SQL");
    }

    @Test
    void executeQueryRejectsASchemaThatIsNotAllowed() {
        when(connectionSchemaService.isSchemaAllowed(1L, "SECRET")).thenReturn(false);

        assertThatThrownBy(() -> service.executeQuery("SELECT 1", null, 1L, "SECRET", Map.of(), 9L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_SCHEMA_NOT_ALLOWED");
    }

    /** functional-design/business-logic-model.md "テスト対象プロパティ" (PBT-01): BR-6/BR-7 always reject stacked statements. */
    @Property
    void anySqlContainingASemicolonIsAlwaysRejected(@ForAll @AlphaChars String prefix, @ForAll @AlphaChars String suffix) {
        var sql = "SELECT " + prefix + ";" + suffix;

        assertThatThrownBy(() -> service.executeQuery(sql, null, 1L, "PUBLIC", Map.of(), 9L))
                .isInstanceOf(QueryException.class)
                .extracting("errorCode")
                .isEqualTo("QUERY_UNSAFE_SQL");
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ" (PBT-01): the
     * bulk-data-access event is recorded exactly when the row count (fixed at
     * 2 for the {@code t1} fixture) meets the threshold, for any threshold.
     *
     * <p>jqwik runs every try of one {@code @Property} method against the same
     * test instance (unlike JUnit Jupiter's per-method lifecycle across
     * different methods), so the shared {@code auditLogService} mock's
     * invocation history must be cleared before each try.
     */
    @Property
    void bulkDataAccessedEventIsRecordedWheneverThresholdIsMet(@ForAll @IntRange(min = 1, max = 10) int threshold) {
        clearInvocations(auditLogService);
        var svc =
                new QueryServiceImpl(
                        savedQueryRepository,
                        historyRepository,
                        connectionRepository,
                        poolRegistry,
                        connectionSchemaService,
                        auditLogService,
                        new BulkAccessProperties(threshold));

        var result = svc.executeQuery("SELECT id FROM t1", null, 1L, "PUBLIC", Map.of(), 9L);

        if (threshold <= result.rowCount()) {
            verify(auditLogService).recordEvent(eq(AuditEventType.BULK_DATA_ACCESSED), eq(9L), isNull(), any());
        } else {
            verify(auditLogService, never())
                    .recordEvent(eq(AuditEventType.BULK_DATA_ACCESSED), any(), any(), any());
        }
    }

    // --- listExecutionHistory (US-4.6) ---

    @Test
    void listExecutionHistoryDelegatesToTheRepositorySearchQuery() {
        service.listExecutionHistory(
                new ExecutionHistoryFilterCriteria(9L, 1L, "PUBLIC", null, null, null), PageRequest.of(0, 50));

        verify(historyRepository).search(eq(9L), eq(1L), eq("PUBLIC"), isNull(), isNull(), isNull(), any());
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
