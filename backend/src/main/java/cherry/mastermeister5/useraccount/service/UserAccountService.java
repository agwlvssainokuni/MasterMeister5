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

import cherry.mastermeister5.useraccount.entity.UserRole;
import java.util.List;
import java.util.Locale;

/**
 * component-methods.md: UserAccountComponent. {@code recordLoginFailure} is
 * folded into {@link #authenticate} (business-logic-model.md §6) rather than
 * exposed separately.
 */
public interface UserAccountService {

    Long inviteUser(String email, UserRole role, Long invitedByUserId, Locale locale);

    void resendInvitation(Long userId, Locale locale);

    Long completeRegistration(String invitationToken, String name, String rawPassword);

    void changeRole(Long userId, UserRole role, Long actorUserId);

    void deactivateUser(Long userId, Long actorUserId);

    void reactivateUser(Long userId, Long actorUserId);

    AuthenticatedUser authenticate(String email, String rawPassword);

    /** Used when reissuing an access token on refresh (current role, not the one at login time). */
    AuthenticatedUser getAuthenticatedUser(Long userId);

    void requestPasswordReset(String email, Locale locale);

    void resetPassword(String resetToken, String newPassword);

    void changePassword(Long userId, String currentPassword, String newPassword);

    void ensureInitialAdmin();

    List<UserSummary> listUsers();
}
