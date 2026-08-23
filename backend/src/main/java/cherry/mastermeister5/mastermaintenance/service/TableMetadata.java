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

/**
 * {@code resolveTableMetadata} return value: everything about a table that
 * the caller needs before/around fetching its record data but that doesn't
 * vary by page/filter/sort — column definitions (visibility, editability,
 * widget) and the caller's resolved table-level aux permissions. Fetched via
 * its own endpoint, separate from both the table listing and record data.
 */
public record TableMetadata(List<ColumnDef> columns, boolean canCreate, boolean canDelete) {
}
