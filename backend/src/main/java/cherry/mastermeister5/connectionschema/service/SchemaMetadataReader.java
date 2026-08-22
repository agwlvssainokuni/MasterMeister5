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

import cherry.mastermeister5.connectionschema.entity.DbTable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * business-rules.md BR-12: JDBC standard {@link DatabaseMetaData} only, no
 * RDBMS-specific {@code INFORMATION_SCHEMA} queries (nfr-requirements-plan.md
 * Question 5). Only PK/FK/NOT NULL constraints are read (Functional Design
 * Question 4).
 */
@Component
public class SchemaMetadataReader {

    private static final Set<String> SYSTEM_SCHEMAS =
            Set.of(
                    "information_schema",
                    "pg_catalog",
                    "pg_toast",
                    "mysql",
                    "performance_schema",
                    "sys");

    /**
     * @param schemaNameHint when present, restricts discovery to exactly this schema
     *     (Functional Design Question 2 addendum); when absent, every schema the
     *     connected user can see is discovered (system schemas excluded)
     */
    public List<DiscoveredSchema> readSchemas(Connection connection, String schemaNameHint)
            throws SQLException {
        var metaData = connection.getMetaData();
        List<String> schemaNames;
        boolean useSchemaPattern;

        if (schemaNameHint != null && !schemaNameHint.isBlank()) {
            schemaNames = List.of(schemaNameHint);
            useSchemaPattern = schemaExists(metaData, schemaNameHint);
        } else {
            var discovered = new ArrayList<String>();
            try (var rs = metaData.getSchemas()) {
                while (rs.next()) {
                    var name = rs.getString("TABLE_SCHEM");
                    if (name != null && !SYSTEM_SCHEMAS.contains(name.toLowerCase())) {
                        discovered.add(name);
                    }
                }
            }
            if (discovered.isEmpty()) {
                // MySQL/MariaDB: no distinct schema concept, catalog IS the schema.
                schemaNames = List.of(connection.getCatalog());
                useSchemaPattern = false;
            } else {
                schemaNames = discovered;
                useSchemaPattern = true;
            }
        }

        var result = new ArrayList<DiscoveredSchema>();
        for (var schemaName : schemaNames) {
            var catalog = useSchemaPattern ? connection.getCatalog() : schemaName;
            var schemaPattern = useSchemaPattern ? schemaName : null;
            result.add(readSchema(metaData, schemaName, catalog, schemaPattern));
        }
        return result;
    }

    private boolean schemaExists(DatabaseMetaData metaData, String schemaName) throws SQLException {
        try (var rs = metaData.getSchemas()) {
            while (rs.next()) {
                if (schemaName.equalsIgnoreCase(rs.getString("TABLE_SCHEM"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private DiscoveredSchema readSchema(
            DatabaseMetaData metaData, String schemaName, String catalog, String schemaPattern)
            throws SQLException {
        var tables = new ArrayList<DiscoveredTable>();
        var tableNames = new ArrayList<String>();
        try (var rs = metaData.getTables(catalog, schemaPattern, "%", new String[] {"TABLE", "VIEW"})) {
            while (rs.next()) {
                var tableName = rs.getString("TABLE_NAME");
                var tableType =
                        "VIEW".equalsIgnoreCase(rs.getString("TABLE_TYPE"))
                                ? DbTable.Type.VIEW
                                : DbTable.Type.TABLE;
                var comment = rs.getString("REMARKS");
                tableNames.add(tableName);
                tables.add(
                        new DiscoveredTable(
                                tableName, tableType, comment, readColumns(metaData, catalog, schemaPattern, tableName)));
            }
        }

        var foreignKeys = new ArrayList<DiscoveredForeignKey>();
        for (var tableName : tableNames) {
            foreignKeys.addAll(readForeignKeys(metaData, catalog, schemaPattern, tableName));
        }

        return new DiscoveredSchema(schemaName, tables, foreignKeys);
    }

    private List<DiscoveredColumn> readColumns(
            DatabaseMetaData metaData, String catalog, String schemaPattern, String tableName)
            throws SQLException {
        var primaryKeyColumns = new HashSet<String>();
        try (var rs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
            while (rs.next()) {
                primaryKeyColumns.add(rs.getString("COLUMN_NAME"));
            }
        }

        var columns = new ArrayList<DiscoveredColumn>();
        try (var rs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
            while (rs.next()) {
                var columnName = rs.getString("COLUMN_NAME");
                columns.add(
                        new DiscoveredColumn(
                                columnName,
                                rs.getInt("ORDINAL_POSITION"),
                                rs.getString("TYPE_NAME"),
                                rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                                primaryKeyColumns.contains(columnName),
                                rs.getString("REMARKS")));
            }
        }
        return columns;
    }

    private List<DiscoveredForeignKey> readForeignKeys(
            DatabaseMetaData metaData, String catalog, String schemaPattern, String tableName)
            throws SQLException {
        var foreignKeys = new ArrayList<DiscoveredForeignKey>();
        try (var rs = metaData.getImportedKeys(catalog, schemaPattern, tableName)) {
            while (rs.next()) {
                foreignKeys.add(
                        new DiscoveredForeignKey(
                                rs.getString("FKTABLE_NAME"),
                                rs.getString("FKCOLUMN_NAME"),
                                rs.getString("PKTABLE_NAME"),
                                rs.getString("PKCOLUMN_NAME")));
            }
        }
        return foreignKeys;
    }
}
