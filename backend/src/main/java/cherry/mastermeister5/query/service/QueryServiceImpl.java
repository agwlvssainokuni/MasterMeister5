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

import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository;
import cherry.mastermeister5.connectionschema.service.ConnectionPoolRegistry;
import cherry.mastermeister5.connectionschema.service.ConnectionSchemaService;
import cherry.mastermeister5.platform.BulkAccessProperties;
import cherry.mastermeister5.query.entity.QueryExecutionHistory;
import cherry.mastermeister5.query.entity.QueryStatus;
import cherry.mastermeister5.query.entity.QueryVisibility;
import cherry.mastermeister5.query.entity.SavedQuery;
import cherry.mastermeister5.query.repository.QueryExecutionHistoryJpaRepository;
import cherry.mastermeister5.query.repository.SavedQueryJpaRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * business-logic-model.md / business-rules.md (BR-1〜BR-19, Unit 6).
 */
@Service
class QueryServiceImpl implements QueryService {

    /** nfr-requirements.md Question 1: result set capped at 1,000 rows. */
    private static final int MAX_ROWS = 1000;

    /** nfr-requirements.md Question 1: execution timeout of 30 seconds. */
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    /** business-rules.md BR-7: same blocklist as Unit 5's raw WHERE/ORDER BY validation. */
    private static final Pattern UNSAFE_SQL_PATTERN = Pattern.compile(";|--|/\\*");

