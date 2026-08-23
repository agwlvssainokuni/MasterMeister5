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
import cherry.mastermeister5.connectionschema.controller.dto.RegisterConnectionRequest;
import cherry.mastermeister5.connectionschema.controller.dto.SchemaImportResultResponse;
import cherry.mastermeister5.connectionschema.controller.dto.SchemaViewResponse;
import cherry.mastermeister5.connectionschema.service.ConnectionSchemaService;
import cherry.mastermeister5.connectionschema.service.RegisterConnectionCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: {@code /api/admin/**} is restricted to ADMIN at the
 * SecurityConfig path-matcher level (Unit 2); {@code @PreAuthorize} here is
 * defense in depth, mirroring Unit 2's AdminUserController.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ConnectionController {

    private final ConnectionSchemaService connectionSchemaService;

    public ConnectionController(ConnectionSchemaService connectionSchemaService) {
        this.connectionSchemaService = connectionSchemaService;
    }

    @GetMapping("/api/admin/connections")
    public List<ConnectionSummaryResponse> listConnections() {
        return connectionSchemaService.listConnections().stream()
                .map(ConnectionSummaryResponse::from)
                .toList();
    }

    @PostMapping("/api/admin/connections")
    public void registerConnection(
            @Valid @RequestBody RegisterConnectionRequest request, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        connectionSchemaService.registerConnection(
                new RegisterConnectionCommand(
                        request.name(),
                        request.rdbmsType(),
                        request.host(),
                        request.port(),
                        request.databaseName(),
                        request.schemaNameHint(),
                        request.extraParams(),
                        request.username(),
                        request.password()),
                actorUserId);
    }

    @PostMapping("/api/admin/connections/{connectionId}/deactivate")
    public void deactivateConnection(@PathVariable Long connectionId, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        connectionSchemaService.deactivateConnection(connectionId, actorUserId);
    }

    @PostMapping("/api/admin/connections/{connectionId}/reactivate")
    public void reactivateConnection(@PathVariable Long connectionId, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        connectionSchemaService.reactivateConnection(connectionId, actorUserId);
    }

    @PostMapping("/api/admin/connections/{connectionId}/schema-import")
    public SchemaImportResultResponse importSchema(
            @PathVariable Long connectionId, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        return SchemaImportResultResponse.from(
                connectionSchemaService.importSchema(connectionId, actorUserId));
    }

    /** Unit 4's PermissionScreen tree display (nfr-design/logical-components.md). */
    @GetMapping("/api/admin/connections/{connectionId}/schema")
    public List<SchemaViewResponse> getSchema(@PathVariable Long connectionId) {
        return connectionSchemaService.getSchema(connectionId).stream().map(SchemaViewResponse::from).toList();
    }
}
