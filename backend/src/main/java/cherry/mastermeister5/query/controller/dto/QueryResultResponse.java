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

package cherry.mastermeister5.query.controller.dto;

import cherry.mastermeister5.query.service.QueryResult;
import java.util.List;
import java.util.Map;

public record QueryResultResponse(
        List<String> columns, List<Map<String, Object>> rows, int rowCount, long executionTimeMs, boolean truncated) {

    /** nfr-requirements.md Question 1: {@code setMaxRows(1000)} silently caps the result, no exception is thrown. */
    private static final int MAX_ROWS = 1000;

    public static QueryResultResponse from(QueryResult result) {
        return new QueryResultResponse(
                result.columns(), result.rows(), result.rowCount(), result.executionTimeMs(), result.rowCount() >= MAX_ROWS);
    }
}
