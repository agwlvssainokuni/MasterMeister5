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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.accesscontrol.entity.PrimaryLevel;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import cherry.mastermeister5.accesscontrol.service.AccessControlException;
import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import cherry.mastermeister5.accesscontrol.service.ImportPermissionsResult;
import cherry.mastermeister5.accesscontrol.service.PermissionEntryView;
import cherry.mastermeister5.accesscontrol.service.SetAuxiliaryPermissionCommand;
import cherry.mastermeister5.accesscontrol.service.SetPrimaryPermissionCommand;
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
 * ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能（Unit 2/3のコントローラ
 * テストと同じ制約）。ここでは request/response mapping と actor-id伝播のみを検証する。
 */
@WebMvcTest(
        controllers = PermissionController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AccessControlService accessControlService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken adminAuthentication;

    @BeforeEach
    void authenticateAsAdmin() {
        adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listPermissionEntriesReturnsExistingSettings() throws Exception {
        given(accessControlService.listPermissionEntries(1L, SubjectType.USER, 5L))
                .willReturn(
                        List.of(
                                new PermissionEntryView(
                                        SubjectType.USER,
                                        5L,
                                        ResourceLevel.SCHEMA,
                                        "public",
                                        null,
                                        null,
                                        PrimaryLevel.READ,
                                        null,
                                        null)));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/admin/permissions")
                                .param("connectionId", "1")
                                .param("subjectType", "USER")
                                .param("subjectId", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].primaryLevel", org.hamcrest.Matchers.is("READ")));
    }

    @Test
    void setPrimaryPermissionPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/permissions/primary")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"subjectType\":\"USER\",\"subjectId\":5,\"connectionId\":1,"
                                                + "\"resourceLevel\":\"SCHEMA\",\"schemaName\":\"public\","
                                                + "\"primaryLevel\":\"READ\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService)
                .setPrimaryPermission(
                        eq(
                                new SetPrimaryPermissionCommand(
                                        SubjectType.USER,
                                        5L,
                                        1L,
                                        ResourceLevel.SCHEMA,
                                        "public",
                                        null,
                                        null,
                                        PrimaryLevel.READ)),
                        eq(1L));
    }

    @Test
    void setAuxiliaryPermissionPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/permissions/auxiliary")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"subjectType\":\"GROUP\",\"subjectId\":9,\"connectionId\":1,"
                                                + "\"resourceLevel\":\"TABLE\",\"schemaName\":\"public\","
                                                + "\"tableName\":\"t1\",\"auxCreate\":true,\"auxDelete\":false}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService)
                .setAuxiliaryPermission(
                        eq(
                                new SetAuxiliaryPermissionCommand(
                                        SubjectType.GROUP, 9L, 1L, ResourceLevel.TABLE, "public", "t1", true, false)),
                        eq(1L));
    }

    @Test
    void exportPermissionsReturnsYamlWithAnAttachmentHeader() throws Exception {
        given(accessControlService.exportPermissions(1L, 1L)).willReturn("entries: []\n");

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/admin/connections/1/permissions/export")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists("Content-Disposition"));
    }

    @Test
    void importPermissionsReturnsTheImportedCount() throws Exception {
        given(accessControlService.importPermissions(1L, "entries: []\n", 1L))
                .willReturn(new ImportPermissionsResult(0));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/1/permissions/import")
                                .principal(adminAuthentication)
                                .content("entries: []\n"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.importedCount", org.hamcrest.Matchers.is(0)));
    }

    @Test
    void importPermissionsReturnsBadRequestOnDuplicateEntries() throws Exception {
        given(accessControlService.importPermissions(any(), any(), any()))
                .willThrow(AccessControlException.duplicateEntry());
        given(
                        errorResponseFactory.create(
                                "PERMISSION_DUPLICATE_ENTRY", "errors.permission_duplicate_entry", null))
                .willReturn(new ErrorResponse("PERMISSION_DUPLICATE_ENTRY", "dup", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/1/permissions/import")
                                .principal(adminAuthentication)
                                .content("entries: []\n"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
