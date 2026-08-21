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

package cherry.mastermeister5.useraccount.service;

import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.platform.AppProperties;
import cherry.mastermeister5.platform.security.PasswordHasher;
import cherry.mastermeister5.platform.security.BreachedPasswordChecker;
import cherry.mastermeister5.platform.security.RefreshTokenService;
import cherry.mastermeister5.platform.security.SecureTokenGenerator;
import cherry.mastermeister5.notification.NotificationService;
import cherry.mastermeister5.useraccount.UserAccountProperties;
import cherry.mastermeister5.useraccount.entity.PasswordResetToken;
import cherry.mastermeister5.useraccount.entity.User;
import cherry.mastermeister5.useraccount.entity.UserRole;
import cherry.mastermeister5.useraccount.entity.UserStatus;
import cherry.mastermeister5.useraccount.repository.PasswordResetTokenJpaRepository;
import cherry.mastermeister5.useraccount.repository.UserJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * business-logic-model.md / business-rules.md (BR-1〜BR-33). Timing-attack
 * resistance for "unknown email" login attempts is out of scope
 * (requirements.md does not call for it).
 */
@Service
class UserAccountServiceImpl implements UserAccountService {

    private final UserJpaRepository userRepository;
    private final PasswordResetTokenJpaRepository passwordResetTokenRepository;
    private final PasswordHasher passwordHasher;
    private final BreachedPasswordChecker breachedPasswordChecker;
    private final SecureTokenGenerator tokenGenerator;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;
    private final UserAccountProperties properties;
    private final AppProperties appProperties;

    UserAccountServiceImpl(
            UserJpaRepository userRepository,
            PasswordResetTokenJpaRepository passwordResetTokenRepository,
            PasswordHasher passwordHasher,
            BreachedPasswordChecker breachedPasswordChecker,
            SecureTokenGenerator tokenGenerator,
            NotificationService notificationService,
            AuditLogService auditLogService,
            RefreshTokenService refreshTokenService,
            UserAccountProperties properties,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordHasher = passwordHasher;
        this.breachedPasswordChecker = breachedPasswordChecker;
        this.tokenGenerator = tokenGenerator;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
        this.properties = properties;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public Long inviteUser(String email, UserRole role, Long invitedByUserId, Locale locale) {
        userRepository
                .findByEmail(email)
                .ifPresent(
                        existing -> {
                            if (existing.getStatus() == UserStatus.INVITED) {
                                throw UserAccountException.invitationAlreadyPending();
                            }
                            throw UserAccountException.emailAlreadyRegistered();
                        });

        var user = new User(email, role);
        user.setInvitedBy(invitedByUserId);
        userRepository.save(user);
        issueInvitationAndSend(user, locale);

        auditLogService.recordEvent(
                AuditEventType.USER_INVITED, invitedByUserId, user.getId(), Map.of("email", email));
        return user.getId();
    }

    @Override
    @Transactional
    public void resendInvitation(Long userId, Locale locale) {
        var user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.INVITED) {
            throw UserAccountException.invitationNotPending();
        }
        issueInvitationAndSend(user, locale);
        auditLogService.recordEvent(
                AuditEventType.INVITATION_RESENT, null, user.getId(), Map.of("email", user.getEmail()));
    }

    private void issueInvitationAndSend(User user, Locale locale) {
        var rawToken = tokenGenerator.generate();
        var ttlHours = properties.invitationTokenTtlHours();
        user.setInvitationToken(
                tokenGenerator.hash(rawToken), Instant.now().plus(Duration.ofHours(ttlHours)));
        userRepository.save(user);
        var link = appProperties.baseUrl() + "/register/" + rawToken;
        notificationService.sendInvitationEmail(user.getEmail(), locale, link, ttlHours);
    }

    @Override
    @Transactional
    public Long completeRegistration(String invitationToken, String name, String rawPassword) {
        var tokenHash = tokenGenerator.hash(invitationToken);
        var user =
                userRepository
                        .findByInvitationTokenHash(tokenHash)
                        .filter(u -> u.getStatus() == UserStatus.INVITED)
                        .orElseThrow(UserAccountException::invalidToken);
        if (user.getInvitationTokenExpiresAt() == null
                || user.getInvitationTokenExpiresAt().isBefore(Instant.now())) {
            throw UserAccountException.invitationTokenExpired();
        }
        validatePassword(rawPassword);

        user.setName(name);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setRegisteredAt(Instant.now());
        user.clearInvitationToken();
        userRepository.save(user);

        auditLogService.recordEvent(AuditEventType.USER_REGISTERED, null, user.getId(), Map.of());
        return user.getId();
    }

