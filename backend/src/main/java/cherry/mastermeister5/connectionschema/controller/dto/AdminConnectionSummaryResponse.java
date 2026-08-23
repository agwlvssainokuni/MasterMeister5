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

package cherry.mastermeister5.connectionschema.controller.dto;

import cherry.mastermeister5.connectionschema.entity.ConnectionStatus;
import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.service.ConnectionSummary;
import java.time.Instant;

/**
 * ADMIN-only listing response for ConnectionListScreen: carries the fields
 * the connection-edit form needs to pre-fill (everything except the
 * password, which is never returned) plus the last schema-import timestamp.
 * Never exposed on the general-user {@code /api/connections} endpoint —
 * see {@link ConnectionSummaryResponse}.
 */
public record AdminConnectionSummaryResponse(
        Long id,
        String name,
        RdbmsType rdbmsType,
        String host,
        int port,
        String databaseName,
        String schemaNameHint,
        String extraParams,
        String username,
        ConnectionStatus status,
        Instant lastSchemaImportAt) {

    public static AdminConnectionSummaryResponse from(ConnectionSummary summary) {
        return new AdminConnectionSummaryResponse(
                summary.id(),
                summary.name(),
                summary.rdbmsType(),
                summary.host(),
                summary.port(),
                summary.databaseName(),
                summary.schemaNameHint(),
                summary.extraParams(),
                summary.username(),
                summary.status(),
                summary.lastSchemaImportAt());
    }
}
