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

import org.springframework.http.HttpStatus;

/**
 * business-rules.md violations raised by {@link UserAccountService}.
 * {@link cherry.mastermeister5.platform.web.GlobalExceptionHandler} maps
 * every instance the same way (status + i18n message key), so a single
 * exception type with factory methods per rule avoids one class per rule.
 */
public class UserAccountException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;

    private UserAccountException(HttpStatus status, String errorCode, String messageKey) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public static UserAccountException emailAlreadyRegistered() {
        return new UserAccountException(
                HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "errors.email_already_registered");
    }

    public static UserAccountException invitationAlreadyPending() {
        return new UserAccountException(
                HttpStatus.CONFLICT, "INVITATION_ALREADY_PENDING", "errors.invitation_already_pending");
    }

    public static UserAccountException invitationNotPending() {
        return new UserAccountException(
                HttpStatus.CONFLICT, "INVITATION_NOT_PENDING", "errors.invitation_not_pending");
    }

    public static UserAccountException invitationTokenExpired() {
        return new UserAccountException(
                HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_EXPIRED", "errors.invitation_token_expired");
    }

    public static UserAccountException invalidToken() {
        return new UserAccountException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "errors.invalid_token");
    }

    public static UserAccountException passwordTooShort() {
        return new UserAccountException(
                HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT", "errors.password_too_short");
    }

    public static UserAccountException breachedPassword() {
        return new UserAccountException(
                HttpStatus.BAD_REQUEST, "BREACHED_PASSWORD", "errors.breached_password");
    }

    public static UserAccountException currentPasswordMismatch() {
        return new UserAccountException(
                HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH", "errors.current_password_mismatch");
    }

    public static UserAccountException authenticationFailed() {
        return new UserAccountException(
                HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "errors.authentication_failed");
    }

    public static UserAccountException userNotFound() {
        return new UserAccountException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "errors.user_not_found");
    }

    /** domain-entities.md state diagram: deactivateUser is only valid from ACTIVE. */
    public static UserAccountException userNotActive() {
        return new UserAccountException(HttpStatus.CONFLICT, "USER_NOT_ACTIVE", "errors.user_not_active");
    }

    /** domain-entities.md state diagram: reactivateUser is only valid from DEACTIVATED. */
    public static UserAccountException userNotDeactivated() {
        return new UserAccountException(
                HttpStatus.CONFLICT, "USER_NOT_DEACTIVATED", "errors.user_not_deactivated");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
