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

package cherry.mastermeister5.mastermaintenance.controller;

import cherry.mastermeister5.mastermaintenance.controller.dto.CustomizationDefinitionResponse;
import cherry.mastermeister5.mastermaintenance.controller.dto.ImportCustomizationResultResponse;
import cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: {@code /api/admin/**} is restricted to ADMIN at the
 * SecurityConfig path-matcher level; {@code @PreAuthorize} here is defense
 * in depth, mirroring Unit 2/3/4's controllers.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class CustomizationController {

    private final MasterMaintenanceService masterMaintenanceService;

    public CustomizationController(MasterMaintenanceService masterMaintenanceService) {
        this.masterMaintenanceService = masterMaintenanceService;
    }

    @GetMapping("/api/admin/connections/{connectionId}/customizations/{schemaName}/{tableName}")
    public CustomizationDefinitionResponse getCustomizationDefinition(
            @PathVariable Long connectionId, @PathVariable String schemaName, @PathVariable String tableName) {
        return CustomizationDefinitionResponse.from(
                masterMaintenanceService.getCustomizationDefinition(connectionId, schemaName, tableName));
    }

    @GetMapping("/api/admin/connections/{connectionId}/customizations/export")
    public ResponseEntity<String> exportCustomizationDefinition(@PathVariable Long connectionId, Authentication authentication) {
        var yaml = masterMaintenanceService.exportCustomizationDefinition(connectionId, Long.valueOf(authentication.getName()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customizations-" + connectionId + ".yaml\"")
                .body(yaml);
    }

    @PostMapping("/api/admin/connections/{connectionId}/customizations/import")
    public ImportCustomizationResultResponse importCustomizationDefinition(
            @PathVariable Long connectionId, @RequestBody String yamlContent, Authentication authentication) {
        return ImportCustomizationResultResponse.from(
                masterMaintenanceService.importCustomizationDefinition(
                        connectionId, yamlContent, Long.valueOf(authentication.getName())));
    }
}
