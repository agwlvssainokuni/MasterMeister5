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

/**
 * business-rules.md BR-21: raised when an already-used (revoked) refresh
 * token is presented again. The whole token family has already been revoked
 * by the time this is thrown; the caller is responsible for recording the
 * audit event and clearing the client's cookie.
 */
public class RefreshTokenReuseDetectedException extends RuntimeException {

    private final Long userId;

    public RefreshTokenReuseDetectedException(Long userId) {
        super("Refresh token reuse detected for user " + userId);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
