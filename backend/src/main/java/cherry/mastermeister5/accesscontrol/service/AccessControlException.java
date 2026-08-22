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

package cherry.mastermeister5.accesscontrol.service;

import org.springframework.http.HttpStatus;

/**
 * business-rules.md violations raised by {@link AccessControlService}. Same
 * single-class-with-factory-methods pattern as Unit 2's
 * {@code UserAccountException} / Unit 3's {@code ConnectionException}.
 */
public class AccessControlException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;

    private AccessControlException(HttpStatus status, String errorCode, String messageKey) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public static AccessControlException groupNameAlreadyExists() {
        return new AccessControlException(
                HttpStatus.CONFLICT, "GROUP_NAME_ALREADY_EXISTS", "errors.group_name_already_exists");
    }

    public static AccessControlException groupNotFound() {
        return new AccessControlException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "errors.group_not_found");
    }

    public static AccessControlException membershipAlreadyExists() {
        return new AccessControlException(
                HttpStatus.CONFLICT, "MEMBERSHIP_ALREADY_EXISTS", "errors.membership_already_exists");
    }

    public static AccessControlException membershipNotFound() {
        return new AccessControlException(
                HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "errors.membership_not_found");
    }

    public static AccessControlException invalidIdentifier() {
        return new AccessControlException(
                HttpStatus.BAD_REQUEST, "ACCESS_CONTROL_INVALID_IDENTIFIER", "errors.access_control_invalid_identifier");
    }

    public static AccessControlException yamlParseFailed() {
        return new AccessControlException(
                HttpStatus.BAD_REQUEST, "PERMISSION_YAML_PARSE_FAILED", "errors.permission_yaml_parse_failed");
    }

    public static AccessControlException subjectNotResolved() {
        return new AccessControlException(
                HttpStatus.BAD_REQUEST, "PERMISSION_SUBJECT_NOT_RESOLVED", "errors.permission_subject_not_resolved");
    }

    public static AccessControlException duplicateEntry() {
        return new AccessControlException(
                HttpStatus.BAD_REQUEST, "PERMISSION_DUPLICATE_ENTRY", "errors.permission_duplicate_entry");
    }

    /** business-rules.md BR-6: auxiliary permissions do not apply at COLUMN level. */
    public static AccessControlException auxiliaryNotApplicable() {
        return new AccessControlException(
                HttpStatus.BAD_REQUEST, "PERMISSION_AUXILIARY_NOT_APPLICABLE", "errors.permission_auxiliary_not_applicable");
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
