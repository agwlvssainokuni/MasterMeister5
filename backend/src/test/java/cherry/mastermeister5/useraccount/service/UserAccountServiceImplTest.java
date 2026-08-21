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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.notification.NotificationService;
import cherry.mastermeister5.platform.AppProperties;
import cherry.mastermeister5.platform.security.BreachedPasswordChecker;
import cherry.mastermeister5.platform.security.PasswordHasher;
import cherry.mastermeister5.platform.security.RefreshTokenService;
import cherry.mastermeister5.platform.security.SecureTokenGenerator;
import cherry.mastermeister5.useraccount.UserAccountProperties;
import cherry.mastermeister5.useraccount.entity.PasswordResetToken;
import cherry.mastermeister5.useraccount.entity.User;
import cherry.mastermeister5.useraccount.entity.UserRole;
import cherry.mastermeister5.useraccount.entity.UserStatus;
import cherry.mastermeister5.useraccount.repository.PasswordResetTokenJpaRepository;
import cherry.mastermeister5.useraccount.repository.UserJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {

    @Mock private UserJpaRepository userRepository;
    @Mock private PasswordResetTokenJpaRepository passwordResetTokenRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private RefreshTokenService refreshTokenService;

    private final PasswordHasher passwordHasher = new PasswordHasher();
    private final BreachedPasswordChecker breachedPasswordChecker = new BreachedPasswordChecker();
    private final SecureTokenGenerator tokenGenerator = new SecureTokenGenerator();
    private final UserAccountProperties properties =
            new UserAccountProperties(3, 3, 5, 15, 8, new UserAccountProperties.InitialAdmin("admin@example.com", "initialPassw0rd"));
    private final AppProperties appProperties = new AppProperties("http://localhost:8080");

    private UserAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        breachedPasswordChecker.loadList();
        service =
                new UserAccountServiceImpl(
                        userRepository,
                        passwordResetTokenRepository,
                        passwordHasher,
                        breachedPasswordChecker,
                        tokenGenerator,
                        notificationService,
                        auditLogService,
                        refreshTokenService,
                        properties,
                        appProperties);
    }

    // --- inviteUser ---

    @Test
    void inviteUserCreatesAnInvitedUserAndSendsAnEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            var user = invocation.getArgument(0, User.class);
                            setId(user, 1L);
                            return user;
                        });

        var userId = service.inviteUser("new@example.com", UserRole.GENERAL, 99L, Locale.JAPANESE);

        assertThat(userId).isEqualTo(1L);
        verify(notificationService)
                .sendInvitationEmail(
                        org.mockito.ArgumentMatchers.eq("new@example.com"),
                        any(),
                        any(),
                        org.mockito.ArgumentMatchers.eq(3L));
    }

    @Test
    void inviteUserRejectsAnAlreadyActiveEmail() {
        var existing = activeUser(2L, "taken@example.com");
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () -> service.inviteUser("taken@example.com", UserRole.GENERAL, 99L, Locale.JAPANESE))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void inviteUserRejectsADuplicatePendingInvitation() {
        var existing = new User("pending@example.com", UserRole.GENERAL);
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                service.inviteUser(
                                        "pending@example.com", UserRole.GENERAL, 99L, Locale.JAPANESE))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("INVITATION_ALREADY_PENDING");
    }

    // --- resendInvitation ---

    @Test
    void resendInvitationRejectsANonPendingUser() {
        var user = activeUser(3L, "active@example.com");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resendInvitation(3L, Locale.JAPANESE))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("INVITATION_NOT_PENDING");
    }

    // --- completeRegistration ---

    @Test
    void completeRegistrationActivatesAnInvitedUser() {
        var user = new User("invitee@example.com", UserRole.GENERAL);
        var rawToken = tokenGenerator.generate();
        user.setInvitationToken(tokenGenerator.hash(rawToken), Instant.now().plusSeconds(3600));
        when(userRepository.findByInvitationTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(user));

        service.completeRegistration(rawToken, "Taro Yamada", "correctHorseBattery1");

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getName()).isEqualTo("Taro Yamada");
        assertThat(passwordHasher.verify("correctHorseBattery1", user.getPasswordHash())).isTrue();
    }

    @Test
    void completeRegistrationRejectsAnExpiredToken() {
        var user = new User("invitee@example.com", UserRole.GENERAL);
        var rawToken = tokenGenerator.generate();
        user.setInvitationToken(tokenGenerator.hash(rawToken), Instant.now().minusSeconds(1));
        when(userRepository.findByInvitationTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.completeRegistration(rawToken, "Name", "correctHorseBattery1"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("INVITATION_TOKEN_EXPIRED");
    }

    @Test
    void completeRegistrationRejectsATooShortPassword() {
        var user = new User("invitee@example.com", UserRole.GENERAL);
        var rawToken = tokenGenerator.generate();
        user.setInvitationToken(tokenGenerator.hash(rawToken), Instant.now().plusSeconds(3600));
        when(userRepository.findByInvitationTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.completeRegistration(rawToken, "Name", "short1"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("PASSWORD_TOO_SHORT");
    }

    @Test
    void completeRegistrationRejectsABreachedPassword() {
        var user = new User("invitee@example.com", UserRole.GENERAL);
        var rawToken = tokenGenerator.generate();
        user.setInvitationToken(tokenGenerator.hash(rawToken), Instant.now().plusSeconds(3600));
        when(userRepository.findByInvitationTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.completeRegistration(rawToken, "Name", "password123"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("BREACHED_PASSWORD");
    }

    // --- deactivate / reactivate: state transition guards ---

    @Test
    void deactivateUserRevokesRefreshTokensAndFlipsStatus() {
        var user = activeUser(4L, "user@example.com");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        service.deactivateUser(4L, 1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        verify(refreshTokenService).revokeAllForUser(4L);
    }

    @Test
    void reactivateUserFlipsStatusBackToActive() {
        var user = activeUser(5L, "user2@example.com");
        user.setStatus(UserStatus.DEACTIVATED);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        service.reactivateUser(5L, 1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": a transition
     * not in the documented state diagram is always rejected. Only ACTIVE
     * -&gt; DEACTIVATED and DEACTIVATED -&gt; ACTIVE are valid.
     */
    @Property
    void deactivateIsRejectedFromAnyNonActiveStatus(@ForAll UserStatus status) {
        if (status == UserStatus.ACTIVE) {
            return; // the one valid starting status; covered by the example test above
        }
        var user = activeUser(6L, "prop@example.com");
        user.setStatus(status);
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.deactivateUser(6L, 1L)).isInstanceOf(UserAccountException.class);
    }

    @Property
    void reactivateIsRejectedFromAnyNonDeactivatedStatus(@ForAll UserStatus status) {
        if (status == UserStatus.DEACTIVATED) {
            return;
        }
        var user = activeUser(6L, "prop2@example.com");
        user.setStatus(status);
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.reactivateUser(6L, 1L)).isInstanceOf(UserAccountException.class);
    }

    // --- authenticate / login failure count & lockout ---

    @Test
    void authenticateSucceedsAndResetsFailedCount() {
        var user = activeUser(7L, "login@example.com");
        user.incrementFailedLoginCount();
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));

        var result = service.authenticate("login@example.com", "correctHorseBattery1");

        assertThat(result.userId()).isEqualTo(7L);
        assertThat(user.getFailedLoginCount()).isEqualTo(0);
    }

    @Test
    void authenticateFailsForAWrongPassword() {
        var user = activeUser(8L, "login2@example.com");
        when(userRepository.findByEmail("login2@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate("login2@example.com", "wrongPassword1"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("AUTHENTICATION_FAILED");
        assertThat(user.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    void authenticateLocksTheAccountAfterMaxFailedAttempts() {
        var user = activeUser(9L, "login3@example.com");
        when(userRepository.findByEmail("login3@example.com")).thenReturn(Optional.of(user));

        for (int i = 0; i < properties.maxFailedLoginAttempts(); i++) {
            assertThatThrownBy(() -> service.authenticate("login3@example.com", "wrongPassword1"));
        }

        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void authenticateRejectsALockedAccountWithoutCheckingThePassword() {
        var user = activeUser(10L, "login4@example.com");
        user.setLockedUntil(Instant.now().plusSeconds(600));
        var lockedUntilBefore = user.getLockedUntil();
        when(userRepository.findByEmail("login4@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate("login4@example.com", "correctHorseBattery1"))
                .isInstanceOf(UserAccountException.class);

        // functional-design "テスト対象プロパティ": re-attempting while locked never
        // extends lockedUntil (idempotent lock).
        assertThat(user.getLockedUntil()).isEqualTo(lockedUntilBefore);
    }

    @Test
    void authenticateRejectsADeactivatedUser() {
        var user = activeUser(11L, "login5@example.com");
        user.setStatus(UserStatus.DEACTIVATED);
        when(userRepository.findByEmail("login5@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.authenticate("login5@example.com", "correctHorseBattery1"))
                .isInstanceOf(UserAccountException.class);
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": failedLoginCount
     * is always &gt;= 0 and always resets to exactly 0 on a subsequent success,
     * regardless of how many failures preceded it (below the lockout
     * threshold, so the account stays usable).
     */
    @Property
    void failedLoginCountAlwaysResetsToZeroOnSuccess(@ForAll @IntRange(min = 0, max = 4) int failures) {
        var user = activeUser(12L, "prop3@example.com");
        when(userRepository.findByEmail("prop3@example.com")).thenReturn(Optional.of(user));

        for (int i = 0; i < failures; i++) {
            try {
                service.authenticate("prop3@example.com", "wrongPassword1");
            } catch (UserAccountException expected) {
                // expected: recorded, loop continues
            }
        }
        assertThat(user.getFailedLoginCount()).isEqualTo(failures);

        service.authenticate("prop3@example.com", "correctHorseBattery1");

        assertThat(user.getFailedLoginCount()).isEqualTo(0);
    }

    // --- password reset ---

    @Test
    void requestPasswordResetSendsAnEmailAndInvalidatesOldTokensForAnActiveUser() {
        var user = activeUser(13L, "reset@example.com");
        setId(user, 13L);
        var oldToken = new PasswordResetToken(13L, "old-hash", Instant.now().plusSeconds(3600));
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(13L))
                .thenReturn(List.of(oldToken));

        service.requestPasswordReset("reset@example.com", Locale.JAPANESE);

        assertThat(oldToken.getUsedAt()).isNotNull();
        verify(notificationService)
                .sendPasswordResetEmail(
                        org.mockito.ArgumentMatchers.eq("reset@example.com"), any(), any(), org.mockito.ArgumentMatchers.eq(3L));
    }

    @Test
    void requestPasswordResetIsANoOpForAnUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@example.com", Locale.JAPANESE);

        verify(notificationService, never()).sendPasswordResetEmail(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void requestPasswordResetSendsNoEmailForAnInvitedUser() {
        var user = new User("invited@example.com", UserRole.GENERAL);
        when(userRepository.findByEmail("invited@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("invited@example.com", Locale.JAPANESE);

        verify(notificationService, never()).sendPasswordResetEmail(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void resetPasswordUpdatesThePasswordAndRevokesRefreshTokens() {
        var user = activeUser(14L, "reset2@example.com");
        setId(user, 14L);
        var rawToken = tokenGenerator.generate();
        var resetToken = new PasswordResetToken(14L, tokenGenerator.hash(rawToken), Instant.now().plusSeconds(3600));
        when(passwordResetTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(resetToken));
        when(userRepository.findById(14L)).thenReturn(Optional.of(user));

        service.resetPassword(rawToken, "newCorrectHorse1");

        assertThat(passwordHasher.verify("newCorrectHorse1", user.getPasswordHash())).isTrue();
        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(refreshTokenService).revokeAllForUser(14L);
    }

    @Test
    void resetPasswordRejectsAnAlreadyUsedToken() {
        var rawToken = tokenGenerator.generate();
        var resetToken = new PasswordResetToken(14L, tokenGenerator.hash(rawToken), Instant.now().plusSeconds(3600));
        resetToken.markUsed();
        when(passwordResetTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken)))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> service.resetPassword(rawToken, "newCorrectHorse1"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_TOKEN");
    }

    /**
     * functional-design/business-logic-model.md "テスト対象プロパティ": at most one
     * usable reset token exists per user — a fresh request always invalidates
     * every previously-unused token.
     */
    @Property
    void requestPasswordResetInvalidatesEveryPriorUnusedToken(@ForAll @IntRange(min = 0, max = 5) int priorTokenCount) {
        var user = activeUser(15L, "prop4@example.com");
        setId(user, 15L);
        when(userRepository.findByEmail("prop4@example.com")).thenReturn(Optional.of(user));
        var priorTokens =
                java.util.stream.IntStream.range(0, priorTokenCount)
                        .mapToObj(
                                i ->
                                        new PasswordResetToken(
                                                15L, "hash-" + i, Instant.now().plusSeconds(3600)))
                        .toList();
        when(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(15L)).thenReturn(priorTokens);

        service.requestPasswordReset("prop4@example.com", Locale.JAPANESE);

        assertThat(priorTokens).allSatisfy(token -> assertThat(token.getUsedAt()).isNotNull());
    }

    // --- changePassword ---

    @Test
    void changePasswordRejectsAWrongCurrentPassword() {
        var user = activeUser(16L, "chpw@example.com");
        when(userRepository.findById(16L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.changePassword(16L, "wrongCurrent1", "newCorrectHorse1"))
                .isInstanceOf(UserAccountException.class)
                .extracting("errorCode")
                .isEqualTo("CURRENT_PASSWORD_MISMATCH");
    }

    @Test
    void changePasswordUpdatesTheHashOnSuccess() {
        var user = activeUser(17L, "chpw2@example.com");
        when(userRepository.findById(17L)).thenReturn(Optional.of(user));

        service.changePassword(17L, "correctHorseBattery1", "newCorrectHorse1");

        assertThat(passwordHasher.verify("newCorrectHorse1", user.getPasswordHash())).isTrue();
    }

    // --- ensureInitialAdmin ---

    @Test
    void ensureInitialAdminCreatesTheConfiguredAdminWhenAbsent() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());

        service.ensureInitialAdmin();

        verify(userRepository)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                user ->
                                        user.getEmail().equals("admin@example.com")
                                                && user.getRole() == UserRole.ADMIN
                                                && user.getStatus() == UserStatus.ACTIVE));
    }

    @Test
    void ensureInitialAdminIsIdempotentWhenAlreadyPresent() {
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(activeUser(1L, "admin@example.com")));

        service.ensureInitialAdmin();

        verify(userRepository, times(0)).save(any(User.class));
    }

    // --- listUsers ---

    @Test
    void listUsersMapsEntitiesToSummaries() {
        var user = activeUser(18L, "list@example.com");
        when(userRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(user));

        var summaries = service.listUsers();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).email()).isEqualTo("list@example.com");
    }

    // --- fixtures ---

    private User activeUser(Long id, String email) {
        var user = new User(email, UserRole.GENERAL);
        setId(user, id);
        user.setName("Test User");
        user.setPasswordHash(passwordHasher.hash("correctHorseBattery1"));
        user.setStatus(UserStatus.ACTIVE);
        user.setRegisteredAt(Instant.now());
        return user;
    }

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
