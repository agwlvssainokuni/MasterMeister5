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

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class AppThemeRepositoryImpl implements AppThemeRepository {

    private final AppThemeJpaRepository jpaRepository;

    AppThemeRepositoryImpl(AppThemeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AppTheme load() {
        return jpaRepository
                .findById(AppThemeEntity.SINGLETON_ID)
                .map(entity -> new AppTheme(entity.getBrandColor(), entity.getFontFamily()))
                .orElseGet(AppTheme::defaultTheme);
    }

    @Override
    @Transactional
    public void save(AppTheme theme) {
        var entity =
                jpaRepository
                        .findById(AppThemeEntity.SINGLETON_ID)
                        .orElseGet(() -> new AppThemeEntity(theme.brandColor(), theme.fontFamily()));
        entity.setBrandColor(theme.brandColor());
        entity.setFontFamily(theme.fontFamily());
        jpaRepository.save(entity);
    }
}
