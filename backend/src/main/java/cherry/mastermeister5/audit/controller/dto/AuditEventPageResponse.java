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

package cherry.mastermeister5.audit.controller.dto;

import cherry.mastermeister5.audit.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;

public record AuditEventPageResponse(List<AuditEventResponse> content, int page, int size, long totalElements) {

    public static AuditEventPageResponse from(Page<AuditEvent> page) {
        return new AuditEventPageResponse(
                page.getContent().stream().map(AuditEventResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }
}
