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

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Placeholder used only until Unit 2 supplies the real {@link JwtTokenValidator}.
 * Every token is treated as invalid, so authenticated endpoints stay closed
 * (fail closed, SECURITY-15) rather than silently open.
 */
@Component
@ConditionalOnMissingBean(JwtTokenValidator.class)
public class NoopJwtTokenValidator implements JwtTokenValidator {

    @Override
    public Optional<JwtAuthentication> validate(String accessToken) {
        return Optional.empty();
    }
}
