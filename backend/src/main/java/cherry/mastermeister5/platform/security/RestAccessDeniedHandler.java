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

package cherry.mastermeister5.platform.security;

import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseFactory errorResponseFactory;
    // Self-instantiated, not Spring-injected: Spring Boot 4's JacksonAutoConfiguration
    // only registers a tools.jackson.databind.ObjectMapper (Jackson 3) bean, not this
    // com.fasterxml.jackson.databind.ObjectMapper (Jackson 2) type — same pattern
    // already used by CustomizationYamlMapper/PermissionYamlMapper/JsonMapConverter.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestAccessDeniedHandler(ErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var errorResponse =
                errorResponseFactory.create("FORBIDDEN", "errors.forbidden", request.getLocale());
        // response.getWriter() uses the Servlet spec's ISO-8859-1 default character
        // encoding unless setCharacterEncoding() is called first, garbling non-ASCII
        // (Japanese) error messages. Writing to the byte OutputStream instead lets
        // Jackson encode as UTF-8 (its own default), sidestepping the Writer pitfall
        // entirely. Found via E2E testing (main-journey.spec.ts) — no test in this
        // project previously asserted on the actual bytes of a security error response.
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
