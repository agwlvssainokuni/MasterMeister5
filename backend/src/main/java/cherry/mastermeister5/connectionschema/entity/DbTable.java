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

package cherry.mastermeister5.connectionschema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** physical table name {@code db_table} avoids the SQL reserved word {@code table}. */
@Entity
@Table(name = "db_table")
public class DbTable {

    public enum Type {
        TABLE,
        VIEW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long schemaId;

    @Column(nullable = false)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type tableType;

    private String comment;

    protected DbTable() {
    }

    public DbTable(Long schemaId, String tableName, Type tableType, String comment) {
        this.schemaId = schemaId;
        this.tableName = tableName;
        this.tableType = tableType;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public Long getSchemaId() {
        return schemaId;
    }

    public String getTableName() {
        return tableName;
    }

    public Type getTableType() {
        return tableType;
    }

    public String getComment() {
        return comment;
    }
}
