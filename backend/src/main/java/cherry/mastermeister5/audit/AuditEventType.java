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

package cherry.mastermeister5.audit;

/**
 * business-rules.md BR-30: events Unit 2 must record. Later Units append
 * their own event types to this enum rather than introducing a parallel
 * mechanism (tech-stack-decisions.md: single table, JSON detail column).
 */
public enum AuditEventType {
    USER_INVITED,
    INVITATION_RESENT,
    USER_REGISTERED,
    USER_ROLE_CHANGED,
    USER_DEACTIVATED,
    USER_REACTIVATED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    LOGOUT,
    REFRESH_TOKEN_REUSE_DETECTED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_CHANGED,
    CONNECTION_REGISTERED,
    CONNECTION_DEACTIVATED,
    CONNECTION_REACTIVATED,
    SCHEMA_IMPORTED
}