    /** business-rules.md BR-6: comment-stripped SQL must start with SELECT or WITH. */
    private static final Pattern READ_ONLY_PREFIX_PATTERN = Pattern.compile("^\\s*(SELECT|WITH)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * business-logic-model.md Section 4 / Functional Design Question 4: detects
     * {@code :paramName} placeholders using the same naming syntax
     * {@link NamedParameterJdbcTemplate} itself recognizes. Spring's own
     * {@code ParsedSql#getParameterNames()} (which the JDBC template uses
     * internally) is package-private and not reachable from application code,
     * so this regex is the actual name-detection step; execution/binding still
     * goes entirely through {@link NamedParameterJdbcTemplate} +
     * {@link MapSqlParameterSource} (built on Spring's named-parameter support),
     * never a custom SQL rewrite.
     */
    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile("(?<!:):([A-Za-z][A-Za-z0-9_]*)");

    private final SavedQueryJpaRepository savedQueryRepository;
    private final QueryExecutionHistoryJpaRepository historyRepository;
    private final TargetConnectionJpaRepository connectionRepository;
    private final ConnectionPoolRegistry poolRegistry;
    private final ConnectionSchemaService connectionSchemaService;
    private final AuditLogService auditLogService;
    private final BulkAccessProperties bulkAccessProperties;

    QueryServiceImpl(
            SavedQueryJpaRepository savedQueryRepository,
            QueryExecutionHistoryJpaRepository historyRepository,
            TargetConnectionJpaRepository connectionRepository,
            ConnectionPoolRegistry poolRegistry,
            ConnectionSchemaService connectionSchemaService,
            AuditLogService auditLogService,
            BulkAccessProperties bulkAccessProperties) {
        this.savedQueryRepository = savedQueryRepository;
        this.historyRepository = historyRepository;
        this.connectionRepository = connectionRepository;
        this.poolRegistry = poolRegistry;
        this.connectionSchemaService = connectionSchemaService;
        this.auditLogService = auditLogService;
        this.bulkAccessProperties = bulkAccessProperties;
    }

    // --- saved queries (US-4.3〜US-4.4) ---

    @Override
    @Transactional(readOnly = true)
    public List<SavedQuery> listSavedQueries(Long userId) {
        return savedQueryRepository.findVisibleTo(QueryStatus.ACTIVE, userId);
    }

    @Override
    @Transactional
    public Long saveQuery(String name, String sqlText, QueryVisibility visibility, Long savedQueryId, Long actorUserId) {
        var isUpdate = savedQueryId != null;
        SavedQuery savedQuery;
        if (isUpdate) {
            savedQuery = savedQueryRepository.findById(savedQueryId).orElseThrow(QueryException::notFound);
            if (!savedQuery.getCreatorUserId().equals(actorUserId)) {
                throw QueryException.permissionDenied();
            }
            savedQuery.update(name, sqlText, visibility);
        } else {
            savedQuery = new SavedQuery(name, sqlText, visibility, actorUserId);
        }
        savedQuery = savedQueryRepository.save(savedQuery);

        auditLogService.recordEvent(
                AuditEventType.QUERY_SAVED,
                actorUserId,
                null,
                Map.of("savedQueryId", savedQuery.getId(), "isUpdate", isUpdate));
        return savedQuery.getId();
    }

    @Override
    @Transactional
    public void retireQuery(Long savedQueryId, Long actorUserId) {
        var savedQuery = savedQueryRepository.findById(savedQueryId).orElseThrow(QueryException::notFound);
        if (!savedQuery.getCreatorUserId().equals(actorUserId)) {
            throw QueryException.permissionDenied();
        }
        savedQuery.retire();
        savedQueryRepository.save(savedQuery);

        auditLogService.recordEvent(
                AuditEventType.QUERY_RETIRED, actorUserId, null, Map.of("savedQueryId", savedQueryId));
    }

    // --- parameter detection (US-4.5) ---

    @Override
    public List<ParameterDescriptor> detectParameters(String sqlText) {
        var matcher = NAMED_PARAMETER_PATTERN.matcher(sqlText);
        var names = new LinkedHashSet<String>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names.stream().map(ParameterDescriptor::new).toList();
    }

    // --- query execution (US-4.1〜US-4.5) ---

    @Override
    @Transactional
    public QueryResult executeQuery(
            String sqlText, Long savedQueryId, Long connectionId, String schemaName, Map<String, Object> params, Long actorUserId) {
        var effectiveSql = resolveSqlText(sqlText, savedQueryId, actorUserId);
        validateReadOnlySql(effectiveSql);

        if (!connectionSchemaService.isSchemaAllowed(connectionId, schemaName)) {
            throw QueryException.schemaNotAllowed();
        }
        var connection = connectionRepository.findById(connectionId).orElseThrow(QueryException::notFound);

        var startTime = System.currentTimeMillis();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows;
        try (var rawConnection = poolRegistry.dataSourceFor(connection).getConnection()) {
            var originalReadOnly = rawConnection.isReadOnly();
            try {
                // nfr-design-patterns.md Question 2: JDBC-level defense in depth
                // alongside the blocklist, plus BR-9's schema application.
                rawConnection.setReadOnly(true);
                rawConnection.setSchema(schemaName);
                var singleConnectionDataSource = new SingleConnectionDataSource(rawConnection, true);
                var jdbcTemplate = new NamedParameterJdbcTemplate(singleConnectionDataSource);
                jdbcTemplate.getJdbcTemplate().setMaxRows(MAX_ROWS);
                jdbcTemplate.getJdbcTemplate().setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                var paramSource = new MapSqlParameterSource(params != null ? params : Map.of());
                rows =
                        jdbcTemplate.query(
                                effectiveSql,
                                paramSource,
                                rs -> {
                                    var metaData = rs.getMetaData();
                                    for (var i = 1; i <= metaData.getColumnCount(); i++) {
                                        columns.add(metaData.getColumnLabel(i));
                                    }
                                    var result = new ArrayList<Map<String, Object>>();
                                    while (rs.next()) {
                                        Map<String, Object> row = new LinkedHashMap<>();
                                        for (var columnName : columns) {
                                            row.put(columnName, rs.getObject(columnName));
                                        }
                                        result.add(row);
                                    }
                                    return result;
                                });
            } finally {
                rawConnection.setReadOnly(originalReadOnly);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        var executionTimeMs = System.currentTimeMillis() - startTime;
        var rowCount = rows.size();

        historyRepository.save(
                new QueryExecutionHistory(
                        savedQueryId, effectiveSql, connectionId, schemaName, params, rowCount, executionTimeMs, actorUserId));

        // business-rules.md BR-13: always recorded.
        auditLogService.recordEvent(
                AuditEventType.QUERY_EXECUTED,
                actorUserId,
                null,
                Map.of(
                        "connectionId", connectionId,
                        "schemaName", schemaName,
                        "resultRowCount", rowCount,
                        "executionTimeMs", executionTimeMs));

        // business-rules.md BR-14/BR-15: additional event when the threshold is met.
        if (rowCount >= bulkAccessProperties.threshold()) {
            auditLogService.recordEvent(
                    AuditEventType.BULK_DATA_ACCESSED,
                    actorUserId,
                    null,
                    Map.of(
                            "source", "query",
                            "connectionId", connectionId,
                            "schemaName", schemaName,
                            "resultRowCount", rowCount));
        }

        return new QueryResult(columns, rows, rowCount, executionTimeMs);
    }

    private String resolveSqlText(String sqlText, Long savedQueryId, Long actorUserId) {
        if (savedQueryId != null) {
            var savedQuery = savedQueryRepository.findById(savedQueryId).orElseThrow(QueryException::notFound);
            // business-rules.md BR-4: a PRIVATE query cannot be executed by anyone but its creator.
            if (savedQuery.getVisibility() == QueryVisibility.PRIVATE && !savedQuery.getCreatorUserId().equals(actorUserId)) {
                throw QueryException.permissionDenied();
            }
            return savedQuery.getSqlText();
        }
        if (sqlText == null || sqlText.isBlank()) {
            throw QueryException.invalidRequest();
        }
        return sqlText;
    }

    private void validateReadOnlySql(String sqlText) {
        if (UNSAFE_SQL_PATTERN.matcher(sqlText).find()) {
            throw QueryException.unsafeSql();
        }
        if (!READ_ONLY_PREFIX_PATTERN.matcher(sqlText).find()) {
            throw QueryException.unsafeSql();
        }
    }

    // --- execution history (US-4.6) ---

    @Override
    @Transactional(readOnly = true)
    public Page<QueryExecutionHistory> listExecutionHistory(ExecutionHistoryFilterCriteria filterCriteria, Pageable pageable) {
        return historyRepository.search(
                filterCriteria.executedByUserId(),
                filterCriteria.connectionId(),
                filterCriteria.schemaName(),
                filterCriteria.sqlTextContains(),
                filterCriteria.fromDate(),
                filterCriteria.toDate(),
                pageable);
    }
}
