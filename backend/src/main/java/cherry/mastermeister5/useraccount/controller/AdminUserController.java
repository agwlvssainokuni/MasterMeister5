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

package cherry.mastermeister5.useraccount.controller;

import cherry.mastermeister5.useraccount.controller.dto.ChangeRoleRequest;
import cherry.mastermeister5.useraccount.controller.dto.InviteUserRequest;
import cherry.mastermeister5.useraccount.controller.dto.UserSummaryResponse;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * SECURITY-08: {@code /api/admin/**} is restricted to ADMIN at the
 * SecurityConfig path-matcher level (primary control); {@code @PreAuthorize}
 * here is defense in depth, mirroring AppThemeService's pattern from Unit 1.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAccountService userAccountService;

    public AdminUserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/api/admin/users")
    public List<UserSummaryResponse> listUsers() {
        return userAccountService.listUsers().stream().map(UserSummaryResponse::from).toList();
    }

    @PostMapping("/api/admin/users/invitations")
    public void inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        var actorUserId = Long.valueOf(authentication.getName());
        userAccountService.inviteUser(
                request.email(), request.role(), actorUserId, httpRequest.getLocale());
    }

    @PostMapping("/api/admin/users/{userId}/invitations/resend")
    public void resendInvitation(@PathVariable Long userId, HttpServletRequest httpRequest) {
        userAccountService.resendInvitation(userId, httpRequest.getLocale());
    }

    @PutMapping("/api/admin/users/{userId}/role")
    public void changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        userAccountService.changeRole(userId, request.role(), actorUserId);
    }

    @PostMapping("/api/admin/users/{userId}/deactivate")
    public void deactivateUser(@PathVariable Long userId, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        userAccountService.deactivateUser(userId, actorUserId);
    }

    @PostMapping("/api/admin/users/{userId}/reactivate")
    public void reactivateUser(@PathVariable Long userId, Authentication authentication) {
        var actorUserId = Long.valueOf(authentication.getName());
        userAccountService.reactivateUser(userId, actorUserId);
    }
}
