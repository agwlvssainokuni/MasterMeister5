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

package cherry.mastermeister5.connectionschema.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.connectionschema.entity.ConnectionStatus;
import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.service.ConnectionException;
import cherry.mastermeister5.connectionschema.service.ConnectionSchemaService;
import cherry.mastermeister5.connectionschema.service.ConnectionSummary;
import cherry.mastermeister5.connectionschema.service.RegisterConnectionCommand;
import cherry.mastermeister5.connectionschema.service.SchemaImportResult;
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
 * ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能（Unit 2の
 * AdminUserControllerTestと同じ制約）。ここでは request/response mapping と
 * actor-id伝播のみを検証する。
 */
@WebMvcTest(
        controllers = ConnectionController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class ConnectionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ConnectionSchemaService connectionSchemaService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken adminAuthentication;

    @BeforeEach
    void authenticateAsAdmin() {
        adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listConnectionsReturnsAllConnections() throws Exception {
        given(connectionSchemaService.listConnections())
                .willReturn(
                        List.of(
                                new ConnectionSummary(
                                        1L,
                                        "conn1",
                                        RdbmsType.MYSQL,
                                        "localhost",
                                        3306,
                                        "db",
                                        ConnectionStatus.ACTIVE)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/connections"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", org.hamcrest.Matchers.is("conn1")));
    }

    @Test
    void registerConnectionPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"conn1\",\"rdbmsType\":\"MYSQL\",\"host\":\"localhost\","
                                                + "\"port\":3306,\"databaseName\":\"db\",\"username\":\"user\","
                                                + "\"password\":\"pass\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(connectionSchemaService)
                .registerConnection(
                        org.mockito.ArgumentMatchers.any(RegisterConnectionCommand.class),
                        org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    void registerConnectionReturnsConflictForADuplicateName() throws Exception {
        org.mockito.BDDMockito.willThrow(ConnectionException.nameAlreadyExists())
                .given(connectionSchemaService)
                .registerConnection(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        given(
                        errorResponseFactory.create(
                                "CONNECTION_NAME_ALREADY_EXISTS", "errors.connection_name_already_exists", null))
                .willReturn(new ErrorResponse("CONNECTION_NAME_ALREADY_EXISTS", "taken", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections")
                                .principal(adminAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"conn1\",\"rdbmsType\":\"MYSQL\",\"host\":\"localhost\","
                                                + "\"port\":3306,\"databaseName\":\"db\",\"username\":\"user\","
                                                + "\"password\":\"pass\"}"))
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @Test
    void deactivateConnectionPassesTheAuthenticatedAdminAsActor() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/2/deactivate")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(connectionSchemaService).deactivateConnection(2L, 1L);
    }

    @Test
    void schemaImportReturnsTheResultSummary() throws Exception {
        given(connectionSchemaService.importSchema(3L, 1L))
                .willReturn(new SchemaImportResult(1, 2, 5, List.of("public.old"), List.of(), List.of()));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/admin/connections/3/schema-import")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.tablesImported", org.hamcrest.Matchers.is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.removedTableRefs[0]", org.hamcrest.Matchers.is("public.old")));
    }
}
