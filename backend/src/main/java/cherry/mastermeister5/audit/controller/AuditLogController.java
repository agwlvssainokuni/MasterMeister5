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

package cherry.mastermeister5.audit.controller;

import cherry.mastermeister5.audit.AuditEventFilterCriteria;
import cherry.mastermeister5.audit.AuditEventType;
import cherry.mastermeister5.audit.AuditLogService;
import cherry.mastermeister5.audit.controller.dto.AuditEventPageResponse;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-5.1 / BR-16: audit log viewing, ADMIN only. SECURITY-08: restricted to
 * ADMIN at the SecurityConfig {@code /api/admin/**} path-matcher level;
 * {@code @PreAuthorize} here is defense in depth, mirroring Unit 2/3/4/5's
 * admin-only controllers.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/admin/audit-events")
    public AuditEventPageResponse listEvents(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var criteria = new AuditEventFilterCriteria(eventType, actorUserId, fromDate, toDate);
        return AuditEventPageResponse.from(auditLogService.listEvents(criteria, PageRequest.of(page, size)));
    }
}
