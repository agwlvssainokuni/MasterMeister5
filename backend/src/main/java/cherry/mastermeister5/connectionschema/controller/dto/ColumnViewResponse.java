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

package cherry.mastermeister5.connectionschema.controller.dto;

import cherry.mastermeister5.connectionschema.service.ColumnView;

public record ColumnViewResponse(
        String columnName, String dataType, boolean nullable, boolean primaryKey, String comment) {

    public static ColumnViewResponse from(ColumnView view) {
        return new ColumnViewResponse(
                view.columnName(), view.dataType(), view.nullable(), view.primaryKey(), view.comment());
    }
}
