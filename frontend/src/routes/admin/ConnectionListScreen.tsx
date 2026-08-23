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

import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Modal, Select, Table, TextInput, type TableColumn } from "make-you-chic-ui";
import {
  deactivateConnection,
  importSchema,
  listAdminConnections,
  reactivateConnection,
  registerConnection,
  updateConnection,
  type AdminConnectionSummaryDto,
  type RdbmsType,
  type SchemaImportResultDto,
} from "../../api/connections";
import { ApiError } from "../../api/auth";

const RDBMS_OPTIONS: { label: string; value: RdbmsType }[] = [
  { label: "MySQL", value: "MYSQL" },
  { label: "MariaDB", value: "MARIADB" },
  { label: "PostgreSQL", value: "POSTGRESQL" },
  { label: "H2", value: "H2" },
];

// Each RDBMS's own conventional default port, so switching RDBMS種別 doesn't
// leave a stale port (e.g. MySQL's 3306) behind for e.g. PostgreSQL.
const RDBMS_DEFAULT_PORTS: Record<RdbmsType, string> = {
  MYSQL: "3306",
  MARIADB: "3306",
  POSTGRESQL: "5432",
  H2: "9092",
};

const PAGE_SIZE = 20;

/** frontend-components.md ConnectionListScreen (US-2.1〜2.3). listConnections() returns the full, unpaginated list; paging below is client-side over that array. */
export function ConnectionListScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [connections, setConnections] = useState<AdminConnectionSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const [registerOpen, setRegisterOpen] = useState(false);
  const [name, setName] = useState("");
  const [rdbmsType, setRdbmsType] = useState<RdbmsType>("MYSQL");
  const [host, setHost] = useState("");
  const [port, setPort] = useState("3306");
  const [databaseName, setDatabaseName] = useState("");
  const [schemaNameHint, setSchemaNameHint] = useState("");
  const [extraParams, setExtraParams] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [registerSubmitting, setRegisterSubmitting] = useState(false);
  const [registerErrorMessage, setRegisterErrorMessage] = useState<string | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [editConnectionId, setEditConnectionId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editRdbmsType, setEditRdbmsType] = useState<RdbmsType>("MYSQL");
  const [editHost, setEditHost] = useState("");
  const [editPort, setEditPort] = useState("3306");
  const [editDatabaseName, setEditDatabaseName] = useState("");
  const [editSchemaNameHint, setEditSchemaNameHint] = useState("");
  const [editExtraParams, setEditExtraParams] = useState("");
  const [editUsername, setEditUsername] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editErrorMessage, setEditErrorMessage] = useState<string | null>(null);

  const [importResult, setImportResult] = useState<SchemaImportResultDto | null>(null);

  const reload = useCallback(() => {
    setLoading(true);
    listAdminConnections()
      .then(setConnections)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleRegisterSubmit(e: FormEvent) {
    e.preventDefault();
    setRegisterSubmitting(true);
    setRegisterErrorMessage(null);
    try {
      await registerConnection({
        name,
        rdbmsType,
        host,
        port: Number(port),
        databaseName,
        schemaNameHint: schemaNameHint || undefined,
        extraParams: extraParams || undefined,
        username,
        password,
      });
      setRegisterOpen(false);
      setName("");
      setHost("");
      setDatabaseName("");
      setSchemaNameHint("");
      setExtraParams("");
      setUsername("");
      setPassword("");
      reload();
    } catch (err) {
      setRegisterErrorMessage(err instanceof ApiError ? err.message : t("admin.connections.register.error"));
    } finally {
      setRegisterSubmitting(false);
    }
  }

  function handleEditOpen(row: AdminConnectionSummaryDto) {
    setEditConnectionId(row.id);
    setEditName(row.name);
    setEditRdbmsType(row.rdbmsType);
    setEditHost(row.host);
    setEditPort(String(row.port));
    setEditDatabaseName(row.databaseName);
    setEditSchemaNameHint(row.schemaNameHint ?? "");
    setEditExtraParams(row.extraParams ?? "");
    setEditUsername(row.username);
    setEditPassword("");
    setEditErrorMessage(null);
    setEditOpen(true);
  }

  async function handleEditSubmit(e: FormEvent) {
    e.preventDefault();
    if (editConnectionId === null) {
      return;
    }
    setEditSubmitting(true);
    setEditErrorMessage(null);
    try {
      await updateConnection(editConnectionId, {
        name: editName,
        rdbmsType: editRdbmsType,
        host: editHost,
        port: Number(editPort),
        databaseName: editDatabaseName,
        schemaNameHint: editSchemaNameHint || undefined,
        extraParams: editExtraParams || undefined,
        username: editUsername,
        password: editPassword || undefined,
      });
      setEditOpen(false);
      reload();
    } catch (err) {
      setEditErrorMessage(err instanceof ApiError ? err.message : t("admin.connections.edit.error"));
    } finally {
      setEditSubmitting(false);
    }
  }

  async function handleDeactivate(connectionId: number) {
    await deactivateConnection(connectionId);
    reload();
  }

  async function handleReactivate(connectionId: number) {
    await reactivateConnection(connectionId);
    reload();
  }

  async function handleImportSchema(connectionId: number) {
    const result = await importSchema(connectionId);
    setImportResult(result);
    reload();
  }

  const columns: TableColumn<AdminConnectionSummaryDto>[] = useMemo(
    () => [
      { key: "name", header: t("admin.connections.columns.name") },
      { key: "rdbmsType", header: t("admin.connections.columns.rdbmsType") },
      {
        key: "hostPort",
        header: t("admin.connections.columns.hostPort"),
        render: (row) => `${row.host}:${row.port}`,
      },
      { key: "databaseName", header: t("admin.connections.columns.databaseName") },
      {
        key: "status",
        header: t("admin.connections.columns.status"),
        render: (row) => t(`admin.connections.status.${row.status.toLowerCase()}`),
      },
      {
        key: "lastSchemaImportAt",
        header: t("admin.connections.columns.lastSchemaImportAt"),
        render: (row) => row.lastSchemaImportAt ?? t("admin.connections.notImported"),
      },
      {
        key: "actions",
        header: t("admin.connections.columns.actions"),
        render: (row) => (
          <>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => handleEditOpen(row)}
              data-testid={`connections-row-${row.id}-edit-button`}
            >
              {t("admin.connections.editButton")}
            </Button>
            {row.status === "ACTIVE" && (
              <>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handleImportSchema(row.id)}
                  data-testid={`connections-row-${row.id}-import-button`}
                >
                  {t("admin.connections.importButton")}
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleDeactivate(row.id)}
                  data-testid={`connections-row-${row.id}-deactivate-button`}
                >
                  {t("admin.connections.deactivateButton")}
                </Button>
              </>
            )}
            {row.status === "DEACTIVATED" && (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => handleReactivate(row.id)}
                data-testid={`connections-row-${row.id}-reactivate-button`}
              >
                {t("admin.connections.reactivateButton")}
              </Button>
            )}
          </>
        ),
      },
    ],
    [t],
  );

  const pagedConnections = connections.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <div>
      <h1>{t("admin.connections.title")}</h1>
      <Button data-testid="connections-register-button" onClick={() => setRegisterOpen(true)}>
        {t("admin.connections.registerButton")}
      </Button>

      {loading ? (
        <p>{t("common.loading")}</p>
      ) : (
        <div data-testid="connections-table">
          <Table
            columns={columns}
            data={pagedConnections}
            totalCount={connections.length}
            getRowId={(row) => String(row.id)}
            page={page}
            pageSize={PAGE_SIZE}
            onPageChange={setPage}
            aria-label={t("admin.connections.title")}
          />
        </div>
      )}

      <Modal
        open={registerOpen}
        onClose={() => setRegisterOpen(false)}
        title={t("admin.connections.register.title")}
      >
        <form onSubmit={handleRegisterSubmit} data-testid="connections-register-form">
          <FormField label={t("admin.connections.columns.name")} required>
            <TextInput value={name} onChange={setName} required data-testid="connections-register-form-name-input" />
          </FormField>
          <FormField label={t("admin.connections.columns.rdbmsType")} required>
            <Select
              options={RDBMS_OPTIONS}
              value={rdbmsType}
              onChange={(value) => {
                const nextType = value as RdbmsType;
                setRdbmsType(nextType);
                setPort(RDBMS_DEFAULT_PORTS[nextType]);
              }}
              data-testid="connections-register-form-rdbms-select"
            />
          </FormField>
          <FormField label={t("admin.connections.host")} required>
            <TextInput value={host} onChange={setHost} required data-testid="connections-register-form-host-input" />
          </FormField>
          <FormField label={t("admin.connections.port")} required>
            <TextInput
              type="number"
              value={port}
              onChange={setPort}
              required
              data-testid="connections-register-form-port-input"
            />
          </FormField>
          <FormField label={t("admin.connections.columns.databaseName")} required>
            <TextInput
              value={databaseName}
              onChange={setDatabaseName}
              required
              data-testid="connections-register-form-database-input"
            />
          </FormField>
          <FormField label={t("admin.connections.schemaNameHint")}>
            <TextInput
              value={schemaNameHint}
              onChange={setSchemaNameHint}
              data-testid="connections-register-form-schema-hint-input"
            />
          </FormField>
          <FormField label={t("admin.connections.extraParams")} helperText={t("admin.connections.extraParamsHint")}>
            <TextInput
              value={extraParams}
              onChange={setExtraParams}
              data-testid="connections-register-form-extra-params-input"
            />
          </FormField>
          <FormField label={t("admin.connections.username")} required>
            <TextInput
              value={username}
              onChange={setUsername}
              required
              data-testid="connections-register-form-username-input"
            />
          </FormField>
          <FormField label={t("admin.connections.password")} required>
            <TextInput
              type="password"
              value={password}
              onChange={setPassword}
              required
              data-testid="connections-register-form-password-input"
            />
          </FormField>
          {registerErrorMessage && <p role="alert">{registerErrorMessage}</p>}
          <Button
            type="submit"
            loading={registerSubmitting}
            data-testid="connections-register-form-submit-button"
          >
            {t("admin.connections.register.submit")}
          </Button>
        </form>
      </Modal>

      <Modal open={editOpen} onClose={() => setEditOpen(false)} title={t("admin.connections.edit.title")}>
        <form onSubmit={handleEditSubmit} data-testid="connections-edit-form">
          <FormField label={t("admin.connections.columns.name")} required>
            <TextInput
              value={editName}
              onChange={setEditName}
              required
              data-testid="connections-edit-form-name-input"
            />
          </FormField>
          <FormField label={t("admin.connections.columns.rdbmsType")} required>
            <Select
              options={RDBMS_OPTIONS}
              value={editRdbmsType}
              onChange={(value) => {
                const nextType = value as RdbmsType;
                setEditRdbmsType(nextType);
                setEditPort(RDBMS_DEFAULT_PORTS[nextType]);
              }}
              data-testid="connections-edit-form-rdbms-select"
            />
          </FormField>
          <FormField label={t("admin.connections.host")} required>
            <TextInput
              value={editHost}
              onChange={setEditHost}
              required
              data-testid="connections-edit-form-host-input"
            />
          </FormField>
          <FormField label={t("admin.connections.port")} required>
            <TextInput
              type="number"
              value={editPort}
              onChange={setEditPort}
              required
              data-testid="connections-edit-form-port-input"
            />
          </FormField>
          <FormField label={t("admin.connections.columns.databaseName")} required>
            <TextInput
              value={editDatabaseName}
              onChange={setEditDatabaseName}
              required
              data-testid="connections-edit-form-database-input"
            />
          </FormField>
          <FormField label={t("admin.connections.schemaNameHint")}>
            <TextInput
              value={editSchemaNameHint}
              onChange={setEditSchemaNameHint}
              data-testid="connections-edit-form-schema-hint-input"
            />
          </FormField>
          <FormField label={t("admin.connections.extraParams")} helperText={t("admin.connections.extraParamsHint")}>
            <TextInput
              value={editExtraParams}
              onChange={setEditExtraParams}
              data-testid="connections-edit-form-extra-params-input"
            />
          </FormField>
          <FormField label={t("admin.connections.username")} required>
            <TextInput
              value={editUsername}
              onChange={setEditUsername}
              required
              data-testid="connections-edit-form-username-input"
            />
          </FormField>
          <FormField label={t("admin.connections.password")} helperText={t("admin.connections.edit.passwordHint")}>
            <TextInput
              type="password"
              value={editPassword}
              onChange={setEditPassword}
              data-testid="connections-edit-form-password-input"
            />
          </FormField>
          {editErrorMessage && <p role="alert">{editErrorMessage}</p>}
          <Button type="submit" loading={editSubmitting} data-testid="connections-edit-form-submit-button">
            {t("admin.connections.edit.submit")}
          </Button>
        </form>
      </Modal>

      <Modal
        open={importResult !== null}
        onClose={() => setImportResult(null)}
        title={t("admin.connections.importResult.title")}
      >
        {importResult && (
          <div data-testid="connections-import-result">
            <p>
              {t("admin.connections.importResult.summary", {
                schemas: importResult.schemasImported,
                tables: importResult.tablesImported,
                columns: importResult.columnsImported,
              })}
            </p>
            {importResult.removedTableRefs.length > 0 && (
              <div>
                <p>{t("admin.connections.importResult.removedTables")}</p>
                <ul>
                  {importResult.removedTableRefs.map((ref) => (
                    <li key={ref}>{ref}</li>
                  ))}
                </ul>
              </div>
            )}
            {importResult.prunedCustomizationCount > 0 && (
              <p data-testid="connections-import-result-pruned-customizations">
                {t("admin.connections.importResult.prunedCustomizations", {
                  count: importResult.prunedCustomizationCount,
                })}
              </p>
            )}
            <Button onClick={() => setImportResult(null)} data-testid="connections-import-result-close-button">
              {t("admin.connections.importResult.close")}
            </Button>
          </div>
        )}
      </Modal>
    </div>
  );
}
