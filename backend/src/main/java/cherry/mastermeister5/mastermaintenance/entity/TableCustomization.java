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

package cherry.mastermeister5.mastermaintenance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * domain-entities.md TableCustomization (Functional Design Question 4):
 * addressed by connectionId + schemaName/tableName strings, not
 * {@code DbTable} ids, for the same reason as Unit 3/4's naming scheme.
 */
@Entity
@Table(name = "table_customization")
public class TableCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long connectionId;

    @Column(nullable = false)
    private String schemaName;

    @Column(nullable = false)
    private String tableName;

    private String defaultSortColumn;

    @Enumerated(EnumType.STRING)
    private SortDirection defaultSortDirection;

    protected TableCustomization() {
    }

    public TableCustomization(Long connectionId, String schemaName, String tableName) {
        this.connectionId = connectionId;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public Long getId() {
        return id;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDefaultSortColumn() {
        return defaultSortColumn;
    }

    public SortDirection getDefaultSortDirection() {
        return defaultSortDirection;
    }

    public void setDefaultSort(String defaultSortColumn, SortDirection defaultSortDirection) {
        this.defaultSortColumn = defaultSortColumn;
        this.defaultSortDirection = defaultSortDirection;
    }
}
