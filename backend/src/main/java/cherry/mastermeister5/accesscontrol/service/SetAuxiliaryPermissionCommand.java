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

package cherry.mastermeister5.accesscontrol.service;

import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;

/** business-rules.md BR-6: auxiliary permissions only apply at SCHEMA/TABLE level. */
public record SetAuxiliaryPermissionCommand(
        SubjectType subjectType,
        Long subjectId,
        Long connectionId,
        ResourceLevel resourceLevel,
        String schemaName,
        String tableName,
        Boolean auxCreate,
        Boolean auxDelete) {
}
