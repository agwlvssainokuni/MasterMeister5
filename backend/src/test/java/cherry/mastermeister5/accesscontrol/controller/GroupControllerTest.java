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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.accesscontrol.service.AccessControlService;
import cherry.mastermeister5.accesscontrol.service.GroupMemberView;
import cherry.mastermeister5.accesscontrol.service.GroupSummary;
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
        controllers = GroupController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

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
    void listGroupsReturnsAllGroups() throws Exception {
        given(accessControlService.listGroups()).willReturn(List.of(new GroupSummary(1L, "sales", 2L)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/groups"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", org.hamcrest.Matchers.is("sales")));
    }

    @Test
    void createGroupPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/groups")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"sales\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService).createGroup("sales", 1L);
    }

    @Test
    void renameGroupPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.patch("/api/admin/groups/2")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"new-name\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService).renameGroup(2L, "new-name", 1L);
    }

    @Test
    void deleteGroupPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/groups/2").principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService).deleteGroup(2L, 1L);
    }

    @Test
    void listMembersReturnsGroupMembers() throws Exception {
        given(accessControlService.listMembers(2L))
                .willReturn(List.of(new GroupMemberView(10L, "a@example.com", "Alice")));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/groups/2/members"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].email", org.hamcrest.Matchers.is("a@example.com")));
    }

    @Test
    void addMemberPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/groups/2/members")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"userId\":10}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService).addUserToGroup(2L, 10L, 1L);
    }

    @Test
    void removeMemberPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/api/admin/groups/2/members/10")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(accessControlService).removeUserFromGroup(2L, 10L, 1L);
    }
}
