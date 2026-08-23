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
import { FormField, Select, Table, TextInput, type TableColumn } from "make-you-chic-ui";
import { listConnections, type ConnectionSummaryDto } from "../../api/connections";
import { listExecutionHistory, type ExecutionHistoryDto, type ExecutionHistoryPageDto } from "../../api/query";

/** frontend-components.md QueryHistoryScreen (US-4.6). */
export function QueryHistoryScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [selectedConnectionId, setSelectedConnectionId] = useState("");
  const [schemaName, setSchemaName] = useState("");
  const [sqlTextContains, setSqlTextContains] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  // 1-origin for display (make-you-chic-ui's Table pagination contract); converted to 0-origin only for the API call.
  const [page, setPage] = useState(1);
  const pageSize = 50;
  const [historyPage, setHistoryPage] = useState<ExecutionHistoryPageDto | null>(null);

  useEffect(() => {
    listConnections().then(setConnections);
  }, []);

  const reload = useCallback(() => {
    listExecutionHistory({
      connectionId: selectedConnectionId ? Number(selectedConnectionId) : undefined,
      schemaName: schemaName || undefined,
      sqlTextContains: sqlTextContains || undefined,
      fromDate: fromDate ? new Date(fromDate).toISOString() : undefined,
      toDate: toDate ? new Date(toDate).toISOString() : undefined,
      page: page - 1,
      size: pageSize,
    }).then(setHistoryPage);
  }, [selectedConnectionId, schemaName, sqlTextContains, fromDate, toDate, page]);

  useEffect(() => {
    reload();
  }, [reload]);

  function updateFilter(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(1);
  }

  const columns: TableColumn<ExecutionHistoryDto>[] = useMemo(
    () => [
      { key: "executedAt", header: t("admin.queryHistory.columns.executedAt") },
      { key: "executedByUserId", header: t("admin.queryHistory.columns.executedBy") },
      { key: "connectionId", header: t("admin.queryHistory.columns.connection") },
      { key: "schemaName", header: t("admin.queryHistory.columns.schema") },
      {
        key: "source",
        header: t("admin.queryHistory.columns.source"),
        render: (row) => (row.savedQueryId !== null ? t("admin.queryHistory.source.saved") : t("admin.queryHistory.source.direct")),
      },
      { key: "sqlText", header: t("admin.queryHistory.columns.sql") },
      { key: "resultRowCount", header: t("admin.queryHistory.columns.rowCount") },
      { key: "executionTimeMs", header: t("admin.queryHistory.columns.executionTime") },
    ],
    [t],
  );

  return (
    <div>
      <h1>{t("admin.queryHistory.title")}</h1>

      <FormField label={t("admin.queryHistory.filter.connection")}>
        <Select
          options={[
            { label: t("admin.queryHistory.filter.all"), value: "" },
            ...connections.map((c) => ({ label: c.name, value: String(c.id) })),
          ]}
          value={selectedConnectionId}
          onChange={(value) => updateFilter(setSelectedConnectionId, value)}
          data-testid="query-history-connection-select"
        />
      </FormField>
      <FormField label={t("admin.queryHistory.filter.schema")}>
        <TextInput
          value={schemaName}
          onChange={(value) => updateFilter(setSchemaName, value)}
          data-testid="query-history-schema-input"
        />
      </FormField>
      <FormField label={t("admin.queryHistory.filter.sqlTextContains")}>
        <TextInput
          value={sqlTextContains}
          onChange={(value) => updateFilter(setSqlTextContains, value)}
          data-testid="query-history-sql-input"
        />
      </FormField>
      <FormField label={t("admin.queryHistory.filter.fromDate")}>
        <TextInput
          type="date"
          value={fromDate}
          onChange={(value) => updateFilter(setFromDate, value)}
          data-testid="query-history-from-date-input"
        />
      </FormField>
      <FormField label={t("admin.queryHistory.filter.toDate")}>
        <TextInput
          type="date"
          value={toDate}
          onChange={(value) => updateFilter(setToDate, value)}
          data-testid="query-history-to-date-input"
        />
      </FormField>

      {historyPage && (
        <Table
          columns={columns}
          data={historyPage.content}
          totalCount={historyPage.totalElements}
          getRowId={(row) => String(row.id)}
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          aria-label={t("admin.queryHistory.title")}
        />
      )}
    </div>
  );
}
