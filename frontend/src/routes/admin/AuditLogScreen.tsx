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

import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Table, TextInput, type TableColumn } from "make-you-chic-ui";
import { listAuditEvents, type AuditEventDto, type AuditEventPageDto } from "../../api/auditLog";

/**
 * frontend-components.md AuditLogScreen (US-5.1). Question 7: filters cover
 * only AuditEvent's real columns (eventType/actorUserId/occurredAt range);
 * `details` (JSON) is shown only via row-level expansion, never filtered on.
 */
export function AuditLogScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [eventType, setEventType] = useState("");
  const [actorUserId, setActorUserId] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  // 1-origin for display (make-you-chic-ui's Table pagination contract); converted to 0-origin only for the API call.
  const [page, setPage] = useState(1);
  const pageSize = 50;
  const [eventPage, setEventPage] = useState<AuditEventPageDto | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const reload = useCallback(() => {
    listAuditEvents({
      eventType: eventType || undefined,
      actorUserId: actorUserId ? Number(actorUserId) : undefined,
      fromDate: fromDate ? new Date(fromDate).toISOString() : undefined,
      toDate: toDate ? new Date(toDate).toISOString() : undefined,
      page: page - 1,
      size: pageSize,
    }).then(setEventPage);
  }, [eventType, actorUserId, fromDate, toDate, page]);

  useEffect(() => {
    reload();
  }, [reload]);

  function updateFilter(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(1);
  }

  const expandedEvent = useMemo(
    () => eventPage?.content.find((e) => e.id === expandedId) ?? null,
    [eventPage, expandedId],
  );

  const columns: TableColumn<AuditEventDto>[] = useMemo(
    () => [
      { key: "occurredAt", header: t("admin.auditLog.columns.occurredAt") },
      { key: "actorUserId", header: t("admin.auditLog.columns.actor") },
      { key: "eventType", header: t("admin.auditLog.columns.eventType") },
      {
        key: "details",
        header: t("admin.auditLog.columns.details"),
        render: (row) => (
          <Button
            size="sm"
            onClick={() => setExpandedId((prev) => (prev === row.id ? null : row.id))}
            data-testid={`audit-log-row-${row.id}-details-button`}
          >
            {t("admin.auditLog.detailsButton")}
          </Button>
        ),
      },
    ],
    [t],
  );

  return (
    <div>
      <h1>{t("admin.auditLog.title")}</h1>

      <FormField label={t("admin.auditLog.filter.eventType")}>
        <TextInput
          value={eventType}
          onChange={(value) => updateFilter(setEventType, value)}
          data-testid="audit-log-event-type-input"
        />
      </FormField>
      <FormField label={t("admin.auditLog.filter.actorUserId")}>
        <TextInput
          value={actorUserId}
          onChange={(value) => updateFilter(setActorUserId, value)}
          data-testid="audit-log-actor-input"
        />
      </FormField>
      <FormField label={t("admin.auditLog.filter.fromDate")}>
        <TextInput
          type="date"
          value={fromDate}
          onChange={(value) => updateFilter(setFromDate, value)}
          data-testid="audit-log-from-date-input"
        />
      </FormField>
      <FormField label={t("admin.auditLog.filter.toDate")}>
        <TextInput
          type="date"
          value={toDate}
          onChange={(value) => updateFilter(setToDate, value)}
          data-testid="audit-log-to-date-input"
        />
      </FormField>

      {eventPage && (
        <>
          <Table
            columns={columns}
            data={eventPage.content}
            totalCount={eventPage.totalElements}
            getRowId={(row) => String(row.id)}
            page={page}
            pageSize={pageSize}
            onPageChange={setPage}
            aria-label={t("admin.auditLog.title")}
          />
          {expandedEvent && <pre data-testid="audit-log-details-json">{JSON.stringify(expandedEvent.details, null, 2)}</pre>}
        </>
      )}
    </div>
  );
}