    @Override
    @Transactional
    public void changeRole(Long userId, UserRole role, Long actorUserId) {
        var user = findUserOrThrow(userId);
        var previousRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        auditLogService.recordEvent(
                AuditEventType.USER_ROLE_CHANGED,
                actorUserId,
                userId,
                Map.of("from", previousRole.name(), "to", role.name()));
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId, Long actorUserId) {
        var user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw UserAccountException.userNotActive();
        }
        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
        auditLogService.recordEvent(AuditEventType.USER_DEACTIVATED, actorUserId, userId, Map.of());
    }

    @Override
    @Transactional
    public void reactivateUser(Long userId, Long actorUserId) {
        var user = findUserOrThrow(userId);
        if (user.getStatus() != UserStatus.DEACTIVATED) {
            throw UserAccountException.userNotDeactivated();
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditLogService.recordEvent(AuditEventType.USER_REACTIVATED, actorUserId, userId, Map.of());
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String email, String rawPassword) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            auditLogService.recordEvent(
                    AuditEventType.LOGIN_FAILED, null, null, Map.of("email", email));
            throw UserAccountException.authenticationFailed();
        }
        var user = userOpt.get();
        var now = Instant.now();
        if (user.isLocked(now) || user.getStatus() != UserStatus.ACTIVE) {
            auditLogService.recordEvent(AuditEventType.LOGIN_FAILED, null, user.getId(), Map.of());
            throw UserAccountException.authenticationFailed();
        }
        if (!passwordHasher.verify(rawPassword, user.getPasswordHash())) {
            recordLoginFailure(user);
            throw UserAccountException.authenticationFailed();
        }
        user.resetFailedLoginCount();
        userRepository.save(user);
        auditLogService.recordEvent(AuditEventType.LOGIN_SUCCEEDED, user.getId(), user.getId(), Map.of());
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }

    private void recordLoginFailure(User user) {
        user.incrementFailedLoginCount();
        if (user.getFailedLoginCount() >= properties.maxFailedLoginAttempts()) {
            user.setLockedUntil(
                    Instant.now().plus(Duration.ofMinutes(properties.accountLockDurationMinutes())));
            userRepository.save(user);
            auditLogService.recordEvent(AuditEventType.ACCOUNT_LOCKED, null, user.getId(), Map.of());
        } else {
            userRepository.save(user);
        }
        auditLogService.recordEvent(AuditEventType.LOGIN_FAILED, null, user.getId(), Map.of());
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, Locale locale) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || userOpt.get().getStatus() != UserStatus.ACTIVE) {
            return; // BR-23/BR-24: always the same externally-visible outcome
        }
        var user = userOpt.get();
        passwordResetTokenRepository
                .findAllByUserIdAndUsedAtIsNull(user.getId())
                .forEach(PasswordResetToken::markUsed);

        var rawToken = tokenGenerator.generate();
        var ttlHours = properties.passwordResetTokenTtlHours();
        var resetToken =
                new PasswordResetToken(
                        user.getId(),
                        tokenGenerator.hash(rawToken),
                        Instant.now().plus(Duration.ofHours(ttlHours)));
        passwordResetTokenRepository.save(resetToken);

        var link = appProperties.baseUrl() + "/password/reset/" + rawToken;
        notificationService.sendPasswordResetEmail(user.getEmail(), locale, link, ttlHours);
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        var tokenHash = tokenGenerator.hash(resetToken);
        var token =
                passwordResetTokenRepository
                        .findByTokenHash(tokenHash)
                        .filter(t -> t.isUsable(Instant.now()))
                        .orElseThrow(UserAccountException::invalidToken);
        validatePassword(newPassword);

        var user = findUserOrThrow(token.getUserId());
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
        token.markUsed();
        passwordResetTokenRepository.save(token);
        refreshTokenService.revokeAllForUser(user.getId());

        auditLogService.recordEvent(
                AuditEventType.PASSWORD_RESET_COMPLETED, user.getId(), user.getId(), Map.of());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        var user = findUserOrThrow(userId);
        if (!passwordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw UserAccountException.currentPasswordMismatch();
        }
        validatePassword(newPassword);
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);
        auditLogService.recordEvent(AuditEventType.PASSWORD_CHANGED, userId, userId, Map.of());
    }

    @Override
    @Transactional
    public void ensureInitialAdmin() {
        var initialAdmin = properties.initialAdmin();
        if (initialAdmin == null || initialAdmin.email() == null || initialAdmin.email().isBlank()) {
            return;
        }
        if (userRepository.findByEmail(initialAdmin.email()).isPresent()) {
            return; // BR-32: idempotent, created once
        }
        var user = new User(initialAdmin.email(), UserRole.ADMIN);
        user.setName("Administrator");
        user.setPasswordHash(passwordHasher.hash(initialAdmin.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRegisteredAt(Instant.now());
        userRepository.save(user);
        auditLogService.recordEvent(
                AuditEventType.USER_REGISTERED, null, user.getId(), Map.of("bootstrap", true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAllByOrderByCreatedAtAsc().stream().map(this::toSummary).toList();
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getInvitedAt(),
                user.getRegisteredAt());
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserAccountException::userNotFound);
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < properties.passwordMinLength()) {
            throw UserAccountException.passwordTooShort();
        }
        if (breachedPasswordChecker.isBreached(rawPassword)) {
            throw UserAccountException.breachedPassword();
        }
    }
}
