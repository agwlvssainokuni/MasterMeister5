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

package cherry.mastermeister5.platform.theme;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

import cherry.mastermeister5.platform.web.ErrorResponse;
import cherry.mastermeister5.platform.web.ErrorResponseFactory;
import cherry.mastermeister5.platform.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Security filters are disabled here (authorization is exercised at the
 * integration-test level, see Build and Test); this test focuses purely on
 * request/response mapping. {@link GlobalExceptionHandler} is picked up
 * automatically by the {@code @WebMvcTest} slice, so its dependency is
 * stubbed rather than left unresolved.
 */
@WebMvcTest(AppThemeController.class)
@AutoConfigureMockMvc(addFilters = false)
class AppThemeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AppThemeService appThemeService;

    @MockitoBean private ErrorResponseFactory errorResponseFactory;

    @Test
    void getThemeReturnsCurrentTheme() throws Exception {
        given(appThemeService.getAppTheme())
                .willReturn(new AppTheme(BrandColor.GREEN, FontFamily.SERIF));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/theme"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.brandColor", is("GREEN")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.fontFamily", is("SERIF")));
    }

    @Test
    void updateThemeRejectsMissingFields() throws Exception {
        given(errorResponseFactory.create("VALIDATION_ERROR", "errors.validation", null))
                .willReturn(new ErrorResponse("VALIDATION_ERROR", "invalid", null));

        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/theme")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
