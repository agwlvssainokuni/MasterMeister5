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
import cherry.mastermeister5.mastermaintenance.service.CustomizationYamlColumn;
import cherry.mastermeister5.mastermaintenance.service.CustomizationYamlEntry;
import java.util.List;

public record CustomizationDefinitionResponse(
        String schemaName,
        String tableName,
        String defaultSortColumn,
        SortDirection defaultSortDirection,
        List<CustomizationYamlColumn> columns) {

    public static CustomizationDefinitionResponse from(CustomizationYamlEntry entry) {
        return new CustomizationDefinitionResponse(
                entry.schemaName(), entry.tableName(), entry.defaultSortColumn(), entry.defaultSortDirection(), entry.columns());
    }
}
