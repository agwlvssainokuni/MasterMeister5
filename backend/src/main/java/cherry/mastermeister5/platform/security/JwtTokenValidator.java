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

/**
 * Extension point for JWT verification. Unit 1 ships only
 * {@link NoopJwtTokenValidator}; Unit 2 (user management) provides the real
 * implementation (signature/expiration/audience/issuer checks) once the token
 * issuing side exists, and replaces this bean.
 */
public interface JwtTokenValidator {

    Optional<JwtAuthentication> validate(String accessToken);
}
