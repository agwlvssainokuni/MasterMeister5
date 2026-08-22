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

package cherry.mastermeister5.connectionschema.service;

import org.springframework.http.HttpStatus;

/**
 * business-rules.md violations raised by {@link ConnectionSchemaService}.
 * Same single-class-with-factory-methods pattern as Unit 2's
 * {@code UserAccountException}.
 */
public class ConnectionException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;

    private ConnectionException(HttpStatus status, String errorCode, String messageKey) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public static ConnectionException nameAlreadyExists() {
        return new ConnectionException(
                HttpStatus.CONFLICT, "CONNECTION_NAME_ALREADY_EXISTS", "errors.connection_name_already_exists");
    }

    public static ConnectionException connectionTestFailed(String reasonCode) {
        return new ConnectionException(
                HttpStatus.BAD_REQUEST, "CONNECTION_TEST_FAILED_" + reasonCode, "errors.connection_test_failed_" + reasonCode.toLowerCase());
    }

    public static ConnectionException notFound() {
        return new ConnectionException(
                HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "errors.connection_not_found");
    }

    public static ConnectionException notActive() {
        return new ConnectionException(
                HttpStatus.CONFLICT, "CONNECTION_NOT_ACTIVE", "errors.connection_not_active");
    }

    public static ConnectionException notDeactivated() {
        return new ConnectionException(
                HttpStatus.CONFLICT, "CONNECTION_NOT_DEACTIVATED", "errors.connection_not_deactivated");
    }

    public static ConnectionException invalidIdentifier() {
        return new ConnectionException(
                HttpStatus.BAD_REQUEST, "CONNECTION_INVALID_IDENTIFIER", "errors.connection_invalid_identifier");
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
