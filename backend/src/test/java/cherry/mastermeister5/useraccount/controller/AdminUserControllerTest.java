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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import cherry.mastermeister5.useraccount.entity.UserRole;
import cherry.mastermeister5.useraccount.entity.UserStatus;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import cherry.mastermeister5.useraccount.service.UserSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * {@code @PreAuthorize} enforcement needs the method-security AOP proxy from
 * {@code SecurityConfig}, which this slice does not load — the same
 * limitation documented on Unit 1's AppThemeServiceImplTest. ADMIN-only
 * enforcement is exercised at the Build and Test integration level instead;
 * this class covers request/response mapping and actor-id propagation only.
 */
@WebMvcTest(
        controllers = AdminUserController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken adminAuthentication;

    @BeforeEach
    void authenticateAsAdmin() {
        adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listUsersReturnsAllUsers() throws Exception {
        given(userAccountService.listUsers())
                .willReturn(
                        List.of(
                                new UserSummary(
                                        2L,
                                        "u@example.com",
                                        "Taro",
                                        UserRole.GENERAL,
                                        UserStatus.ACTIVE,
                                        null,
                                        Instant.now())));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/users"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email", org.hamcrest.Matchers.is("u@example.com")));
    }

    @Test
    void inviteUserPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/users/invitations")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"new@example.com\",\"role\":\"GENERAL\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(userAccountService)
                .inviteUser(
                        org.mockito.ArgumentMatchers.eq("new@example.com"),
                        org.mockito.ArgumentMatchers.eq(UserRole.GENERAL),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inviteUserReturnsConflictForADuplicateEmail() throws Exception {
        org.mockito.BDDMockito.willThrow(UserAccountException.emailAlreadyRegistered())
                .given(userAccountService)
                .inviteUser(
                        org.mockito.ArgumentMatchers.eq("taken@example.com"),
                        org.mockito.ArgumentMatchers.eq(UserRole.GENERAL),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any());
        given(errorResponseFactory.create("EMAIL_ALREADY_REGISTERED", "errors.email_already_registered", null))
                .willReturn(new ErrorResponse("EMAIL_ALREADY_REGISTERED", "taken", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/users/invitations")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"taken@example.com\",\"role\":\"GENERAL\"}"))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void deactivateUserPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/users/2/deactivate")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(userAccountService).deactivateUser(2L, 1L);
    }

    @Test
    void changeRolePassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/admin/users/2/role")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(userAccountService).changeRole(2L, UserRole.ADMIN, 1L);
    }
}
