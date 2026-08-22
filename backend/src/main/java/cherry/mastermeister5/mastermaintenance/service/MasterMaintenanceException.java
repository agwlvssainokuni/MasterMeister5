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

package cherry.mastermeister5.mastermaintenance.service;

import org.springframework.http.HttpStatus;

/**
 * business-rules.md violations raised by {@link MasterMaintenanceService}.
 * Same single-class-with-factory-methods pattern as Unit 2/3/4's exceptions.
 */
public class MasterMaintenanceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;

    private MasterMaintenanceException(HttpStatus status, String errorCode, String messageKey) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public static MasterMaintenanceException invalidIdentifier() {
        return new MasterMaintenanceException(
                HttpStatus.BAD_REQUEST, "MASTER_DATA_INVALID_IDENTIFIER", "errors.master_data_invalid_identifier");
    }

    public static MasterMaintenanceException unsafeRawClause() {
        return new MasterMaintenanceException(
                HttpStatus.BAD_REQUEST, "MASTER_DATA_UNSAFE_RAW_CLAUSE", "errors.master_data_unsafe_raw_clause");
    }

    public static MasterMaintenanceException noPrimaryKey() {
        return new MasterMaintenanceException(
                HttpStatus.BAD_REQUEST, "MASTER_DATA_NO_PRIMARY_KEY", "errors.master_data_no_primary_key");
    }

    public static MasterMaintenanceException permissionDenied() {
        return new MasterMaintenanceException(
                HttpStatus.FORBIDDEN, "MASTER_DATA_PERMISSION_DENIED", "errors.master_data_permission_denied");
    }

    public static MasterMaintenanceException validationFailed() {
        return new MasterMaintenanceException(
                HttpStatus.BAD_REQUEST, "MASTER_DATA_VALIDATION_FAILED", "errors.master_data_validation_failed");
    }

    public static MasterMaintenanceException yamlParseFailed() {
        return new MasterMaintenanceException(
                HttpStatus.BAD_REQUEST, "CUSTOMIZATION_YAML_PARSE_FAILED", "errors.customization_yaml_parse_failed");
    }

    public static MasterMaintenanceException notFound() {
        return new MasterMaintenanceException(
                HttpStatus.NOT_FOUND, "MASTER_DATA_NOT_FOUND", "errors.master_data_not_found");
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
