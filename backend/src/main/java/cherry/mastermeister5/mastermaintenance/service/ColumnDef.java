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

import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.mastermaintenance.entity.InputWidget;

/**
 * business-logic-model.md §1 Step 3-4: the result of merging Unit 4's
 * per-column effective permission with this Unit's customization
 * (BR-14: hidden/readOnly never widen what permission already restricts).
 */
public record ColumnDef(
        String columnName,
        String displayLabel,
        String dataType,
        boolean primaryKey,
        PrimaryLevel effectiveLevel,
        boolean readOnly,
        InputWidget inputWidget,
        String selectOptionsJson) {
}
