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

package cherry.mastermeister5.mastermaintenance.service;

import java.util.List;

/** component-methods.md: MasterMaintenanceComponent. */
public interface MasterMaintenanceService {

    List<TableSummary> listTables(Long connectionId, Long userId);

    RecordPage listRecords(ListRecordsCommand command);

    ApplyResult applyChanges(
            Long connectionId, String schemaName, String tableName, Long userId, RecordChangeSet changeSet);

    CustomizationYamlEntry getCustomizationDefinition(Long connectionId, String schemaName, String tableName);

    String exportCustomizationDefinition(Long connectionId, Long actorUserId);

    ImportCustomizationResult importCustomizationDefinition(Long connectionId, String yamlContent, Long actorUserId);
}
