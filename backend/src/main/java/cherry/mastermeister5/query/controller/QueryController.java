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

package cherry.mastermeister5.query.controller;

import cherry.mastermeister5.query.controller.dto.DetectParametersRequest;
import cherry.mastermeister5.query.controller.dto.ExecuteQueryRequest;
import cherry.mastermeister5.query.controller.dto.ExecutionHistoryPageResponse;
import cherry.mastermeister5.query.controller.dto.ParameterDescriptorResponse;
import cherry.mastermeister5.query.controller.dto.QueryResultResponse;
import cherry.mastermeister5.query.controller.dto.SaveQueryRequest;
import cherry.mastermeister5.query.controller.dto.SavedQueryIdResponse;
import cherry.mastermeister5.query.controller.dto.SavedQueryResponse;
import cherry.mastermeister5.query.service.ExecutionHistoryFilterCriteria;
import cherry.mastermeister5.query.service.QueryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: covered by SecurityConfig's {@code anyRequest().authenticated()}
 * fallback (US-4.1〜US-4.6 are available to every authenticated user, not just
 * ADMIN; PRIVATE-query and creator-only access is enforced by
 * {@link QueryService}, not by role).
 */
@RestController
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/query/saved-queries")
    public List<SavedQueryResponse> listSavedQueries(Authentication authentication) {
        return queryService.listSavedQueries(Long.valueOf(authentication.getName())).stream()
                .map(SavedQueryResponse::from)
                .toList();
    }

    @PostMapping("/api/query/saved-queries")
    public SavedQueryIdResponse saveQuery(@Valid @RequestBody SaveQueryRequest request, Authentication authentication) {
        var savedQueryId =
                queryService.saveQuery(
                        request.name(),
                        request.sqlText(),
                        request.visibility(),
                        request.savedQueryId(),
                        Long.valueOf(authentication.getName()));
        return new SavedQueryIdResponse(savedQueryId);
    }

    @DeleteMapping("/api/query/saved-queries/{id}")
    public ResponseEntity<Void> retireQuery(@PathVariable Long id, Authentication authentication) {
        queryService.retireQuery(id, Long.valueOf(authentication.getName()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/query/detect-parameters")
    public List<ParameterDescriptorResponse> detectParameters(@Valid @RequestBody DetectParametersRequest request) {
        return queryService.detectParameters(request.sqlText()).stream().map(ParameterDescriptorResponse::from).toList();
    }

    @PostMapping("/api/query/execute")
    public QueryResultResponse executeQuery(@Valid @RequestBody ExecuteQueryRequest request, Authentication authentication) {
        var result =
                queryService.executeQuery(
                        request.sqlText(),
                        request.savedQueryId(),
                        request.connectionId(),
                        request.schemaName(),
                        request.params(),
                        Long.valueOf(authentication.getName()));
        return QueryResultResponse.from(result);
    }

    @GetMapping("/api/query/execution-history")
    public ExecutionHistoryPageResponse listExecutionHistory(
            @RequestParam(required = false) Long executedByUserId,
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String schemaName,
            @RequestParam(required = false) String sqlTextContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var criteria = new ExecutionHistoryFilterCriteria(executedByUserId, connectionId, schemaName, sqlTextContains, fromDate, toDate);
        return ExecutionHistoryPageResponse.from(queryService.listExecutionHistory(criteria, PageRequest.of(page, size)));
    }
}
