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

import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.platform.security.AuthCookieSupport;
import cherry.mastermeister5.platform.security.JwtTokenProvider;
import cherry.mastermeister5.platform.security.RefreshTokenReuseDetectedException;
import cherry.mastermeister5.platform.security.RefreshTokenService;
import cherry.mastermeister5.useraccount.controller.dto.AuthenticatedUserResponse;
import cherry.mastermeister5.useraccount.controller.dto.LoginRequest;
import cherry.mastermeister5.useraccount.controller.dto.LoginResponse;
import cherry.mastermeister5.useraccount.controller.dto.RefreshResponse;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * nfr-design-plan.md Question 2: calls {@link UserAccountService} directly
 * rather than going through Spring Security's AuthenticationManager.
 */
@RestController
public class AuthController {

    private final UserAccountService userAccountService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieSupport authCookieSupport;
    private final AuditLogService auditLogService;

    public AuthController(
            UserAccountService userAccountService,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            AuthCookieSupport authCookieSupport,
            AuditLogService auditLogService) {
        this.userAccountService = userAccountService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.authCookieSupport = authCookieSupport;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var user = userAccountService.authenticate(request.email(), request.password());
        var accessToken = jwtTokenProvider.issueAccessToken(user.userId(), user.role().name());
        var refreshToken = refreshTokenService.issue(user.userId());
        authCookieSupport.setRefreshTokenCookie(
                response, refreshToken.rawToken(), refreshToken.expiresAt());
        return new LoginResponse(accessToken, AuthenticatedUserResponse.from(user));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            HttpServletRequest request, HttpServletResponse response) {
        var rawToken = authCookieSupport.readRefreshTokenCookie(request);
        if (rawToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        try {
            var rotated = refreshTokenService.rotate(rawToken.get());
            if (rotated.isEmpty()) {
                authCookieSupport.clearRefreshTokenCookie(response);
                return ResponseEntity.status(401).build();
            }
            var issued = rotated.get();
            var user = userAccountService.getAuthenticatedUser(issued.userId());
            var accessToken = jwtTokenProvider.issueAccessToken(user.userId(), user.role().name());
            authCookieSupport.setRefreshTokenCookie(response, issued.rawToken(), issued.expiresAt());
            return ResponseEntity.ok(
                    new RefreshResponse(accessToken, AuthenticatedUserResponse.from(user)));
        } catch (RefreshTokenReuseDetectedException e) {
            authCookieSupport.clearRefreshTokenCookie(response);
            auditLogService.recordEvent(
                    AuditEventType.REFRESH_TOKEN_REUSE_DETECTED, null, e.getUserId(), Map.of());
            return ResponseEntity.status(401).build();
        } catch (UserAccountException e) {
            // The user backing this refresh token no longer exists / is not
            // resolvable (e.g. deleted between issue and refresh) — fail closed.
            authCookieSupport.clearRefreshTokenCookie(response);
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieSupport
                .readRefreshTokenCookie(request)
                .ifPresent(
                        rawToken ->
                                refreshTokenService
                                        .revoke(rawToken)
                                        .ifPresent(
                                                userId ->
                                                        auditLogService.recordEvent(
                                                                AuditEventType.LOGOUT, userId, userId, Map.of())));
        authCookieSupport.clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
