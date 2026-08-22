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

package cherry.mastermeister5.query.service;

import org.springframework.http.HttpStatus;

/**
 * business-rules.md (Unit 6) violations raised by {@link QueryService}. Same
 * single-class-with-factory-methods pattern as Unit 2/3/4/5's exceptions.
 */
public class QueryException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;

    private QueryException(HttpStatus status, String errorCode, String messageKey) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
    }

    public static QueryException notFound() {
        return new QueryException(HttpStatus.NOT_FOUND, "QUERY_NOT_FOUND", "errors.query_not_found");
    }

    public static QueryException permissionDenied() {
        return new QueryException(HttpStatus.FORBIDDEN, "QUERY_PERMISSION_DENIED", "errors.query_permission_denied");
    }

    public static QueryException unsafeSql() {
        return new QueryException(HttpStatus.BAD_REQUEST, "QUERY_UNSAFE_SQL", "errors.query_unsafe_sql");
    }

    public static QueryException schemaNotAllowed() {
        return new QueryException(HttpStatus.BAD_REQUEST, "QUERY_SCHEMA_NOT_ALLOWED", "errors.query_schema_not_allowed");
    }

    public static QueryException invalidRequest() {
        return new QueryException(HttpStatus.BAD_REQUEST, "QUERY_INVALID_REQUEST", "errors.query_invalid_request");
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
