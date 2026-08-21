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

package cherry.mastermeister5.notification;

import java.util.Locale;

/**
 * component-methods.md: NotificationComponent#renderTemplate / #sendEmail,
 * specialized here to the two email flows Unit 2 actually needs (invitation,
 * password reset). {@code expiresInHours} is passed by the caller so the
 * template reflects the configured TTL rather than a hardcoded value.
 */
public interface NotificationService {

    void sendInvitationEmail(String recipientEmail, Locale locale, String invitationLink, long expiresInHours);

    void sendPasswordResetEmail(String recipientEmail, Locale locale, String resetLink, long expiresInHours);
}
