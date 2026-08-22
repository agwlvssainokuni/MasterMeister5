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

package cherry.mastermeister5.accesscontrol.controller;

import cherry.mastermeister5.accesscontrol.controller.dto.ImportPermissionsResultResponse;
import cherry.mastermeister5.accesscontrol.controller.dto.PermissionEntryResponse;
import cherry.mastermeister5.accesscontrol.controller.dto.SetAuxiliaryPermissionRequest;
import cherry.mastermeister5.accesscontrol.controller.dto.SetPrimaryPermissionRequest;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import cherry.mastermeister5.accesscontrol.service.SetAuxiliaryPermissionCommand;
import cherry.mastermeister5.accesscontrol.service.SetPrimaryPermissionCommand;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: {@code /api/admin/**} is restricted to ADMIN at the
 * SecurityConfig path-matcher level; {@code @PreAuthorize} here is defense
 * in depth, mirroring Unit 2/3's controllers.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final AccessControlService accessControlService;

    public PermissionController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/api/admin/permissions")
    public List<PermissionEntryResponse> listPermissionEntries(
            @RequestParam Long connectionId, @RequestParam SubjectType subjectType, @RequestParam Long subjectId) {
        return accessControlService.listPermissionEntries(connectionId, subjectType, subjectId).stream()
                .map(PermissionEntryResponse::from)
                .toList();
    }

    @PostMapping("/api/admin/permissions/primary")
    public void setPrimaryPermission(
            @Valid @RequestBody SetPrimaryPermissionRequest request, Authentication authentication) {
        accessControlService.setPrimaryPermission(
                new SetPrimaryPermissionCommand(
                        request.subjectType(),
                        request.subjectId(),
                        request.connectionId(),
                        request.resourceLevel(),
                        request.schemaName(),
                        request.tableName(),
                        request.columnName(),
                        request.primaryLevel()),
                Long.valueOf(authentication.getName()));
    }

    @PostMapping("/api/admin/permissions/auxiliary")
    public void setAuxiliaryPermission(
            @Valid @RequestBody SetAuxiliaryPermissionRequest request, Authentication authentication) {
        accessControlService.setAuxiliaryPermission(
                new SetAuxiliaryPermissionCommand(
                        request.subjectType(),
                        request.subjectId(),
                        request.connectionId(),
                        request.resourceLevel(),
                        request.schemaName(),
                        request.tableName(),
                        request.auxCreate(),
                        request.auxDelete()),
                Long.valueOf(authentication.getName()));
    }

    @GetMapping("/api/admin/connections/{connectionId}/permissions/export")
    public ResponseEntity<String> exportPermissions(@PathVariable Long connectionId, Authentication authentication) {
        var yaml = accessControlService.exportPermissions(connectionId, Long.valueOf(authentication.getName()));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"permissions-" + connectionId + ".yaml\"")
                .body(yaml);
    }

    @PostMapping("/api/admin/connections/{connectionId}/permissions/import")
    public ImportPermissionsResultResponse importPermissions(
            @PathVariable Long connectionId, @RequestBody String yamlContent, Authentication authentication) {
        return ImportPermissionsResultResponse.from(
                accessControlService.importPermissions(
                        connectionId, yamlContent, Long.valueOf(authentication.getName())));
    }
}
