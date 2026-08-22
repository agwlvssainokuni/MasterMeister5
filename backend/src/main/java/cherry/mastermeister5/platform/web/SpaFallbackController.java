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

package cherry.mastermeister5.platform.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the bundled SPA's {@code index.html} directly (streamed, not
 * {@code forward:}ed) for any non-{@code /api/**}, non-{@code /assets/**}
 * path, so React Router can render it client-side.
 *
 * <p>Spring Boot's "welcome page" convention only serves {@code index.html}
 * for the exact root path {@code /} — a fresh top-level navigation to any
 * other frontend route (e.g. a bookmark, a browser refresh, or the
 * invitation/password-reset link a user clicks from their email client,
 * which is necessarily a fresh navigation, never client-side routing) had no
 * matching handler and fell through to Spring Security's
 * {@code anyRequest().authenticated()}, returning a JSON 401 instead of the
 * app shell. Found by E2E testing (main-journey.spec.ts), which is the only
 * kind of test that ever performs a real top-level navigation instead of
 * driving React Router directly.
 *
 * <p>An earlier version of this fix used {@code forward:/index.html}, but
 * that triggered a {@code StackOverflowError} in Tomcat's own forward-request
 * wrapping (a {@code HttpServletRequestWrapper} ending up wrapping itself) —
 * also only discoverable by an E2E test actually issuing a real top-level
 * navigation, since it is specific to {@code RequestDispatcher.forward()}.
 * Streaming the file's bytes directly sidesteps forwarding entirely.
 */
@Controller
class SpaFallbackController {

    @GetMapping("/{path:^(?!api|assets|index\\.html$).*$}")
    void serveTopLevel(HttpServletResponse response) throws IOException {
        serveIndexHtml(response);
    }

    @GetMapping("/{path:^(?!api|assets).*$}/**")
    void serveNested(HttpServletResponse response) throws IOException {
        serveIndexHtml(response);
    }

    private void serveIndexHtml(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        var resource = new ClassPathResource("static/index.html");
        try (var in = resource.getInputStream()) {
            in.transferTo(response.getOutputStream());
        }
    }
}
