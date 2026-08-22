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

package cherry.mastermeister5.mastermaintenance.controller.dto;

import cherry.mastermeister5.mastermaintenance.entity.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * code-generation-plan.md deviation: a filter condition list does not map
 * cleanly onto GET query parameters, so {@code listRecords} is exposed as a
 * POST "search" request instead of the GET sketched in the plan.
 */
public record ListRecordsRequest(
        List<FilterConditionRequest> conditions,
        String rawWhereClause,
        String sortColumn,
        SortDirection sortDirection,
        String rawOrderByClause,
        @Min(0) int page,
        @Min(1) @Max(200) int pageSize) {
}
