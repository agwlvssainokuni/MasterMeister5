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

package cherry.mastermeister5.connectionschema.controller;

import cherry.mastermeister5.connectionschema.controller.dto.ConnectionSummaryResponse;
import cherry.mastermeister5.connectionschema.controller.dto.SchemaViewResponse;
import cherry.mastermeister5.connectionschema.service.ConnectionSchemaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only connection/schema listing for screens available to every
 * authenticated user, not just ADMIN: Unit 5's MasterDataScreen (connection
 * picker) and Unit 6's QueryScreen (connection + schema pickers). Neither
 * response type ({@link ConnectionSummaryResponse}/{@link SchemaViewResponse})
 * exposes credentials, so this is safe to open beyond ADMIN.
 *
 * <p>{@link ConnectionController}'s {@code /api/admin/connections/**}
 * endpoints (register/deactivate/reactivate/import-schema, and its own copies
 * of these two GET endpoints used by the admin-only ConnectionListScreen/
 * PermissionScreen) are unaffected and remain ADMIN-only.
 *
 * <p>Found missing via E2E testing (main-journey.spec.ts): a general user's
 * MasterDataScreen/QueryScreen called the same frontend {@code
 * listConnections}/{@code getSchema} functions as the ADMIN-only screens,
 * which pointed at {@code /api/admin/connections/**} — every such call 403'd
 * for a non-ADMIN user, so a general user could never populate the connection
 * dropdown on either screen. No unit/slice test exercised the actual role
 * restriction of that endpoint from a general user's perspective.
 */
@RestController
public class ConnectionViewController {

    private final ConnectionSchemaService connectionSchemaService;

    public ConnectionViewController(ConnectionSchemaService connectionSchemaService) {
        this.connectionSchemaService = connectionSchemaService;
    }

    @GetMapping("/api/connections")
    public List<ConnectionSummaryResponse> listConnections() {
        return connectionSchemaService.listConnections().stream()
                .map(ConnectionSummaryResponse::from)
                .toList();
    }

    @GetMapping("/api/connections/{connectionId}/schema")
    public List<SchemaViewResponse> getSchema(@PathVariable Long connectionId) {
        return connectionSchemaService.getSchema(connectionId).stream().map(SchemaViewResponse::from).toList();
    }
}
