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

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, RadioGroup, Select } from "make-you-chic-ui";
import { getSchema, listConnections, type ConnectionSummaryDto, type SchemaViewDto } from "../../api/connections";
import { listUsers, type UserSummaryDto } from "../../api/adminUsers";
import { listGroups, type GroupSummaryDto } from "../../api/groups";
import {
  exportPermissions,
  importPermissions,
  listPermissionEntries,
  setAuxiliaryPermission,
  setPrimaryPermission,
  type PermissionEntryDto,
  type PrimaryLevel,
  type SubjectType,
} from "../../api/permissions";
import { ApiError } from "../../api/auth";

const PRIMARY_OPTIONS: { label: string; value: string }[] = [
  { label: "-", value: "" },
  { label: "NONE", value: "NONE" },
  { label: "READ", value: "READ" },
  { label: "UPDATE", value: "UPDATE" },
];

function entryKey(resourceLevel: string, tableName: string | null, columnName: string | null): string {
  return `${resourceLevel}:${tableName ?? ""}:${columnName ?? ""}`;
}

function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });
}

/** frontend-components.md PermissionScreen (US-2.4, US-2.5, US-2.6). */
export function PermissionScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [selectedConnectionId, setSelectedConnectionId] = useState("");
  const [subjectType, setSubjectType] = useState<SubjectType>("USER");
  const [users, setUsers] = useState<UserSummaryDto[]>([]);
  const [groups, setGroups] = useState<GroupSummaryDto[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState("");
  const [schema, setSchema] = useState<SchemaViewDto[]>([]);
  const [entries, setEntries] = useState<Map<string, PermissionEntryDto>>(new Map());
  const [importErrorMessage, setImportErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    listConnections().then((all) => setConnections(all.filter((c) => c.status === "ACTIVE")));
    listUsers().then(setUsers);
    listGroups().then(setGroups);
  }, []);

  const reloadTree = useCallback(() => {
    if (!selectedConnectionId) {
      setSchema([]);
      return;
    }
    getSchema(Number(selectedConnectionId)).then(setSchema);
  }, [selectedConnectionId]);

  const reloadEntries = useCallback(() => {
    if (!selectedConnectionId || !selectedSubjectId) {
      setEntries(new Map());
      return;
    }
    listPermissionEntries(Number(selectedConnectionId), subjectType, Number(selectedSubjectId)).then((list) => {
      const map = new Map<string, PermissionEntryDto>();
      for (const entry of list) {
        map.set(entryKey(entry.resourceLevel, entry.tableName, entry.columnName), entry);
      }
      setEntries(map);
    });
  }, [selectedConnectionId, selectedSubjectId, subjectType]);

  useEffect(() => {
    reloadTree();
  }, [reloadTree]);

  useEffect(() => {
    reloadEntries();
  }, [reloadEntries]);

  async function handlePrimaryChange(
    resourceLevel: "SCHEMA" | "TABLE" | "COLUMN",
    schemaName: string,
    tableName: string | undefined,
    columnName: string | undefined,
    value: string,
  ) {
    if (!value || !selectedConnectionId || !selectedSubjectId) {
      return;
    }
    await setPrimaryPermission({
      subjectType,
      subjectId: Number(selectedSubjectId),
      connectionId: Number(selectedConnectionId),
      resourceLevel,
      schemaName,
      tableName,
      columnName,
      primaryLevel: value as PrimaryLevel,
    });
    reloadEntries();
  }

  async function handleAuxChange(
    resourceLevel: "SCHEMA" | "TABLE",
    schemaName: string,
    tableName: string | undefined,
    field: "auxCreate" | "auxDelete",
    checked: boolean,
  ) {
    if (!selectedConnectionId || !selectedSubjectId) {
      return;
    }
    const existing = entries.get(entryKey(resourceLevel, tableName ?? null, null));
    await setAuxiliaryPermission({
      subjectType,
      subjectId: Number(selectedSubjectId),
      connectionId: Number(selectedConnectionId),
      resourceLevel,
      schemaName,
      tableName,
      auxCreate: field === "auxCreate" ? checked : (existing?.auxCreate ?? undefined),
      auxDelete: field === "auxDelete" ? checked : (existing?.auxDelete ?? undefined),
    });
    reloadEntries();
  }

  async function handleExport() {
    if (!selectedConnectionId) {
      return;
    }
    const yaml = await exportPermissions(Number(selectedConnectionId));
    const blob = new Blob([yaml], { type: "application/x-yaml" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `permissions-${selectedConnectionId}.yaml`;
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
      await importPermissions(Number(selectedConnectionId), content);
      reloadEntries();
    } catch (err) {
      setImportErrorMessage(err instanceof ApiError ? err.message : t("admin.permissions.import.error"));
    }
  }

  const subjectOptions = [
    { label: t("common.selectPlaceholder"), value: "" },
    ...(subjectType === "USER"
      ? users.map((u) => ({ label: `${u.name ?? ""} <${u.email}>`, value: String(u.id) }))
      : groups.map((g) => ({ label: g.name, value: String(g.id) }))),
  ];

  const readyToEdit = selectedConnectionId !== "" && selectedSubjectId !== "";

  return (
    <div>
      <h1>{t("admin.permissions.title")}</h1>

      <Select
        options={[
          { label: t("common.selectPlaceholder"), value: "" },
          ...connections.map((c) => ({ label: c.name, value: String(c.id) })),
        ]}
        value={selectedConnectionId}
        onChange={(value) => {
          setSelectedConnectionId(value);
          setSelectedSubjectId("");
        }}
        data-testid="permissions-connection-select"
      />

      <RadioGroup
        name="subject-type"
        options={[
          { label: t("admin.permissions.subjectType.user"), value: "USER" },
          { label: t("admin.permissions.subjectType.group"), value: "GROUP" },
        ]}
        value={subjectType}
        onChange={(value) => {
          setSubjectType(value as SubjectType);
          setSelectedSubjectId("");
        }}
      />

      <Select
        options={subjectOptions}
        value={selectedSubjectId}
        onChange={setSelectedSubjectId}
        data-testid="permissions-subject-select"
      />

      {selectedConnectionId && (
        <div>
          <Button onClick={handleExport} data-testid="permissions-export-button">
            {t("admin.permissions.exportButton")}
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".yaml,.yml"
            data-testid="permissions-import-input"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                handleImportFileChange(file);
              }
              e.target.value = "";
            }}
          />
          {importErrorMessage && <p role="alert">{importErrorMessage}</p>}
        </div>
      )}

      {selectedConnectionId && schema.length === 0 && (
        <p data-testid="permissions-no-schema-message">{t("admin.permissions.noSchemaImported")}</p>
      )}

      {readyToEdit && (
        <div data-testid="permissions-tree">
          {schema.map((s) => (
            <div key={s.schemaName} data-testid={`permissions-schema-${s.schemaName}`}>
              <strong>{s.schemaName}</strong>
              <Select
                options={PRIMARY_OPTIONS}
                value={entries.get(entryKey("SCHEMA", null, null))?.primaryLevel ?? ""}
                onChange={(value) => handlePrimaryChange("SCHEMA", s.schemaName, undefined, undefined, value)}
                data-testid={`permissions-schema-${s.schemaName}-primary-select`}
              />
              <ul>
                {s.tables.map((table) => (
                  <li key={table.tableName} data-testid={`permissions-table-${s.schemaName}-${table.tableName}`}>
                    {table.tableName}
                    <Select
                      options={PRIMARY_OPTIONS}
                      value={entries.get(entryKey("TABLE", table.tableName, null))?.primaryLevel ?? ""}
                      onChange={(value) =>
                        handlePrimaryChange("TABLE", s.schemaName, table.tableName, undefined, value)
                      }
                      data-testid={`permissions-table-${s.schemaName}-${table.tableName}-primary-select`}
                    />
                    <label>
                      {t("admin.permissions.auxCreate")}
                      <input
                        type="checkbox"
                        checked={entries.get(entryKey("TABLE", table.tableName, null))?.auxCreate ?? false}
                        onChange={(e) =>
                          handleAuxChange("TABLE", s.schemaName, table.tableName, "auxCreate", e.target.checked)
                        }
                        data-testid={`permissions-table-${s.schemaName}-${table.tableName}-aux-create-checkbox`}
                      />
                    </label>
                    <label>
                      {t("admin.permissions.auxDelete")}
                      <input
                        type="checkbox"
                        checked={entries.get(entryKey("TABLE", table.tableName, null))?.auxDelete ?? false}
                        onChange={(e) =>
                          handleAuxChange("TABLE", s.schemaName, table.tableName, "auxDelete", e.target.checked)
                        }
                        data-testid={`permissions-table-${s.schemaName}-${table.tableName}-aux-delete-checkbox`}
                      />
                    </label>
                    <ul>
                      {table.columns.map((column) => (
                        <li
                          key={column.columnName}
                          data-testid={`permissions-column-${s.schemaName}-${table.tableName}-${column.columnName}`}
                        >
                          {column.columnName}
                          <Select
                            options={PRIMARY_OPTIONS}
                            value={
                              entries.get(entryKey("COLUMN", table.tableName, column.columnName))?.primaryLevel ?? ""
                            }
                            onChange={(value) =>
                              handlePrimaryChange(
                                "COLUMN",
                                s.schemaName,
                                table.tableName,
                                column.columnName,
                                value,
                              )
                            }
                            data-testid={`permissions-column-${s.schemaName}-${table.tableName}-${column.columnName}-primary-select`}
                          />
                        </li>
                      ))}
                    </ul>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
