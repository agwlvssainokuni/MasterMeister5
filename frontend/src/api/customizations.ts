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

import { authenticatedFetch, authenticatedJson, ApiError } from "./auth";
import type { InputWidget, SortDirection } from "./masterData";

export interface SelectOptionDto {
  value: string;
  label: string;
}

export type ValidationRuleType = "REGEX" | "RANGE";

export interface ValidationRuleDto {
  type: ValidationRuleType;
  pattern: string | null;
  minValue: string | null;
  maxValue: string | null;
}

export interface CustomizationColumnDto {
  columnName: string;
  displayLabel: string | null;
  displayOrder: number | null;
  hidden: boolean;
  readOnly: boolean;
  inputWidget: InputWidget | null;
  selectOptions: SelectOptionDto[] | null;
  validationRules: ValidationRuleDto[];
}

export interface CustomizationDefinitionDto {
  schemaName: string;
  tableName: string;
  defaultSortColumn: string | null;
  defaultSortDirection: SortDirection | null;
  columns: CustomizationColumnDto[];
}

export function getCustomizationDefinition(
  connectionId: number,
  schemaName: string,
  tableName: string,
): Promise<CustomizationDefinitionDto> {
  return authenticatedJson<CustomizationDefinitionDto>(
    `/api/admin/connections/${connectionId}/customizations/${schemaName}/${tableName}`,
  );
}

export async function exportCustomizationDefinition(connectionId: number): Promise<string> {
  const response = await authenticatedFetch(`/api/admin/connections/${connectionId}/customizations/export`);
  if (!response.ok) {
    throw new ApiError("EXPORT_FAILED", `Request failed: ${response.status}`);
  }
  return response.text();
}

export interface ImportCustomizationResultDto {
  importedTableCount: number;
}

export async function importCustomizationDefinition(
  connectionId: number,
  yamlContent: string,
): Promise<ImportCustomizationResultDto> {
  return authenticatedJson<ImportCustomizationResultDto>(
    `/api/admin/connections/${connectionId}/customizations/import`,
    { method: "POST", headers: { "Content-Type": "application/x-yaml" }, body: yamlContent },
  );
}
