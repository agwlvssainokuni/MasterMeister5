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

import cherry.mastermeister5.connectionschema.entity.DbTable;

/**
 * frontend-components.md MasterDataScreen table selector (US-3.1).
 * {@code canCreate}/{@code canDelete} are the resolved table-level aux
 * permissions, fetched here as metadata alongside the table listing rather
 * than with every record page (they don't vary by page/filter/sort).
 */
public record TableSummary(
        String schemaName, String tableName, DbTable.Type tableType, String comment, boolean canCreate, boolean canDelete) {
}
