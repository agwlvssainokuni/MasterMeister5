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

import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Select } from "make-you-chic-ui";
import { listConnections, type ConnectionSummaryDto } from "../../api/connections";
import { listTables, type TableSummaryDto } from "../../api/masterData";
import { exportCustomizationDefinition, importCustomizationDefinition } from "../../api/customizations";
import { ApiError } from "../../api/auth";

function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });
}

/** frontend-components.md CustomizationScreen (US-3.7). */
export function CustomizationScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [selectedConnectionId, setSelectedConnectionId] = useState("");
  const [tables, setTables] = useState<TableSummaryDto[]>([]);
  const [importErrorMessage, setImportErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    listConnections().then((all) => setConnections(all.filter((c) => c.status === "ACTIVE")));
  }, []);

  useEffect(() => {
    if (!selectedConnectionId) {
      setTables([]);
      return;
    }
    // ADMIN's own connectionId is passed with userId=0 semantics handled server-side via
    // the authenticated principal; listTables only needs a table list here, not permissions.
    listTables(Number(selectedConnectionId)).then(setTables);
  }, [selectedConnectionId]);

  async function handleExport() {
    if (!selectedConnectionId) {
      return;
    }
    const yaml = await exportCustomizationDefinition(Number(selectedConnectionId));
    const blob = new Blob([yaml], { type: "application/x-yaml" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `customizations-${selectedConnectionId}.yaml`;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function handleImportFileChange(file: File) {
    if (!selectedConnectionId) {
      return;
    }
    setImportErrorMessage(null);
    try {
      const content = await readFileAsText(file);
      await importCustomizationDefinition(Number(selectedConnectionId), content);
    } catch (err) {
      setImportErrorMessage(err instanceof ApiError ? err.message : t("admin.customizations.import.error"));
    }
  }

  return (
    <div>
      <h1>{t("admin.customizations.title")}</h1>

      <Select
        options={connections.map((c) => ({ label: c.name, value: String(c.id) }))}
        value={selectedConnectionId}
        onChange={setSelectedConnectionId}
        data-testid="customizations-connection-select"
      />

      {selectedConnectionId && (
        <div>
          <Button onClick={handleExport} data-testid="customizations-export-button">
            {t("admin.customizations.exportButton")}
          </Button>
          <input
            type="file"
            accept=".yaml,.yml"
            data-testid="customizations-import-input"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                handleImportFileChange(file);
              }
              e.target.value = "";
            }}
          />
          {importErrorMessage && <p role="alert">{importErrorMessage}</p>}

          <table data-testid="customizations-tables">
            <thead>
              <tr>
                <th>{t("admin.customizations.columns.schemaName")}</th>
                <th>{t("admin.customizations.columns.tableName")}</th>
              </tr>
            </thead>
            <tbody>
              {tables.map((table) => (
                <tr key={`${table.schemaName}.${table.tableName}`}>
                  <td>{table.schemaName}</td>
                  <td>{table.tableName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
