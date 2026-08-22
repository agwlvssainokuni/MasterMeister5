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

import cherry.mastermeister5.accesscontrol.cache.EffectivePermission;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import java.util.List;

/** component-methods.md: AccessControlComponent. */
public interface AccessControlService {

    Long createGroup(String name, Long actorUserId);

    void renameGroup(Long groupId, String name, Long actorUserId);

    void deleteGroup(Long groupId, Long actorUserId);

    void addUserToGroup(Long groupId, Long userId, Long actorUserId);

    void removeUserFromGroup(Long groupId, Long userId, Long actorUserId);

    List<GroupSummary> listGroups();

    List<GroupMemberView> listMembers(Long groupId);

    void setPrimaryPermission(SetPrimaryPermissionCommand command, Long actorUserId);

    void setAuxiliaryPermission(SetAuxiliaryPermissionCommand command, Long actorUserId);

    List<PermissionEntryView> listPermissionEntries(Long connectionId, SubjectType subjectType, Long subjectId);

    EffectivePermission resolveEffectivePermission(
            Long userId, Long connectionId, ResourceLevel resourceLevel, String schemaName, String tableName, String columnName);

    String exportPermissions(Long connectionId, Long actorUserId);

    ImportPermissionsResult importPermissions(Long connectionId, String yamlContent, Long actorUserId);
}
