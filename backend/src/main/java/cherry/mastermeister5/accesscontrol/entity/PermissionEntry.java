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

package cherry.mastermeister5.accesscontrol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * domain-entities.md PermissionEntry (Functional Design Question 2).
 * business-rules.md BR-4/BR-5/BR-6: connectionId+schemaName/tableName/
 * columnName are the natural key (not DbSchema/DbTable/DbColumn ids), and
 * tableName/columnName are null above their resourceLevel. BR-7's
 * uniqueness (subjectType,subjectId,connectionId,resourceLevel,schemaName,
 * tableName,columnName) is enforced at the service layer (upsert lookup)
 * rather than a DB constraint, because ANSI unique constraints treat NULL
 * columns (tableName/columnName at SCHEMA/TABLE level) as distinct.
 */
@Entity
@Table(name = "permission_entry")
public class PermissionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubjectType subjectType;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long connectionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceLevel resourceLevel;

    @Column(nullable = false)
    private String schemaName;

    private String tableName;

    private String columnName;

    @Enumerated(EnumType.STRING)
    private PrimaryLevel primaryLevel;

    private Boolean auxCreate;

    private Boolean auxDelete;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PermissionEntry() {
    }

    public PermissionEntry(
            SubjectType subjectType,
            Long subjectId,
            Long connectionId,
            ResourceLevel resourceLevel,
            String schemaName,
            String tableName,
            String columnName) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.connectionId = connectionId;
        this.resourceLevel = resourceLevel;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.columnName = columnName;
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public ResourceLevel getResourceLevel() {
        return resourceLevel;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public PrimaryLevel getPrimaryLevel() {
        return primaryLevel;
    }

    public void setPrimaryLevel(PrimaryLevel primaryLevel) {
        this.primaryLevel = primaryLevel;
        touch();
    }

    public Boolean getAuxCreate() {
        return auxCreate;
    }

    public Boolean getAuxDelete() {
        return auxDelete;
    }

    public void setAuxiliary(Boolean auxCreate, Boolean auxDelete) {
        this.auxCreate = auxCreate;
        this.auxDelete = auxDelete;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
