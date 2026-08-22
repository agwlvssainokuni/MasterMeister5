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

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Modal, Select, TextInput } from "make-you-chic-ui";
import {
  deactivateConnection,
  importSchema,
  listConnections,
  reactivateConnection,
  registerConnection,
  type ConnectionSummaryDto,
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

/** frontend-components.md ConnectionListScreen (US-2.1〜2.3). */
export function ConnectionListScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [registerOpen, setRegisterOpen] = useState(false);
  const [name, setName] = useState("");
  const [rdbmsType, setRdbmsType] = useState<RdbmsType>("MYSQL");
  const [host, setHost] = useState("");
  const [port, setPort] = useState("3306");
  const [databaseName, setDatabaseName] = useState("");
  const [schemaNameHint, setSchemaNameHint] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [registerSubmitting, setRegisterSubmitting] = useState(false);
  const [registerErrorMessage, setRegisterErrorMessage] = useState<string | null>(null);

  const [importResult, setImportResult] = useState<SchemaImportResultDto | null>(null);

  const reload = useCallback(() => {
    setLoading(true);
    listConnections()
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
        username,
        password,
      });
      setRegisterOpen(false);
      setName("");
      setHost("");
      setDatabaseName("");
      setSchemaNameHint("");
      setUsername("");
      setPassword("");
      reload();
    } catch (err) {
      setRegisterErrorMessage(err instanceof ApiError ? err.message : t("admin.connections.register.error"));
    } finally {
      setRegisterSubmitting(false);
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

  return (
    <div>
      <h1>{t("admin.connections.title")}</h1>
      <Button data-testid="connections-register-button" onClick={() => setRegisterOpen(true)}>
        {t("admin.connections.registerButton")}
      </Button>

      {loading ? (
        <p>{t("common.loading")}</p>
      ) : (
        <table data-testid="connections-table">
          <thead>
            <tr>
              <th>{t("admin.connections.columns.name")}</th>
              <th>{t("admin.connections.columns.rdbmsType")}</th>
              <th>{t("admin.connections.columns.hostPort")}</th>
              <th>{t("admin.connections.columns.databaseName")}</th>
              <th>{t("admin.connections.columns.status")}</th>
              <th>{t("admin.connections.columns.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {connections.map((connection) => (
              <tr key={connection.id} data-testid={`connections-row-${connection.id}`}>
                <td>{connection.name}</td>
                <td>{connection.rdbmsType}</td>
                <td>
                  {connection.host}:{connection.port}
                </td>
                <td>{connection.databaseName}</td>
                <td>{t(`admin.connections.status.${connection.status.toLowerCase()}`)}</td>
                <td>
                  {connection.status === "ACTIVE" && (
                    <>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleImportSchema(connection.id)}
                        data-testid={`connections-row-${connection.id}-import-button`}
                      >
                        {t("admin.connections.importButton")}
                      </Button>
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => handleDeactivate(connection.id)}
                        data-testid={`connections-row-${connection.id}-deactivate-button`}
                      >
                        {t("admin.connections.deactivateButton")}
                      </Button>
                    </>
                  )}
                  {connection.status === "DEACTIVATED" && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleReactivate(connection.id)}
                      data-testid={`connections-row-${connection.id}-reactivate-button`}
                    >
                      {t("admin.connections.reactivateButton")}
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
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
              onChange={(value) => setRdbmsType(value as RdbmsType)}
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
