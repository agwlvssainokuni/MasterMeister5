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

import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Modal, RadioGroup, Select, Table, Tabs, TextInput, Textarea, type TableColumn } from "make-you-chic-ui";
import { useAuth } from "../../auth/AuthContext";
import { listConnections, getSchema, type ConnectionSummaryDto, type SchemaViewDto } from "../../api/connections";
import {
  detectParameters,
  executeQuery,
  listSavedQueries,
  retireQuery,
  saveQuery,
  type ParameterDescriptorDto,
  type QueryResultDto,
  type QueryVisibility,
  type SavedQueryDto,
} from "../../api/query";
import { ApiError } from "../../api/auth";
import { buildSql, parseSqlToBuilderState, EMPTY_QUERY_BUILDER_STATE, type QueryBuilderState } from "./queryBuilder";

type Mode = "builder" | "raw";

/** frontend-components.md QueryScreen (US-4.1〜US-4.5). */
export function QueryScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();

  const [mode, setMode] = useState<Mode>("builder");
  const [builderState, setBuilderState] = useState<QueryBuilderState>(EMPTY_QUERY_BUILDER_STATE);
  const [sqlText, setSqlText] = useState("");

  const [savedQueries, setSavedQueries] = useState<SavedQueryDto[]>([]);
  const [selectedSavedQueryId, setSelectedSavedQueryId] = useState("");

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [selectedConnectionId, setSelectedConnectionId] = useState("");
  const [schemas, setSchemas] = useState<SchemaViewDto[]>([]);
  const [selectedSchemaName, setSelectedSchemaName] = useState("");

  const [detectedParams, setDetectedParams] = useState<ParameterDescriptorDto[]>([]);
  const [paramValues, setParamValues] = useState<Record<string, string>>({});

  const [queryResult, setQueryResult] = useState<QueryResultDto | null>(null);
  const [executeErrorMessage, setExecuteErrorMessage] = useState<string | null>(null);

  const [saveModalOpen, setSaveModalOpen] = useState(false);
  const [saveName, setSaveName] = useState("");
  const [saveVisibility, setSaveVisibility] = useState<QueryVisibility>("PRIVATE");

  useEffect(() => {
    listConnections().then((all) => setConnections(all.filter((c) => c.status === "ACTIVE")));
  }, []);

  useEffect(() => {
    listSavedQueries().then(setSavedQueries);
  }, []);

  useEffect(() => {
    if (!selectedConnectionId) {
      setSchemas([]);
      return;
    }
    getSchema(Number(selectedConnectionId)).then(setSchemas);
  }, [selectedConnectionId]);

  // frontend-components.md: builder edits regenerate the SQL preview immediately.
  useEffect(() => {
    if (mode === "builder") {
      setSqlText(buildSql(builderState));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [builderState, mode]);

  // frontend-components.md: parameter auto-detection re-runs whenever sqlText changes.
  useEffect(() => {
    if (!sqlText.trim()) {
      setDetectedParams([]);
      return;
    }
    detectParameters(sqlText)
      .then((params) => {
        setDetectedParams(params);
        setParamValues((prev) => {
          const next: Record<string, string> = {};
          for (const p of params) {
            next[p.name] = prev[p.name] ?? "";
          }
          return next;
        });
      })
      .catch(() => setDetectedParams([]));
  }, [sqlText]);

  function handleSelectSavedQuery(idStr: string) {
    setSelectedSavedQueryId(idStr);
    const selected = savedQueries.find((sq) => String(sq.id) === idStr);
    if (!selected) {
      return;
    }
    setSqlText(selected.sqlText);
    setSaveName(selected.name);
    setSaveVisibility(selected.visibility);
    if (mode === "builder") {
      setBuilderState(parseSqlToBuilderState(selected.sqlText));
    }
  }

  function handleParseToBuilder() {
    setBuilderState(parseSqlToBuilderState(sqlText));
    setMode("builder");
  }

  const selectedSavedQuery = useMemo(
    () => savedQueries.find((sq) => String(sq.id) === selectedSavedQueryId) ?? null,
    [savedQueries, selectedSavedQueryId],
  );
  const canRetire = selectedSavedQuery !== null && selectedSavedQuery.creatorUserId === user?.id;

  async function handleSave() {
    await saveQuery({
      savedQueryId: selectedSavedQuery ? selectedSavedQuery.id : undefined,
      name: saveName,
      sqlText,
      visibility: saveVisibility,
    });
    setSaveModalOpen(false);
    setSavedQueries(await listSavedQueries());
  }

  async function handleRetire() {
    if (!selectedSavedQuery) {
      return;
    }
    await retireQuery(selectedSavedQuery.id);
    setSelectedSavedQueryId("");
    setSavedQueries(await listSavedQueries());
  }

  async function handleExecute() {
    if (!selectedConnectionId || !selectedSchemaName) {
      return;
    }
    setExecuteErrorMessage(null);
    const params: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(paramValues)) {
      params[key] = value;
    }
    try {
      const result = await executeQuery({
        sqlText: selectedSavedQuery ? undefined : sqlText,
        savedQueryId: selectedSavedQuery ? selectedSavedQuery.id : undefined,
        connectionId: Number(selectedConnectionId),
        schemaName: selectedSchemaName,
        params,
      });
      setQueryResult(result);
    } catch (err) {
      setQueryResult(null);
      setExecuteErrorMessage(err instanceof ApiError ? err.message : t("admin.query.execute.error"));
    }
  }

  const resultColumns: TableColumn<Record<string, unknown>>[] = useMemo(
    () => (queryResult ? queryResult.columns.map((c) => ({ key: c, header: c })) : []),
    [queryResult],
  );

  function builderField(
    label: string,
    key: keyof QueryBuilderState,
    multiline: boolean,
  ) {
    const testId = `query-builder-${key.toLowerCase()}-input`;
    return (
      <FormField label={label}>
        {multiline ? (
          <Textarea
            value={builderState[key]}
            onChange={(value) => setBuilderState((prev) => ({ ...prev, [key]: value }))}
            data-testid={testId}
          />
        ) : (
          <TextInput
            value={builderState[key]}
            onChange={(value) => setBuilderState((prev) => ({ ...prev, [key]: value }))}
            data-testid={testId}
          />
        )}
      </FormField>
    );
  }

  return (
    <div>
      <h1>{t("admin.query.title")}</h1>

      <FormField label={t("admin.query.savedQuery")}>
        <Select
          options={[
            { label: t("admin.query.newQuery"), value: "" },
            ...savedQueries.map((sq) => ({ label: sq.name, value: String(sq.id) })),
          ]}
          value={selectedSavedQueryId}
          onChange={handleSelectSavedQuery}
          data-testid="query-saved-query-select"
        />
      </FormField>

      <Tabs
        aria-label={t("admin.query.title")}
        activeIndex={mode === "builder" ? 0 : 1}
        onChange={(index) => setMode(index === 0 ? "builder" : "raw")}
        items={[
          {
            label: t("admin.query.mode.builder"),
            content: (
              <Tabs
                aria-label={t("admin.query.mode.builder")}
                items={[
                  { label: "SELECT", content: builderField("SELECT", "select", false) },
                  { label: "FROM", content: builderField("FROM", "from", false) },
                  { label: "JOIN", content: builderField("JOIN", "join", true) },
                  { label: "WHERE", content: builderField("WHERE", "where", true) },
                  { label: "GROUP BY", content: builderField("GROUP BY", "groupBy", false) },
                  { label: "HAVING", content: builderField("HAVING", "having", false) },
                  { label: "ORDER BY", content: builderField("ORDER BY", "orderBy", false) },
                  { label: "LIMIT/OFFSET", content: builderField("LIMIT/OFFSET", "limitOffset", false) },
                ]}
              />
            ),
          },
          {
            label: t("admin.query.mode.raw"),
            content: (
              <div>
                <Textarea value={sqlText} onChange={setSqlText} rows={6} data-testid="query-raw-sql-textarea" />
                <Button onClick={handleParseToBuilder} data-testid="query-parse-to-builder-button">
                  {t("admin.query.parseToBuilder")}
                </Button>
              </div>
            ),
          },
        ]}
      />

      <FormField label={t("admin.query.sqlPreview")}>
        <Textarea value={sqlText} readOnly rows={4} data-testid="query-sql-preview-textarea" />
      </FormField>

      <Button onClick={() => setSaveModalOpen(true)} data-testid="query-save-button">
        {t("admin.query.saveButton")}
      </Button>
      {canRetire && (
        <Button variant="danger" onClick={handleRetire} data-testid="query-retire-button">
          {t("admin.query.retireButton")}
        </Button>
      )}

      <FormField label={t("admin.query.connection")}>
        <Select
          options={connections.map((c) => ({ label: c.name, value: String(c.id) }))}
          value={selectedConnectionId}
          onChange={(value) => {
            setSelectedConnectionId(value);
            setSelectedSchemaName("");
          }}
          data-testid="query-connection-select"
        />
      </FormField>
      <FormField label={t("admin.query.schema")}>
        <Select
          options={schemas.map((s) => ({ label: s.schemaName, value: s.schemaName }))}
          value={selectedSchemaName}
          onChange={setSelectedSchemaName}
          data-testid="query-schema-select"
        />
      </FormField>

      {detectedParams.length > 0 && (
        <div data-testid="query-params-form">
          {detectedParams.map((p) => (
            <FormField key={p.name} label={p.name}>
              <TextInput
                value={paramValues[p.name] ?? ""}
                onChange={(value) => setParamValues((prev) => ({ ...prev, [p.name]: value }))}
                data-testid={`query-param-${p.name}-input`}
              />
            </FormField>
          ))}
        </div>
      )}

      {executeErrorMessage && <p role="alert">{executeErrorMessage}</p>}

      <Button
        onClick={handleExecute}
        disabled={!selectedConnectionId || !selectedSchemaName}
        data-testid="query-execute-button"
      >
        {t("admin.query.executeButton")}
      </Button>

      {queryResult && (
        <div>
          <p>{t("admin.query.result.summary", { count: queryResult.rowCount, ms: queryResult.executionTimeMs })}</p>
          {queryResult.truncated && <p role="alert">{t("admin.query.result.truncated")}</p>}
          <Table
            columns={resultColumns}
            data={queryResult.rows}
            totalCount={queryResult.rowCount}
            getRowId={(row) => JSON.stringify(row)}
            page={0}
            pageSize={Math.max(queryResult.rows.length, 1)}
            onPageChange={() => {}}
            aria-label={t("admin.query.result.title")}
          />
        </div>
      )}

      <Modal open={saveModalOpen} onClose={() => setSaveModalOpen(false)} title={t("admin.query.save.title")}>
        <FormField label={t("admin.query.save.name")}>
          <TextInput value={saveName} onChange={setSaveName} data-testid="query-save-name-input" />
        </FormField>
        <FormField label={t("admin.query.save.visibility")}>
          <RadioGroup
            name="query-visibility"
            options={[
              { label: t("admin.query.save.visibilityPublic"), value: "PUBLIC" },
              { label: t("admin.query.save.visibilityPrivate"), value: "PRIVATE" },
            ]}
            value={saveVisibility}
            onChange={(value) => setSaveVisibility(value as QueryVisibility)}
          />
        </FormField>
        <Button onClick={handleSave} data-testid="query-save-confirm-button">
          {t("admin.query.save.confirm")}
        </Button>
      </Modal>
    </div>
  );
}
