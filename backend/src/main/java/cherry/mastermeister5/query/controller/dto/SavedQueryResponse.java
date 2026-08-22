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

import cherry.mastermeister5.query.entity.QueryStatus;
import cherry.mastermeister5.query.entity.QueryVisibility;
import cherry.mastermeister5.query.entity.SavedQuery;
import java.time.Instant;

public record SavedQueryResponse(
        Long id,
        String name,
        String sqlText,
        QueryVisibility visibility,
        Long creatorUserId,
        QueryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static SavedQueryResponse from(SavedQuery savedQuery) {
        return new SavedQueryResponse(
                savedQuery.getId(),
                savedQuery.getName(),
                savedQuery.getSqlText(),
                savedQuery.getVisibility(),
                savedQuery.getCreatorUserId(),
                savedQuery.getStatus(),
                savedQuery.getCreatedAt(),
                savedQuery.getUpdatedAt());
    }
}
