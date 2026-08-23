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

import { authenticatedJson, authenticatedVoid } from "./auth";

export type RdbmsType = "MYSQL" | "MARIADB" | "POSTGRESQL" | "H2";
export type ConnectionStatus = "ACTIVE" | "DEACTIVATED";

export interface ConnectionSummaryDto {
  id: number;
  name: string;
  rdbmsType: RdbmsType;
  host: string;
  port: number;
  databaseName: string;
  status: ConnectionStatus;
}

export interface RegisterConnectionRequest {
  name: string;
  rdbmsType: RdbmsType;
  host: string;
  port: number;
  databaseName: string;
  schemaNameHint?: string;
  /** Appended verbatim to the JDBC URL (e.g. "?useSSL=true&serverTimezone=UTC" for MySQL, ";MODE=MySQL" for H2). */
  extraParams?: string;
  username: string;
  password: string;
}

export interface SchemaImportFailureDto {
  schemaName: string;
  reasonCode: string;
}

export interface SchemaImportResultDto {
  schemasImported: number;
  tablesImported: number;
  columnsImported: number;
  removedTableRefs: string[];
  removedColumnRefs: string[];
  failures: SchemaImportFailureDto[];
  prunedCustomizationCount: number;
}

export interface ColumnViewDto {
  columnName: string;
  dataType: string;
  nullable: boolean;
  primaryKey: boolean;
  comment: string | null;
}

export interface TableViewDto {
  tableName: string;
  tableType: "TABLE" | "VIEW";
  comment: string | null;
  columns: ColumnViewDto[];
}

export interface SchemaViewDto {
  schemaName: string;
  tables: TableViewDto[];
}

/**
 * Read-only listing (backend/ConnectionViewController), available to every
 * authenticated user — not just ADMIN — since MasterDataScreen and
 * QueryScreen need it too, not only the ADMIN-only ConnectionListScreen/
 * PermissionScreen. Registering/deactivating/reactivating a connection
 * remain ADMIN-only (`/api/admin/connections/**`, below).
 */
export function listConnections(): Promise<ConnectionSummaryDto[]> {
  return authenticatedJson<ConnectionSummaryDto[]>("/api/connections");
}

export function registerConnection(request: RegisterConnectionRequest): Promise<void> {
  return authenticatedVoid("/api/admin/connections", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function deactivateConnection(connectionId: number): Promise<void> {
  return authenticatedVoid(`/api/admin/connections/${connectionId}/deactivate`, { method: "POST" });
}

export function reactivateConnection(connectionId: number): Promise<void> {
  return authenticatedVoid(`/api/admin/connections/${connectionId}/reactivate`, { method: "POST" });
}

export function importSchema(connectionId: number): Promise<SchemaImportResultDto> {
  return authenticatedJson<SchemaImportResultDto>(
    `/api/admin/connections/${connectionId}/schema-import`,
    { method: "POST" },
  );
}

/**
 * Read-only schema tree (backend/ConnectionViewController), available to
 * every authenticated user — used by Unit 4's PermissionScreen (ADMIN-only
 * screen, but the endpoint itself isn't role-restricted) and Unit 6's
 * QueryScreen (schema picker, general users).
 */
export function getSchema(connectionId: number): Promise<SchemaViewDto[]> {
  return authenticatedJson<SchemaViewDto[]>(`/api/connections/${connectionId}/schema`);
}
