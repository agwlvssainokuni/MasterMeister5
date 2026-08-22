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

export type QueryVisibility = "PUBLIC" | "PRIVATE";
export type QueryStatus = "ACTIVE" | "RETIRED";

export interface SavedQueryDto {
  id: number;
  name: string;
  sqlText: string;
  visibility: QueryVisibility;
  creatorUserId: number;
  status: QueryStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SaveQueryRequest {
  savedQueryId?: number;
  name: string;
  sqlText: string;
  visibility: QueryVisibility;
}

export interface SavedQueryIdDto {
  savedQueryId: number;
}

export interface ParameterDescriptorDto {
  name: string;
}

export interface QueryResultDto {
  columns: string[];
  rows: Record<string, unknown>[];
  rowCount: number;
  executionTimeMs: number;
  truncated: boolean;
}

export interface ExecuteQueryRequest {
  sqlText?: string;
  savedQueryId?: number;
  connectionId: number;
  schemaName: string;
  params?: Record<string, unknown>;
}

export interface ExecutionHistoryDto {
  id: number;
  savedQueryId: number | null;
  sqlText: string;
  connectionId: number;
  schemaName: string;
  params: Record<string, unknown> | null;
  resultRowCount: number;
  executionTimeMs: number;
  executedByUserId: number;
  executedAt: string;
}

export interface ExecutionHistoryPageDto {
  content: ExecutionHistoryDto[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ExecutionHistoryFilter {
  executedByUserId?: number;
  connectionId?: number;
  schemaName?: string;
  sqlTextContains?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export function listSavedQueries(): Promise<SavedQueryDto[]> {
  return authenticatedJson<SavedQueryDto[]>("/api/query/saved-queries");
}

export function saveQuery(request: SaveQueryRequest): Promise<SavedQueryIdDto> {
  return authenticatedJson<SavedQueryIdDto>("/api/query/saved-queries", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function retireQuery(id: number): Promise<void> {
  return authenticatedVoid(`/api/query/saved-queries/${id}`, { method: "DELETE" });
}

export function detectParameters(sqlText: string): Promise<ParameterDescriptorDto[]> {
  return authenticatedJson<ParameterDescriptorDto[]>("/api/query/detect-parameters", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sqlText }),
  });
}

export function executeQuery(request: ExecuteQueryRequest): Promise<QueryResultDto> {
  return authenticatedJson<QueryResultDto>("/api/query/execute", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
}

export function listExecutionHistory(filter: ExecutionHistoryFilter = {}): Promise<ExecutionHistoryPageDto> {
  const params = new URLSearchParams();
  if (filter.executedByUserId !== undefined) params.set("executedByUserId", String(filter.executedByUserId));
  if (filter.connectionId !== undefined) params.set("connectionId", String(filter.connectionId));
  if (filter.schemaName) params.set("schemaName", filter.schemaName);
  if (filter.sqlTextContains) params.set("sqlTextContains", filter.sqlTextContains);
  if (filter.fromDate) params.set("fromDate", filter.fromDate);
  if (filter.toDate) params.set("toDate", filter.toDate);
  params.set("page", String(filter.page ?? 0));
  params.set("size", String(filter.size ?? 50));
  return authenticatedJson<ExecutionHistoryPageDto>(`/api/query/execution-history?${params.toString()}`);
}
