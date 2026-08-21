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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Authorization enforcement (@PreAuthorize) requires the Spring AOP proxy and is
 * verified at the controller/integration test level (Step 6), not here.
 */
@ExtendWith(MockitoExtension.class)
class AppThemeServiceImplTest {

    @Mock
    private AppThemeRepository repository;

    @Test
    void getAppThemeDelegatesToRepository() {
        var stored = new AppTheme(BrandColor.GREEN, FontFamily.SERIF);
        when(repository.load()).thenReturn(stored);

        var service = new AppThemeServiceImpl(repository);

        assertThat(service.getAppTheme()).isEqualTo(stored);
    }

    @Test
    void setAppThemeDelegatesToRepository() {
        var service = new AppThemeServiceImpl(repository);
        var theme = new AppTheme(BrandColor.PURPLE, FontFamily.SANS);

        service.setAppTheme(theme);

        verify(repository).save(theme);
    }
}
