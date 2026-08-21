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

package cherry.mastermeister5.useraccount.entity;

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
 * domain-entities.md: invited-but-not-registered and fully registered users
 * are the same row, distinguished by {@link #status} (Functional Design
 * Question 1 = A).
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    private String passwordHash;

    private String invitationTokenHash;

    private Instant invitationTokenExpiresAt;

    private Instant invitedAt;

    private Long invitedBy;

    private Instant registeredAt;

    @Column(nullable = false)
    private int failedLoginCount;

    private Instant lockedUntil;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String email, UserRole role) {
        this.email = email;
        this.role = role;
        this.status = UserStatus.INVITED;
        this.failedLoginCount = 0;
        var now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
        touch();
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
        touch();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        touch();
    }

    public String getInvitationTokenHash() {
        return invitationTokenHash;
    }

    public Instant getInvitationTokenExpiresAt() {
        return invitationTokenExpiresAt;
    }

    public void setInvitationToken(String invitationTokenHash, Instant expiresAt) {
        this.invitationTokenHash = invitationTokenHash;
        this.invitationTokenExpiresAt = expiresAt;
        this.invitedAt = Instant.now();
        touch();
    }

    public void clearInvitationToken() {
        this.invitationTokenHash = null;
        this.invitationTokenExpiresAt = null;
        touch();
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
        touch();
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void incrementFailedLoginCount() {
        this.failedLoginCount++;
        touch();
    }

    public void resetFailedLoginCount() {
        this.failedLoginCount = 0;
        touch();
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
        touch();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
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
