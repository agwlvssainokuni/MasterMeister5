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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * domain-entities.md Question 4: only PK/FK/NOT NULL constraints are
 * imported; UNIQUE/CHECK/default values are out of scope.
 */
@Entity
@Table(name = "db_column")
public class DbColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tableId;

    @Column(nullable = false)
    private String columnName;

    @Column(nullable = false)
    private int ordinalPosition;

    @Column(nullable = false)
    private String dataType;

    @Column(nullable = false)
    private boolean nullable;

    @Column(nullable = false)
    private boolean primaryKey;

    private String comment;

    protected DbColumn() {
    }

    public DbColumn(
            Long tableId,
            String columnName,
            int ordinalPosition,
            String dataType,
            boolean nullable,
            boolean primaryKey,
            String comment) {
        this.tableId = tableId;
        this.columnName = columnName;
        this.ordinalPosition = ordinalPosition;
        this.dataType = dataType;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public Long getTableId() {
        return tableId;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public String getComment() {
        return comment;
    }
}
