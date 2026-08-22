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

package cherry.mastermeister5.query.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import cherry.mastermeister5.query.entity.QueryExecutionHistory;
import cherry.mastermeister5.query.entity.QueryVisibility;
import cherry.mastermeister5.query.service.ParameterDescriptor;
import cherry.mastermeister5.query.service.QueryException;
import cherry.mastermeister5.query.service.QueryResult;
import cherry.mastermeister5.query.service.QueryService;
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
 * Unlike Unit 2〜4's ADMIN-only controllers, US-4.1〜US-4.6 are available to
 * any authenticated user (SecurityConfig's {@code anyRequest().authenticated()}
 * fallback) — there is no role to assert here, only actor-id propagation and
 * response mapping.
 */
@WebMvcTest(
        controllers = QueryController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class QueryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private QueryService queryService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken userAuthentication;

    @BeforeEach
    void authenticateAsAGeneralUser() {
        userAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "9", null, List.of(new SimpleGrantedAuthority("ROLE_GENERAL")));
    }

    @Test
    void saveQueryPassesTheAuthenticatedUserAsActor() throws Exception {
        given(queryService.saveQuery(eq("q1"), eq("SELECT 1"), eq(QueryVisibility.PUBLIC), eq(null), eq(9L)))
                .willReturn(10L);

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/query/saved-queries")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"q1\",\"sqlText\":\"SELECT 1\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.savedQueryId", org.hamcrest.Matchers.is(10)));
    }

    @Test
    void retireQueryPassesTheAuthenticatedUserAsActor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/query/saved-queries/10").principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        verify(queryService).retireQuery(10L, 9L);
    }

    @Test
    void retireQueryReturnsForbiddenWhenPermissionIsDenied() throws Exception {
        org.mockito.Mockito.doThrow(QueryException.permissionDenied()).when(queryService).retireQuery(10L, 9L);
        given(errorResponseFactory.create("QUERY_PERMISSION_DENIED", "errors.query_permission_denied", null))
                .willReturn(new ErrorResponse("QUERY_PERMISSION_DENIED", "denied", null));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/query/saved-queries/10").principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void detectParametersReturnsTheDetectedNames() throws Exception {
        given(queryService.detectParameters("SELECT * FROM t1 WHERE id = :id"))
                .willReturn(List.of(new ParameterDescriptor("id")));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/query/detect-parameters")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sqlText\":\"SELECT * FROM t1 WHERE id = :id\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", org.hamcrest.Matchers.is("id")));
    }

    @Test
    void executeQueryPassesTheAuthenticatedUserAsActor() throws Exception {
        given(queryService.executeQuery(eq("SELECT 1"), eq(null), eq(1L), eq("PUBLIC"), any(), eq(9L)))
                .willReturn(new QueryResult(List.of("1"), List.of(), 0, 5L));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/query/execute")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sqlText\":\"SELECT 1\",\"connectionId\":1,\"schemaName\":\"PUBLIC\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.rowCount", org.hamcrest.Matchers.is(0)));
    }

    @Test
    void executeQueryReturnsBadRequestWhenSqlIsUnsafe() throws Exception {
        given(queryService.executeQuery(any(), any(), any(), any(), any(), any()))
                .willThrow(QueryException.unsafeSql());
        given(errorResponseFactory.create("QUERY_UNSAFE_SQL", "errors.query_unsafe_sql", null))
                .willReturn(new ErrorResponse("QUERY_UNSAFE_SQL", "unsafe", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/query/execute")
                                .principal(userAuthentication)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"sqlText\":\"DELETE FROM t1\",\"connectionId\":1,\"schemaName\":\"PUBLIC\"}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void listExecutionHistoryReturnsThePageContent() throws Exception {
        var history = new QueryExecutionHistory(null, "SELECT 1", 1L, "PUBLIC", java.util.Map.of(), 1, 5L, 9L);
        given(queryService.listExecutionHistory(any(), any())).willReturn(new org.springframework.data.domain.PageImpl<>(List.of(history)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/query/execution-history").principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].sqlText", org.hamcrest.Matchers.is("SELECT 1")));
    }

    @Test
    void listSavedQueriesPassesTheAuthenticatedUserAsActor() throws Exception {
        given(queryService.listSavedQueries(9L)).willReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/query/saved-queries").principal(userAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
