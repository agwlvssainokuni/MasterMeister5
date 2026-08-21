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

import cherry.mastermeister5.platform.i18n.MessageResolver;
import cherry.mustache.Mustache;
import cherry.mustache.Template;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * `java-mustache-processor` (cherry-mustache-core) renders the email body;
 * the subject comes from {@link MessageResolver} (i18n). Templates are
 * compiled once at startup ({@link Template} is reusable/thread-safe per its
 * javadoc) rather than re-parsed on every send.
 */
@Service
class NotificationServiceImpl implements NotificationService {

    private enum TemplateId {
        INVITATION,
        PASSWORD_RESET
    }

    private final JavaMailSender mailSender;
    private final MessageResolver messageResolver;
    private final MailProperties mailProperties;
    private final Map<TemplateId, Template> jaTemplates = new EnumMap<>(TemplateId.class);
    private final Map<TemplateId, Template> enTemplates = new EnumMap<>(TemplateId.class);

    NotificationServiceImpl(
            JavaMailSender mailSender, MessageResolver messageResolver, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.messageResolver = messageResolver;
        this.mailProperties = mailProperties;
    }

    @PostConstruct
    void compileTemplates() {
        jaTemplates.put(TemplateId.INVITATION, load("notification/templates/invitation_ja.mustache"));
        jaTemplates.put(
                TemplateId.PASSWORD_RESET, load("notification/templates/password_reset_ja.mustache"));
        enTemplates.put(TemplateId.INVITATION, load("notification/templates/invitation_en.mustache"));
        enTemplates.put(
                TemplateId.PASSWORD_RESET, load("notification/templates/password_reset_en.mustache"));
    }

    @Override
    public void sendInvitationEmail(
            String recipientEmail, Locale locale, String invitationLink, long expiresInHours) {
        send(
                TemplateId.INVITATION,
                "email.invitation.subject",
                recipientEmail,
                locale,
                Map.of("link", invitationLink, "expiresInHours", expiresInHours));
    }

    @Override
    public void sendPasswordResetEmail(
            String recipientEmail, Locale locale, String resetLink, long expiresInHours) {
        send(
                TemplateId.PASSWORD_RESET,
                "email.password_reset.subject",
                recipientEmail,
                locale,
                Map.of("link", resetLink, "expiresInHours", expiresInHours));
    }

    private void send(
            TemplateId templateId,
            String subjectKey,
            String recipientEmail,
            Locale locale,
            Map<String, Object> params) {
        var templates = "en".equals(locale.getLanguage()) ? enTemplates : jaTemplates;
        var body = templates.get(templateId).render(params);
        var subject = messageResolver.resolveMessage(subjectKey, locale);

        var message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(recipientEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private Template load(String resourcePath) {
        var resource = new ClassPathResource(resourcePath);
        try (var reader =
                new java.io.InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return Mustache.compile(reader, new cherry.mustache.MapPartialResolver(Map.of()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template: " + resourcePath, e);
        }
    }
}
