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
import cherry.mastermeister5.useraccount.service.UserAccountException;
import cherry.mastermeister5.useraccount.service.UserAccountService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(
        controllers = PasswordController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class PasswordControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resetRequestAlwaysReturnsOkRegardlessOfWhetherTheEmailExists() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/password/reset-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void resetSucceedsWithAValidToken() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/password/reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"tok\",\"newPassword\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void changePasswordUsesTheAuthenticatedUserId() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "42", null, List.of(new SimpleGrantedAuthority("ROLE_GENERAL"))));

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/account/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"old\",\"newPassword\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(userAccountService).changePassword(42L, "old", "correctHorseBattery1");
    }

    @Test
    void changePasswordRejectsAWrongCurrentPassword() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "42", null, List.of(new SimpleGrantedAuthority("ROLE_GENERAL"))));
        org.mockito.BDDMockito.willThrow(UserAccountException.currentPasswordMismatch())
                .given(userAccountService)
                .changePassword(42L, "wrong", "correctHorseBattery1");
        given(
                        errorResponseFactory.create(
                                "CURRENT_PASSWORD_MISMATCH", "errors.current_password_mismatch", null))
                .willReturn(new ErrorResponse("CURRENT_PASSWORD_MISMATCH", "mismatch", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/account/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"wrong\",\"newPassword\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
