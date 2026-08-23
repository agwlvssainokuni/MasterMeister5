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
import java.util.Map;

/**
 * component-methods.md: {@code listRecords} return value. Column
 * definitions are not included here — see {@link TableMetadata}, fetched
 * separately since they don't vary by page/filter/sort.
 */
public record RecordPage(List<Map<String, Object>> rows, int page, int pageSize, long totalCount) {
}
