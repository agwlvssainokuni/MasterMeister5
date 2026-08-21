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

package cherry.mastermeister5.platform.web;

import cherry.mastermeister5.platform.i18n.MessageResolver;
import cherry.mastermeister5.platform.logging.CorrelationIdFilter;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {

    private final MessageResolver messageResolver;

    public ErrorResponseFactory(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public ErrorResponse create(String errorCode, String messageKey, Locale locale) {
        var correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        var message = messageResolver.resolveMessage(messageKey, locale);
        return new ErrorResponse(errorCode, message, correlationId);
    }
}
