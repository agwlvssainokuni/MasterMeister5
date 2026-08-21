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

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.platform.security.AuthCookieSupport;
import cherry.mastermeister5.platform.security.IssuedRefreshToken;
import cherry.mastermeister5.platform.security.JwtTokenProvider;
import cherry.mastermeister5.platform.security.RefreshTokenReuseDetectedException;
import cherry.mastermeister5.platform.security.RefreshTokenService;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import cherry.mastermeister5.useraccount.entity.UserRole;
import cherry.mastermeister5.useraccount.service.AuthenticatedUser;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/** Excludes {@code platform.security} for the same reason as Unit 1's AppThemeControllerTest. */
@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private RefreshTokenService refreshTokenService;
    @MockitoBean private AuthCookieSupport authCookieSupport;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    @Test
    void loginReturnsAnAccessTokenAndSetsTheRefreshCookie() throws Exception {
        var user = new AuthenticatedUser(1L, "user@example.com", "Taro", UserRole.GENERAL);
        given(userAccountService.authenticate("user@example.com", "correctHorseBattery1"))
                .willReturn(user);
        given(jwtTokenProvider.issueAccessToken(1L, "GENERAL")).willReturn("access-token");
        given(refreshTokenService.issue(1L))
                .willReturn(new IssuedRefreshToken(1L, "refresh-token", Instant.now().plusSeconds(3600)));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken", is("access-token")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.email", is("user@example.com")));

        verify(authCookieSupport)
                .setRefreshTokenCookie(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("refresh-token"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginRejectsWrongCredentialsWithAGenericError() throws Exception {
        given(userAccountService.authenticate("user@example.com", "wrong"))
                .willThrow(UserAccountException.authenticationFailed());
        given(errorResponseFactory.create("AUTHENTICATION_FAILED", "errors.authentication_failed", null))
                .willReturn(
                        new cherry.mastermeister5.platform.web.ErrorResponse(
                                "AUTHENTICATION_FAILED", "invalid", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void refreshWithoutACookieReturns401() throws Exception {
        given(authCookieSupport.readRefreshTokenCookie(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void refreshOnReuseDetectionClearsTheCookieAndRecordsAnAuditEvent() throws Exception {
        given(authCookieSupport.readRefreshTokenCookie(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.of("stolen-token"));
        given(refreshTokenService.rotate("stolen-token"))
                .willThrow(new RefreshTokenReuseDetectedException(1L));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/refresh"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        verify(authCookieSupport).clearRefreshTokenCookie(org.mockito.ArgumentMatchers.any());
        verify(auditLogService)
                .recordEvent(
                        org.mockito.ArgumentMatchers.eq(
                                cherry.mastermeister5.audit.AuditEventType.REFRESH_TOKEN_REUSE_DETECTED),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void logoutClearsTheCookieRegardlessOfWhetherATokenWasPresent() throws Exception {
        given(authCookieSupport.readRefreshTokenCookie(org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        verify(authCookieSupport).clearRefreshTokenCookie(org.mockito.ArgumentMatchers.any());
    }
}
