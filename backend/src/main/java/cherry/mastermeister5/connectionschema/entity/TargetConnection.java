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
import java.time.Instant;

/**
 * domain-entities.md: named {@code TargetConnection} (not {@code Connection}) to
 * avoid shadowing {@link java.sql.Connection}, which this Unit's JDBC code
 * uses extensively (Code Generation Plan決定事項).
 */
@Entity
@Table(name = "connection")
public class TargetConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RdbmsType rdbmsType;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String databaseName;

    private String schemaNameHint;

    /** Appended verbatim to the JDBC URL (e.g. "?useSSL=true&serverTimezone=UTC" for MySQL, ";MODE=MySQL" for H2). */
    private String extraParams;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String encryptedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TargetConnection() {
    }

    public TargetConnection(
            String name,
            RdbmsType rdbmsType,
            String host,
            int port,
            String databaseName,
            String schemaNameHint,
            String extraParams,
            String username,
            String encryptedPassword) {
        this.name = name;
        this.rdbmsType = rdbmsType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.schemaNameHint = schemaNameHint;
        this.extraParams = extraParams;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.status = ConnectionStatus.ACTIVE;
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RdbmsType getRdbmsType() {
        return rdbmsType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaNameHint() {
        return schemaNameHint;
    }

    public String getExtraParams() {
        return extraParams;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
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
