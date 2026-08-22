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

package cherry.mastermeister5.mastermaintenance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import cherry.mastermeister5.mastermaintenance.service.CustomizationYamlEntry;
import cherry.mastermeister5.mastermaintenance.service.ImportCustomizationResult;
import cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceService;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能（Unit 2〜4のコントローラ
 * テストと同じ制約）。ここでは request/response mapping と actor-id伝播のみを検証する。
 */
@WebMvcTest(
        controllers = CustomizationController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class CustomizationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MasterMaintenanceService masterMaintenanceService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken adminAuthentication;

    @BeforeEach
    void authenticateAsAdmin() {
        adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void getCustomizationDefinitionReturnsTheStoredDefinition() throws Exception {
        given(masterMaintenanceService.getCustomizationDefinition(1L, "public", "t1"))
                .willReturn(new CustomizationYamlEntry("public", "t1", "name", null, List.of()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/connections/1/customizations/public/t1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tableName", org.hamcrest.Matchers.is("t1")));
    }

    @Test
    void exportCustomizationDefinitionReturnsYamlWithAnAttachmentHeader() throws Exception {
        given(masterMaintenanceService.exportCustomizationDefinition(1L, 1L)).willReturn("tables: []\n");

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/admin/connections/1/customizations/export")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists("Content-Disposition"));
    }

    @Test
    void importCustomizationDefinitionReturnsTheImportedCount() throws Exception {
        given(masterMaintenanceService.importCustomizationDefinition(1L, "tables: []\n", 1L))
                .willReturn(new ImportCustomizationResult(0));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/1/customizations/import")
                                .principal(adminAuthentication)
                                .content("tables: []\n"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.importedTableCount", org.hamcrest.Matchers.is(0)));
    }

    @Test
    void importCustomizationDefinitionPropagatesServiceValidationErrors() throws Exception {
        given(masterMaintenanceService.importCustomizationDefinition(any(), any(), any()))
                .willThrow(cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceException.invalidIdentifier());
        given(
                        errorResponseFactory.create(
                                "MASTER_DATA_INVALID_IDENTIFIER", "errors.master_data_invalid_identifier", null))
                .willReturn(new cherry.mastermeister5.platform.web.ErrorResponse("MASTER_DATA_INVALID_IDENTIFIER", "bad", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/1/customizations/import")
                                .principal(adminAuthentication)
                                .content("tables: []\n"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
