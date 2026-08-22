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

package cherry.mastermeister5.query.entity;

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
 * domain-entities.md (Unit 6) SavedQuery. {@code sqlText} is schema-unqualified
 * (BR-9: schema is applied at execution time via {@code Connection#setSchema}).
 */
@Entity
@Table(name = "saved_query")
public class SavedQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String sqlText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryVisibility visibility;

    @Column(nullable = false)
    private Long creatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SavedQuery() {
    }

    public SavedQuery(String name, String sqlText, QueryVisibility visibility, Long creatorUserId) {
        this.name = name;
        this.sqlText = sqlText;
        this.visibility = visibility;
        this.creatorUserId = creatorUserId;
        this.status = QueryStatus.ACTIVE;
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** BR-3: caller must have already verified {@code creatorUserId} ownership. */
    public void update(String name, String sqlText, QueryVisibility visibility) {
        this.name = name;
        this.sqlText = sqlText;
        this.visibility = visibility;
        this.updatedAt = Instant.now();
    }

    /** BR-2: logical hide only, never a physical delete. */
    public void retire() {
        this.status = QueryStatus.RETIRED;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSqlText() {
        return sqlText;
    }

    public QueryVisibility getVisibility() {
        return visibility;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }

    public QueryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
