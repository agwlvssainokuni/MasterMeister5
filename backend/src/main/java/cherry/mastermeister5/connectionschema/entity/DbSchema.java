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
import java.time.Instant;

/**
 * domain-entities.md: named {@code DbSchema} (not {@code Schema}) — the
 * physical table name {@code db_schema} avoids the SQL reserved word
 * {@code schema} (Infrastructure Design Question 1). Existence of a row here
 * IS the "allowed schema list" (Functional Design Question 3).
 */
@Entity
@Table(name = "db_schema")
public class DbSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long connectionId;

    @Column(nullable = false)
    private String schemaName;

    @Column(nullable = false)
    private Instant importedAt;

    protected DbSchema() {
    }

    public DbSchema(Long connectionId, String schemaName) {
        this.connectionId = connectionId;
        this.schemaName = schemaName;
        this.importedAt = Instant.now();
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

    public Instant getImportedAt() {
        return importedAt;
    }

    public void markImported() {
        this.importedAt = Instant.now();
    }
}
