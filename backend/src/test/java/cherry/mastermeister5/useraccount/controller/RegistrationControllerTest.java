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

import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import cherry.mastermeister5.useraccount.service.UserAccountException;
import cherry.mastermeister5.useraccount.service.UserAccountService;
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

@WebMvcTest(
        controllers = RegistrationController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    @Test
    void registerSucceedsWithAValidToken() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"token\":\"tok\",\"name\":\"Taro\",\"password\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void registerReturnsBadRequestForAnExpiredToken() throws Exception {
        given(userAccountService.completeRegistration("tok", "Taro", "correctHorseBattery1"))
                .willThrow(UserAccountException.invitationTokenExpired());
        given(
                        errorResponseFactory.create(
                                "INVITATION_TOKEN_EXPIRED", "errors.invitation_token_expired", null))
                .willReturn(new ErrorResponse("INVITATION_TOKEN_EXPIRED", "expired", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"token\":\"tok\",\"name\":\"Taro\",\"password\":\"correctHorseBattery1\"}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void registerRejectsMissingFields() throws Exception {
        given(errorResponseFactory.create("VALIDATION_ERROR", "errors.validation", null))
                .willReturn(new ErrorResponse("VALIDATION_ERROR", "invalid", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
