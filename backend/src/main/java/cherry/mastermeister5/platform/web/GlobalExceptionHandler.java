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

package cherry.mastermeister5.platform.web;

import cherry.mastermeister5.connectionschema.service.ConnectionException;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * SECURITY-15: catches unhandled exceptions and always returns a generic,
 * i18n-resolved message — never a stack trace or internal detail (SECURITY-09).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponseFactory errorResponseFactory;

    public GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        errorResponseFactory.create(
                                "VALIDATION_ERROR", "errors.validation", request.getLocale()));
    }

    @ExceptionHandler(UserAccountException.class)
    public ResponseEntity<ErrorResponse> handleUserAccount(
            UserAccountException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(
                        errorResponseFactory.create(
                                ex.getErrorCode(), ex.getMessageKey(), request.getLocale()));
    }

    @ExceptionHandler(ConnectionException.class)
    public ResponseEntity<ErrorResponse> handleConnection(
            ConnectionException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(
                        errorResponseFactory.create(
                                ex.getErrorCode(), ex.getMessageKey(), request.getLocale()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        errorResponseFactory.create(
                                "INTERNAL_ERROR", "errors.internal", request.getLocale()));
    }
}
