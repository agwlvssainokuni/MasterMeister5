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

package cherry.mastermeister5.accesscontrol.controller;

import cherry.mastermeister5.accesscontrol.controller.dto.AddGroupMemberRequest;
import cherry.mastermeister5.accesscontrol.controller.dto.CreateGroupRequest;
import cherry.mastermeister5.accesscontrol.controller.dto.GroupMemberResponse;
import cherry.mastermeister5.accesscontrol.controller.dto.GroupSummaryResponse;
import cherry.mastermeister5.accesscontrol.controller.dto.RenameGroupRequest;
import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: {@code /api/admin/**} is restricted to ADMIN at the
 * SecurityConfig path-matcher level; {@code @PreAuthorize} here is defense
 * in depth, mirroring Unit 2/3's controllers.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class GroupController {

    private final AccessControlService accessControlService;

    public GroupController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/api/admin/groups")
    public List<GroupSummaryResponse> listGroups() {
        return accessControlService.listGroups().stream().map(GroupSummaryResponse::from).toList();
    }

    @PostMapping("/api/admin/groups")
    public void createGroup(@Valid @RequestBody CreateGroupRequest request, Authentication authentication) {
        accessControlService.createGroup(request.name(), Long.valueOf(authentication.getName()));
    }

    @PatchMapping("/api/admin/groups/{groupId}")
    public void renameGroup(
            @PathVariable Long groupId, @Valid @RequestBody RenameGroupRequest request, Authentication authentication) {
        accessControlService.renameGroup(groupId, request.name(), Long.valueOf(authentication.getName()));
    }

    @DeleteMapping("/api/admin/groups/{groupId}")
    public void deleteGroup(@PathVariable Long groupId, Authentication authentication) {
        accessControlService.deleteGroup(groupId, Long.valueOf(authentication.getName()));
    }

    @GetMapping("/api/admin/groups/{groupId}/members")
    public List<GroupMemberResponse> listMembers(@PathVariable Long groupId) {
        return accessControlService.listMembers(groupId).stream().map(GroupMemberResponse::from).toList();
    }

    @PostMapping("/api/admin/groups/{groupId}/members")
    public void addMember(
            @PathVariable Long groupId, @Valid @RequestBody AddGroupMemberRequest request, Authentication authentication) {
        accessControlService.addUserToGroup(groupId, request.userId(), Long.valueOf(authentication.getName()));
    }

    @DeleteMapping("/api/admin/groups/{groupId}/members/{userId}")
    public void removeMember(
            @PathVariable Long groupId, @PathVariable Long userId, Authentication authentication) {
        accessControlService.removeUserFromGroup(groupId, userId, Long.valueOf(authentication.getName()));
    }
}
