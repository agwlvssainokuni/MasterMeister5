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

import cherry.mastermeister5.useraccount.controller.dto.ChangePasswordRequest;
import cherry.mastermeister5.useraccount.controller.dto.PasswordResetRequest;
import cherry.mastermeister5.useraccount.controller.dto.PasswordResetRequestRequest;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordController {

    private final UserAccountService userAccountService;

    public PasswordController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/api/auth/password/reset-request")
    public void requestReset(
            @Valid @RequestBody PasswordResetRequestRequest request, HttpServletRequest httpRequest) {
        // BR-23: the response is identical regardless of outcome, so the
        // service call's result is intentionally not inspected here.
        userAccountService.requestPasswordReset(request.email(), httpRequest.getLocale());
    }

    @PostMapping("/api/auth/password/reset")
    public void reset(@Valid @RequestBody PasswordResetRequest request) {
        userAccountService.resetPassword(request.token(), request.newPassword());
    }

    @PutMapping("/api/account/password")
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        var userId = Long.valueOf(authentication.getName());
        userAccountService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
}
