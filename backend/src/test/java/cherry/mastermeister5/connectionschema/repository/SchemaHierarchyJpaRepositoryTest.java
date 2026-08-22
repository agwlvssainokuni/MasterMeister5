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

package cherry.mastermeister5.connectionschema.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cherry.mastermeister5.connectionschema.entity.DbColumn;
import cherry.mastermeister5.connectionschema.entity.DbSchema;
import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.connectionschema.entity.ForeignKeyConstraint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;

/**
 * Covers DbSchema/DbTable/DbColumn/ForeignKeyConstraint together since they
 * form a single owned hierarchy (Connection -&gt; Schema -&gt; Table -&gt;
 * Column/ForeignKeyConstraint, domain-entities.md) that Unit 3's
 * full-replace logic manipulates as a unit.
 */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class SchemaHierarchyJpaRepositoryTest {

    @Autowired private DbSchemaJpaRepository schemaRepository;
    @Autowired private DbTableJpaRepository tableRepository;
    @Autowired private DbColumnJpaRepository columnRepository;
    @Autowired private ForeignKeyConstraintJpaRepository foreignKeyRepository;

    @Test
    void findByConnectionIdAndSchemaNameReturnsTheMatchingSchema() {
        schemaRepository.save(new DbSchema(1L, "public"));

        assertThat(schemaRepository.findByConnectionIdAndSchemaName(1L, "public")).isPresent();
        assertThat(schemaRepository.findByConnectionIdAndSchemaName(1L, "other")).isEmpty();
        assertThat(schemaRepository.findAllByConnectionId(1L)).hasSize(1);
    }

    @Test
    void deleteAllBySchemaIdRemovesOnlyThatSchemasTables() {
        var schema = schemaRepository.save(new DbSchema(1L, "public"));
        var otherSchema = schemaRepository.save(new DbSchema(1L, "other"));
        tableRepository.save(new DbTable(schema.getId(), "t1", DbTable.Type.TABLE, null));
        var keep = tableRepository.save(new DbTable(otherSchema.getId(), "t2", DbTable.Type.TABLE, null));

        tableRepository.deleteAllBySchemaId(schema.getId());

        assertThat(tableRepository.findAllBySchemaId(schema.getId())).isEmpty();
        assertThat(tableRepository.findById(keep.getId())).isPresent();
    }

    @Test
    void deleteAllByTableIdInRemovesColumnsAndForeignKeysForThoseTablesOnly() {
        var schema = schemaRepository.save(new DbSchema(1L, "public"));
        var table1 = tableRepository.save(new DbTable(schema.getId(), "t1", DbTable.Type.TABLE, null));
        var table2 = tableRepository.save(new DbTable(schema.getId(), "t2", DbTable.Type.TABLE, null));
        columnRepository.save(new DbColumn(table1.getId(), "id", 1, "BIGINT", false, true, null));
        var keptColumn = columnRepository.save(new DbColumn(table2.getId(), "id", 1, "BIGINT", false, true, null));
        foreignKeyRepository.save(new ForeignKeyConstraint(table1.getId(), "parent_id", table2.getId(), "id"));

        columnRepository.deleteAllByTableIdIn(java.util.List.of(table1.getId()));
        foreignKeyRepository.deleteAllByFromTableIdIn(java.util.List.of(table1.getId()));

        assertThat(columnRepository.findAllByTableId(table1.getId())).isEmpty();
        assertThat(columnRepository.findById(keptColumn.getId())).isPresent();
    }
}
