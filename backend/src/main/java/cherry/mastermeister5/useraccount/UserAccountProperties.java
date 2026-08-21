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

package cherry.mastermeister5.useraccount;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * business-rules.md BR-4 (invitation TTL default 3h), BR-15 (lockout default
 * 5 attempts / 15 minutes), BR-25 (password reset TTL default 3h).
 */
@ConfigurationProperties(prefix = "mastermeister5.security.account")
public record UserAccountProperties(
        long invitationTokenTtlHours,
        long passwordResetTokenTtlHours,
        int maxFailedLoginAttempts,
        long accountLockDurationMinutes,
        int passwordMinLength,
        @NestedConfigurationProperty InitialAdmin initialAdmin) {

    public record InitialAdmin(String email, String password) {
    }
}
