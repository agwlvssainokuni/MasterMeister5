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

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET is available to every authenticated user (brand color / font apply to
 * all users); PUT is restricted to administrators via
 * {@link AppThemeServiceImpl#setAppTheme}.
 */
@RestController
public class AppThemeController {

    private final AppThemeService appThemeService;

    public AppThemeController(AppThemeService appThemeService) {
        this.appThemeService = appThemeService;
    }

    @GetMapping("/api/theme")
    public AppThemeResponse getTheme() {
        return AppThemeResponse.from(appThemeService.getAppTheme());
    }

    @PutMapping("/api/theme")
    public AppThemeResponse updateTheme(@Valid @RequestBody AppThemeRequest request) {
        var theme = request.toDomain();
        appThemeService.setAppTheme(theme);
        return AppThemeResponse.from(theme);
    }
}
