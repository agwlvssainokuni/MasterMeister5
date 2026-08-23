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
import { Badge, Button, Card, FormField, Icon, RadioGroup, Select } from "make-you-chic-ui";
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
import "./PermissionScreen.css";

const PRIMARY_OPTIONS: { label: string; value: string }[] = [
  { label: "-", value: "" },
  { label: "NONE", value: "NONE" },
  { label: "READ", value: "READ" },
  { label: "UPDATE", value: "UPDATE" },
];

// "-" means unset (falls back to the enclosing schema/table, per BR-11),
// distinct from an explicit false — a plain checkbox can't represent that.
const AUX_OPTIONS: { label: string; value: string }[] = [
  { label: "-", value: "" },
  { label: "true", value: "true" },
  { label: "false", value: "false" },
];

function auxValueToOption(value: boolean | null | undefined): string {
  if (value === true) return "true";
  if (value === false) return "false";
  return "";
}

function auxOptionToValue(option: string): boolean | undefined {
  if (option === "true") return true;
  if (option === "false") return false;
  return undefined;
}

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
  const [collapsedSchemas, setCollapsedSchemas] = useState<Set<string>>(new Set());
  const [collapsedTables, setCollapsedTables] = useState<Set<string>>(new Set());

  useEffect(() => {
    listConnections().then((all) => setConnections(all.filter((c) => c.status === "ACTIVE")));
    listUsers().then(setUsers);
    listGroups().then(setGroups);
  }, []);

  const reloadTree = useCallback(() => {
    if (!selectedConnectionId) {
      setSchema([]);
      setCollapsedSchemas(new Set());
      setCollapsedTables(new Set());
      return;
    }
    getSchema(Number(selectedConnectionId)).then((result) => {
      setSchema(result);
      // Start fully collapsed (schemas and tables alike) so a large schema
      // doesn't dump its whole table/column tree on screen at once.
      setCollapsedSchemas(new Set(result.map((s) => s.schemaName)));
      setCollapsedTables(
        new Set(result.flatMap((s) => s.tables.map((table) => `${s.schemaName}.${table.tableName}`))),
      );
    });
  }, [selectedConnectionId]);

  function toggleSchema(schemaName: string) {
    setCollapsedSchemas((prev) => {
      const next = new Set(prev);
      if (next.has(schemaName)) {
        next.delete(schemaName);
      } else {
        next.add(schemaName);
      }
      return next;
    });
  }

  function toggleTable(schemaName: string, tableName: string) {
    const key = `${schemaName}.${tableName}`;
    setCollapsedTables((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

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
    if (!selectedConnectionId || !selectedSubjectId) {
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
      // "-" clears the override back to unset (inherit), not a no-op.
      primaryLevel: value ? (value as PrimaryLevel) : null,
    });
    reloadEntries();
  }

  async function handleAuxChange(
    resourceLevel: "SCHEMA" | "TABLE",
    schemaName: string,
    tableName: string | undefined,
    field: "auxCreate" | "auxDelete",
    value: boolean | undefined,
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
      auxCreate: field === "auxCreate" ? value : (existing?.auxCreate ?? undefined),
      auxDelete: field === "auxDelete" ? value : (existing?.auxDelete ?? undefined),
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

      <Card className="mm5-permissions-controls">
        <FormField label={t("admin.permissions.connection")}>
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
        </FormField>

        <FormField label={t("admin.permissions.subjectTypeLabel")}>
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
        </FormField>

        <FormField label={t(`admin.permissions.subjectType.${subjectType.toLowerCase()}`)}>
          <Select
            options={subjectOptions}
            value={selectedSubjectId}
            onChange={setSelectedSubjectId}
            data-testid="permissions-subject-select"
          />
        </FormField>
      </Card>

      {selectedConnectionId && (
        <div className="mm5-permissions-actions">
          <Button onClick={handleExport} data-testid="permissions-export-button">
            {t("admin.permissions.exportButton")}
          </Button>
          <FormField label={t("admin.permissions.importButton")}>
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
          </FormField>
          {importErrorMessage && <p role="alert">{importErrorMessage}</p>}
        </div>
      )}

      {selectedConnectionId && schema.length === 0 && (
        <p data-testid="permissions-no-schema-message">{t("admin.permissions.noSchemaImported")}</p>
      )}

      {readyToEdit && (
        <div className="mm5-permissions-tree" data-testid="permissions-tree">
          {schema.map((s) => {
            const schemaCollapsed = collapsedSchemas.has(s.schemaName);
            return (
              <div key={s.schemaName} className="mm5-permissions-schema">
                <div
                  className="mm5-permissions-row mm5-permissions-schema-row"
                  data-testid={`permissions-schema-${s.schemaName}`}
                >
                  <button
                    type="button"
                    className={schemaCollapsed ? "mm5-permissions-toggle collapsed" : "mm5-permissions-toggle"}
                    onClick={() => toggleSchema(s.schemaName)}
                    aria-label={schemaCollapsed ? t("admin.permissions.expand") : t("admin.permissions.collapse")}
                  >
                    <Icon name="chevron-down" size={16} />
                  </button>
                  <span className="mm5-permissions-name">{s.schemaName}</span>
                  <span className="mm5-permissions-primary-select">
                    <Select
                      aria-label={t("admin.permissions.primaryLevelFor", { name: s.schemaName })}
                      options={PRIMARY_OPTIONS}
                      value={entries.get(entryKey("SCHEMA", null, null))?.primaryLevel ?? ""}
                      onChange={(value) => handlePrimaryChange("SCHEMA", s.schemaName, undefined, undefined, value)}
                      data-testid={`permissions-schema-${s.schemaName}-primary-select`}
                    />
                  </span>
                  <span className="mm5-permissions-aux">
                    <span className="mm5-permissions-aux-item">
                      <span className="mm5-permissions-aux-label">{t("admin.permissions.auxCreate")}</span>
                      <Select
                        aria-label={t("admin.permissions.auxCreate")}
                        options={AUX_OPTIONS}
                        value={auxValueToOption(entries.get(entryKey("SCHEMA", null, null))?.auxCreate)}
                        onChange={(value) =>
                          handleAuxChange("SCHEMA", s.schemaName, undefined, "auxCreate", auxOptionToValue(value))
                        }
                        data-testid={`permissions-schema-${s.schemaName}-aux-create-select`}
                      />
                    </span>
                    <span className="mm5-permissions-aux-item">
                      <span className="mm5-permissions-aux-label">{t("admin.permissions.auxDelete")}</span>
                      <Select
                        aria-label={t("admin.permissions.auxDelete")}
                        options={AUX_OPTIONS}
                        value={auxValueToOption(entries.get(entryKey("SCHEMA", null, null))?.auxDelete)}
                        onChange={(value) =>
                          handleAuxChange("SCHEMA", s.schemaName, undefined, "auxDelete", auxOptionToValue(value))
                        }
                        data-testid={`permissions-schema-${s.schemaName}-aux-delete-select`}
                      />
                    </span>
                  </span>
                </div>

                {!schemaCollapsed &&
                  s.tables.map((table) => {
                    const tableCollapsed = collapsedTables.has(`${s.schemaName}.${table.tableName}`);
                    return (
                      <div key={table.tableName}>
                        <div
                          className="mm5-permissions-row mm5-permissions-table-row"
                          data-testid={`permissions-table-${s.schemaName}-${table.tableName}`}
                        >
                          <button
                            type="button"
                            className={
                              tableCollapsed ? "mm5-permissions-toggle collapsed" : "mm5-permissions-toggle"
                            }
                            onClick={() => toggleTable(s.schemaName, table.tableName)}
                            aria-label={
                              tableCollapsed ? t("admin.permissions.expand") : t("admin.permissions.collapse")
                            }
                          >
                            <Icon name="chevron-down" size={16} />
                          </button>
                          <span className="mm5-permissions-name">{table.tableName}</span>
                          <span className="mm5-permissions-primary-select">
                            <Select
                              aria-label={t("admin.permissions.primaryLevelFor", { name: table.tableName })}
                              options={PRIMARY_OPTIONS}
                              value={entries.get(entryKey("TABLE", table.tableName, null))?.primaryLevel ?? ""}
                              onChange={(value) =>
                                handlePrimaryChange("TABLE", s.schemaName, table.tableName, undefined, value)
                              }
                              data-testid={`permissions-table-${s.schemaName}-${table.tableName}-primary-select`}
                            />
                          </span>
                          <span className="mm5-permissions-aux">
                            <span className="mm5-permissions-aux-item">
                              <span className="mm5-permissions-aux-label">{t("admin.permissions.auxCreate")}</span>
                              <Select
                                aria-label={t("admin.permissions.auxCreate")}
                                options={AUX_OPTIONS}
                                value={auxValueToOption(
                                  entries.get(entryKey("TABLE", table.tableName, null))?.auxCreate,
                                )}
                                onChange={(value) =>
                                  handleAuxChange(
                                    "TABLE",
                                    s.schemaName,
                                    table.tableName,
                                    "auxCreate",
                                    auxOptionToValue(value),
                                  )
                                }
                                data-testid={`permissions-table-${s.schemaName}-${table.tableName}-aux-create-select`}
                              />
                            </span>
                            <span className="mm5-permissions-aux-item">
                              <span className="mm5-permissions-aux-label">{t("admin.permissions.auxDelete")}</span>
                              <Select
                                aria-label={t("admin.permissions.auxDelete")}
                                options={AUX_OPTIONS}
                                value={auxValueToOption(
                                  entries.get(entryKey("TABLE", table.tableName, null))?.auxDelete,
                                )}
                                onChange={(value) =>
                                  handleAuxChange(
                                    "TABLE",
                                    s.schemaName,
                                    table.tableName,
                                    "auxDelete",
                                    auxOptionToValue(value),
                                  )
                                }
                                data-testid={`permissions-table-${s.schemaName}-${table.tableName}-aux-delete-select`}
                              />
                            </span>
                          </span>
                        </div>

                        {!tableCollapsed &&
                          table.columns.map((column) => (
                            <div
                              key={column.columnName}
                              className="mm5-permissions-row mm5-permissions-column-row"
                              data-testid={`permissions-column-${s.schemaName}-${table.tableName}-${column.columnName}`}
                            >
                              <span className="mm5-permissions-name">{column.columnName}</span>
                              <span className="mm5-permissions-badges">
                                {column.primaryKey && <Badge variant="primary">PK</Badge>}
                                {column.foreignKey && <Badge variant="secondary">FK</Badge>}
                                {!column.nullable && <Badge variant="secondary">NOT NULL</Badge>}
                              </span>
                              <span className="mm5-permissions-primary-select">
                                <Select
                                  aria-label={t("admin.permissions.primaryLevelFor", { name: column.columnName })}
                                  options={PRIMARY_OPTIONS}
                                  value={
                                    entries.get(entryKey("COLUMN", table.tableName, column.columnName))
                                      ?.primaryLevel ?? ""
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
                              </span>
                              <span className="mm5-permissions-aux" aria-hidden="true" />
                            </div>
                          ))}
                      </div>
                    );
                  })}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
