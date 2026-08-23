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
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.connectionschema.entity.DbTable;
import cherry.mastermeister5.mastermaintenance.service.ApplyResult;
import cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceException;
import cherry.mastermeister5.mastermaintenance.service.MasterMaintenanceService;
import cherry.mastermeister5.mastermaintenance.service.RecordPage;
import cherry.mastermeister5.mastermaintenance.service.TableMetadata;
import cherry.mastermeister5.mastermaintenance.service.TableSummary;
import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
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
 * Unlike Unit 2/3/4's ADMIN-only controllers, US-3.1〜US-3.6 are available to
 * any authenticated user (SecurityConfig's {@code anyRequest().authenticated()}
 * fallback) — there is no role to assert here, only actor-id propagation and
 * response mapping.
 */
@WebMvcTest(
        controllers = MasterDataController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class MasterDataControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MasterMaintenanceService masterMaintenanceService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken userAuthentication;

    @BeforeEach
    void authenticateAsAGeneralUser() {
        userAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "9", null, List.of(new SimpleGrantedAuthority("ROLE_GENERAL")));
    }

    @Test
    void listTablesPassesTheAuthenticatedUserAsActor() throws Exception {
        given(masterMaintenanceService.listTables(1L, 9L))
                .willReturn(List.of(new TableSummary("public", "t1", DbTable.Type.TABLE, null)));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/data/connections/1/tables").principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].tableName", org.hamcrest.Matchers.is("t1")));
    }

    @Test
    void listRecordsPassesTheAuthenticatedUserAsActor() throws Exception {
        given(masterMaintenanceService.listRecords(any())).willReturn(new RecordPage(List.of(), 0, 50, 0));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/data/connections/1/tables/public/t1/records")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"page\":0,\"pageSize\":50}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(masterMaintenanceService)
                .listRecords(
                        org.mockito.ArgumentMatchers.argThat(
                                command -> command.connectionId().equals(1L) && command.userId().equals(9L)));
    }

    @Test
    void getTableMetadataPassesTheAuthenticatedUserAsActor() throws Exception {
        given(masterMaintenanceService.resolveTableMetadata(1L, "public", "t1", 9L))
                .willReturn(new TableMetadata(List.of(), List.of("id"), true, false));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/data/connections/1/tables/public/t1/metadata")
                                .principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.canCreate", org.hamcrest.Matchers.is(true)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.canDelete", org.hamcrest.Matchers.is(false)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryKeyColumns[0]", org.hamcrest.Matchers.is("id")));
    }

    @Test
    void applyChangesPassesTheAuthenticatedUserAsActor() throws Exception {
        given(masterMaintenanceService.applyChanges(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("public"), org.mockito.ArgumentMatchers.eq("t1"), org.mockito.ArgumentMatchers.eq(9L), any()))
                .willReturn(new ApplyResult(0, 1, 0));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/data/connections/1/tables/public/t1/apply")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"changes\":[{\"operation\":\"UPDATE\",\"primaryKeyValues\":{\"id\":1},"
                                                + "\"columnValues\":{\"name\":\"Alicia\"}}]}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.updatedCount", org.hamcrest.Matchers.is(1)));
    }

    @Test
    void applyChangesReturnsForbiddenWhenPermissionIsDenied() throws Exception {
        given(masterMaintenanceService.applyChanges(any(), any(), any(), any(), any()))
                .willThrow(MasterMaintenanceException.permissionDenied());
        given(errorResponseFactory.create("MASTER_DATA_PERMISSION_DENIED", "errors.master_data_permission_denied", null))
                .willReturn(new ErrorResponse("MASTER_DATA_PERMISSION_DENIED", "denied", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/data/connections/1/tables/public/t1/apply")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"changes\":[{\"operation\":\"DELETE\",\"primaryKeyValues\":{\"id\":1}}]}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}
