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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AppThemeRepositoryImpl.class)
class AppThemeRepositoryImplTest {

    @Autowired private AppThemeRepository repository;

    @Test
    void loadReturnsDefaultThemeWhenNoRowExists() {
        assertThat(repository.load()).isEqualTo(AppTheme.defaultTheme());
    }

    @Test
    void saveThenLoadRoundTrips() {
        var theme = new AppTheme(BrandColor.PURPLE, FontFamily.SERIF);

        repository.save(theme);

        assertThat(repository.load()).isEqualTo(theme);
    }

    @Test
    void saveTwiceUpdatesTheSingletonRowRatherThanInserting() {
        repository.save(new AppTheme(BrandColor.BLUE, FontFamily.SANS));
        repository.save(new AppTheme(BrandColor.ORANGE, FontFamily.SERIF));

        assertThat(repository.load())
                .isEqualTo(new AppTheme(BrandColor.ORANGE, FontFamily.SERIF));
    }
}
