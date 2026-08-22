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

import { authenticatedFetch, authenticatedJson, authenticatedVoid, ApiError } from "./auth";

export type SubjectType = "USER" | "GROUP";
export type ResourceLevel = "SCHEMA" | "TABLE" | "COLUMN";
export type PrimaryLevel = "NONE" | "READ" | "UPDATE";

export interface PermissionEntryDto {
  subjectType: SubjectType;
  subjectId: number;
  resourceLevel: ResourceLevel;
  schemaName: string;
  tableName: string | null;
  columnName: string | null;
  primaryLevel: PrimaryLevel | null;
  auxCreate: boolean | null;
  auxDelete: boolean | null;
}

export function listPermissionEntries(
  connectionId: number,
  subjectType: SubjectType,
  subjectId: number,
): Promise<PermissionEntryDto[]> {
  const params = new URLSearchParams({
    connectionId: String(connectionId),
    subjectType,
    subjectId: String(subjectId),
  });
  return authenticatedJson<PermissionEntryDto[]>(`/api/admin/permissions?${params.toString()}`);
}

export interface SetPrimaryPermissionRequest {
  subjectType: SubjectType;
  subjectId: number;
  connectionId: number;
  resourceLevel: ResourceLevel;
  schemaName: string;
  tableName?: string;
  columnName?: string;
  primaryLevel: PrimaryLevel;
}

export function setPrimaryPermission(request: SetPrimaryPermissionRequest): Promise<void> {
  return authenticatedVoid("/api/admin/permissions/primary", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export interface SetAuxiliaryPermissionRequest {
  subjectType: SubjectType;
  subjectId: number;
  connectionId: number;
  resourceLevel: "SCHEMA" | "TABLE";
  schemaName: string;
  tableName?: string;
  auxCreate?: boolean;
  auxDelete?: boolean;
}

export function setAuxiliaryPermission(request: SetAuxiliaryPermissionRequest): Promise<void> {
  return authenticatedVoid("/api/admin/permissions/auxiliary", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export async function exportPermissions(connectionId: number): Promise<string> {
  const response = await authenticatedFetch(`/api/admin/connections/${connectionId}/permissions/export`);
  if (!response.ok) {
    throw new ApiError("EXPORT_FAILED", `Request failed: ${response.status}`);
  }
  return response.text();
}

export interface ImportPermissionsResultDto {
  importedCount: number;
}

export async function importPermissions(
  connectionId: number,
  yamlContent: string,
): Promise<ImportPermissionsResultDto> {
  return authenticatedJson<ImportPermissionsResultDto>(
    `/api/admin/connections/${connectionId}/permissions/import`,
    { method: "POST", headers: { "Content-Type": "application/x-yaml" }, body: yamlContent },
  );
}
