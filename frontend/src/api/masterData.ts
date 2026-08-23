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

import { authenticatedJson } from "./auth";

export type FilterOperator = "EQ" | "NE" | "LT" | "LE" | "GT" | "GE" | "LIKE" | "IS_NULL" | "IS_NOT_NULL";
export type SortDirection = "ASC" | "DESC";
export type PrimaryLevel = "NONE" | "READ" | "UPDATE";
export type InputWidget = "TEXT" | "SELECT" | "CHECKBOX" | "DATE";
export type ChangeOperation = "CREATE" | "UPDATE" | "DELETE";

export interface TableSummaryDto {
  schemaName: string;
  tableName: string;
  tableType: "TABLE" | "VIEW";
  comment: string | null;
}

export interface ColumnDefDto {
  columnName: string;
  displayLabel: string;
  dataType: string;
  primaryKey: boolean;
  effectiveLevel: PrimaryLevel;
  readOnly: boolean;
  inputWidget: InputWidget;
  selectOptionsJson: string | null;
}

export interface RecordPageDto {
  columns: ColumnDefDto[];
  rows: Record<string, unknown>[];
  page: number;
  pageSize: number;
  totalCount: number;
  canCreate: boolean;
  canDelete: boolean;
}

export interface FilterConditionDto {
  columnName: string;
  operator: FilterOperator;
  value?: string;
}

export interface ListRecordsRequest {
  conditions?: FilterConditionDto[];
  rawWhereClause?: string;
  sortColumn?: string;
  sortDirection?: SortDirection;
  rawOrderByClause?: string;
  page: number;
  pageSize: number;
}

export interface RecordChangeDto {
  operation: ChangeOperation;
  primaryKeyValues: Record<string, unknown>;
  columnValues: Record<string, unknown>;
}

export interface ApplyResultDto {
  createdCount: number;
  updatedCount: number;
  deletedCount: number;
}

export function listTables(connectionId: number): Promise<TableSummaryDto[]> {
  return authenticatedJson<TableSummaryDto[]>(`/api/data/connections/${connectionId}/tables`, { method: "POST" });
}

export function listRecords(
  connectionId: number,
  schemaName: string,
  tableName: string,
  request: ListRecordsRequest,
): Promise<RecordPageDto> {
  return authenticatedJson<RecordPageDto>(
    `/api/data/connections/${connectionId}/tables/${schemaName}/${tableName}/records`,
    { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(request) },
  );
}

export function applyChanges(
  connectionId: number,
  schemaName: string,
  tableName: string,
  changes: RecordChangeDto[],
): Promise<ApplyResultDto> {
  return authenticatedJson<ApplyResultDto>(
    `/api/data/connections/${connectionId}/tables/${schemaName}/${tableName}/apply`,
    { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ changes }) },
  );
}
