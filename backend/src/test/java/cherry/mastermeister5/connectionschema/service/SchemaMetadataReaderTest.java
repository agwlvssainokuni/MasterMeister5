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

import cherry.mastermeister5.connectionschema.entity.DbTable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link SchemaMetadataReader} against a real H2 database (JDBC
 * standard {@code DatabaseMetaData}, business-rules.md BR-12) rather than
 * mocks, since the whole point of this class is to be correct against an
 * actual driver's metadata implementation.
 */
class SchemaMetadataReaderTest {

    private final SchemaMetadataReader reader = new SchemaMetadataReader();
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        var dbName = "schemareadertest_" + UUID.randomUUID().toString().replace("-", "");
        connection = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "CREATE TABLE parent_table (id BIGINT PRIMARY KEY, name VARCHAR(50) NOT NULL)");
            statement.execute(
                    "CREATE TABLE child_table ("
                            + "id BIGINT PRIMARY KEY, "
                            + "parent_id BIGINT, "
                            + "FOREIGN KEY (parent_id) REFERENCES parent_table(id))");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void discoversTheDefaultSchemaWhenNoHintIsGiven() throws Exception {
        var schemas = reader.readSchemas(connection, null);

        assertThat(schemas).anySatisfy(s -> assertThat(s.schemaName()).isEqualToIgnoringCase("PUBLIC"));
    }

    @Test
    void readsTablesColumnsAndPrimaryKeys() throws Exception {
        var schemas = reader.readSchemas(connection, "PUBLIC");

        assertThat(schemas).hasSize(1);
        var tables = schemas.get(0).tables();
        var parent = tables.stream().filter(t -> t.tableName().equalsIgnoreCase("parent_table")).findFirst();
        assertThat(parent).isPresent();
        assertThat(parent.get().tableType()).isEqualTo(DbTable.Type.TABLE);
        var idColumn =
                parent.get().columns().stream()
                        .filter(c -> c.columnName().equalsIgnoreCase("id"))
                        .findFirst()
                        .orElseThrow();
        assertThat(idColumn.primaryKey()).isTrue();
        var nameColumn =
                parent.get().columns().stream()
                        .filter(c -> c.columnName().equalsIgnoreCase("name"))
                        .findFirst()
                        .orElseThrow();
        assertThat(nameColumn.nullable()).isFalse();
    }

    @Test
    void readsForeignKeys() throws Exception {
        var schemas = reader.readSchemas(connection, "PUBLIC");

        var foreignKeys = schemas.get(0).foreignKeys();
        assertThat(foreignKeys)
                .anySatisfy(
                        fk -> {
                            assertThat(fk.fromTableName()).isEqualToIgnoringCase("child_table");
                            assertThat(fk.fromColumnName()).isEqualToIgnoringCase("parent_id");
                            assertThat(fk.toTableName()).isEqualToIgnoringCase("parent_table");
                        });
    }
}
