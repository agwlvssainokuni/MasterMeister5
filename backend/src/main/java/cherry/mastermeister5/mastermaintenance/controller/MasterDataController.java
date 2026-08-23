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

import cherry.mastermeister5.mastermaintenance.controller.dto.ApplyChangesRequest;
import cherry.mastermeister5.mastermaintenance.controller.dto.ApplyResultResponse;
import cherry.mastermeister5.mastermaintenance.controller.dto.ListRecordsRequest;
import cherry.mastermeister5.mastermaintenance.controller.dto.RecordPageResponse;
import cherry.mastermeister5.mastermaintenance.controller.dto.TablePermissionResponse;
import cherry.mastermeister5.mastermaintenance.controller.dto.TableSummaryResponse;
import cherry.mastermeister5.mastermaintenance.service.FilterCondition;
import cherry.mastermeister5.mastermaintenance.service.FilterCriteria;
import cherry.mastermeister5.mastermaintenance.service.ListRecordsCommand;
import cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceService;
import cherry.mastermeister5.mastermaintenance.service.RecordChange;
import cherry.mastermeister5.mastermaintenance.service.RecordChangeSet;
import cherry.mastermeister5.mastermaintenance.service.SortCriteria;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: covered by SecurityConfig's {@code anyRequest().authenticated()}
 * fallback (US-3.1〜US-3.6 are available to every authenticated user, not just
 * ADMIN; per-column/per-table access is enforced by Unit 4's effective
 * permissions inside {@link MasterMaintenanceService}, not by role).
 */
@RestController
public class MasterDataController {

    private final MasterMaintenanceService masterMaintenanceService;

    public MasterDataController(MasterMaintenanceService masterMaintenanceService) {
        this.masterMaintenanceService = masterMaintenanceService;
    }

    @PostMapping("/api/data/connections/{connectionId}/tables")
    public List<TableSummaryResponse> listTables(@PathVariable Long connectionId, Authentication authentication) {
        return masterMaintenanceService.listTables(connectionId, Long.valueOf(authentication.getName())).stream()
                .map(TableSummaryResponse::from)
                .toList();
    }

    @PostMapping("/api/data/connections/{connectionId}/tables/{schemaName}/{tableName}/records")
    public RecordPageResponse listRecords(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody ListRecordsRequest request,
            Authentication authentication) {
        var conditions =
                request.conditions() == null
                        ? List.<FilterCondition>of()
                        : request.conditions().stream()
                                .map(c -> new FilterCondition(c.columnName(), c.operator(), c.value()))
                                .toList();
        var command =
                new ListRecordsCommand(
                        connectionId,
                        schemaName,
                        tableName,
                        Long.valueOf(authentication.getName()),
                        new FilterCriteria(conditions, request.rawWhereClause()),
                        new SortCriteria(request.sortColumn(), request.sortDirection(), request.rawOrderByClause()),
                        request.page(),
                        request.pageSize());
        return RecordPageResponse.from(masterMaintenanceService.listRecords(command));
    }

    @GetMapping("/api/data/connections/{connectionId}/tables/{schemaName}/{tableName}/permissions")
    public TablePermissionResponse getTablePermission(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            Authentication authentication) {
        return TablePermissionResponse.from(
                masterMaintenanceService.resolveTablePermission(
                        connectionId, schemaName, tableName, Long.valueOf(authentication.getName())));
    }

    @PostMapping("/api/data/connections/{connectionId}/tables/{schemaName}/{tableName}/apply")
    public ApplyResultResponse applyChanges(
            @PathVariable Long connectionId,
            @PathVariable String schemaName,
            @PathVariable String tableName,
            @Valid @RequestBody ApplyChangesRequest request,
            Authentication authentication) {
        var changes =
                request.changes().stream()
                        .map(c -> new RecordChange(c.operation(), c.primaryKeyValues(), c.columnValues()))
                        .toList();
        var result =
                masterMaintenanceService.applyChanges(
                        connectionId, schemaName, tableName, Long.valueOf(authentication.getName()), new RecordChangeSet(changes));
        return ApplyResultResponse.from(result);
    }
}
