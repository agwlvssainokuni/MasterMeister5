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

package cherry.mastermeister5.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import cherry.mastermeister5.audit.AuditEvent;
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能（Unit 2〜5のコントローラ
 * テストと同じ制約）。ここでは request/response mapping のみを検証する。
 */
@WebMvcTest(
        controllers = AuditLogController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern = "cherry\\.mastermeister5\\.platform\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    private UsernamePasswordAuthenticationToken adminAuthentication;

    @BeforeEach
    void authenticateAsAdmin() {
        adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void listEventsReturnsThePageContent() throws Exception {
        var event = new AuditEvent(AuditEventType.LOGIN_SUCCEEDED, 1L, 1L, Map.of("ip", "127.0.0.1"), "corr-1");
        given(auditLogService.listEvents(any(), any())).willReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/audit-events").principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.content[0].eventType", org.hamcrest.Matchers.is("LOGIN_SUCCEEDED")));
    }

    @Test
    void listEventsAppliesTheFilterQueryParameters() throws Exception {
        given(auditLogService.listEvents(any(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/admin/audit-events")
                                .param("eventType", "LOGIN_SUCCEEDED")
                                .param("actorUserId", "1")
                                .principal(adminAuthentication))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
