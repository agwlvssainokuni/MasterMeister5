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

/**
 * component-methods.md `buildSql`/`parseSqlToBuilderState` (Functional
 * Design Question 1): implemented entirely in the frontend, the backend
 * never sees a QueryBuilderState — only the resulting {@code sqlText}.
 */
export interface QueryBuilderState {
  select: string;
  from: string;
  join: string;
  where: string;
  groupBy: string;
  having: string;
  orderBy: string;
  limitOffset: string;
}

export const EMPTY_QUERY_BUILDER_STATE: QueryBuilderState = {
  select: "*",
  from: "",
  join: "",
  where: "",
  groupBy: "",
  having: "",
  orderBy: "",
  limitOffset: "",
};

export function buildSql(state: QueryBuilderState): string {
  const lines: string[] = [`SELECT ${state.select.trim() || "*"}`];
  if (state.from.trim()) {
    lines.push(`FROM ${state.from.trim()}`);
  }
  if (state.join.trim()) {
    lines.push(state.join.trim());
  }
  if (state.where.trim()) {
    lines.push(`WHERE ${state.where.trim()}`);
  }
  if (state.groupBy.trim()) {
    lines.push(`GROUP BY ${state.groupBy.trim()}`);
  }
  if (state.having.trim()) {
    lines.push(`HAVING ${state.having.trim()}`);
  }
  if (state.orderBy.trim()) {
    lines.push(`ORDER BY ${state.orderBy.trim()}`);
  }
  if (state.limitOffset.trim()) {
    lines.push(state.limitOffset.trim());
  }
  return lines.join("\n");
}

/**
 * business-logic-model.md Section 1: best-effort reverse parse for a simple
 * single-table SELECT statement (the shape {@link buildSql} itself produces).
 * Complex joins/subqueries are not guaranteed to round-trip — anything the
 * regex cannot confidently split is returned entirely in {@code select}.
 */
const SIMPLE_SELECT_PATTERN =
  /^SELECT\s+(.*?)\s+FROM\s+(.*?)(?:\s+((?:LEFT\s+|RIGHT\s+|INNER\s+|FULL\s+)?JOIN\s+.*?))?(?:\s+WHERE\s+(.*?))?(?:\s+GROUP BY\s+(.*?))?(?:\s+HAVING\s+(.*?))?(?:\s+ORDER BY\s+(.*?))?(?:\s+(LIMIT\s+.*))?$/is;

export function parseSqlToBuilderState(sqlText: string): QueryBuilderState {
  const normalized = sqlText.trim().replace(/\s+/g, " ");
  const match = SIMPLE_SELECT_PATTERN.exec(normalized);
  if (!match) {
    return { ...EMPTY_QUERY_BUILDER_STATE, select: sqlText };
  }
  return {
    select: match[1] ?? "",
    from: match[2] ?? "",
    join: match[3] ?? "",
    where: match[4] ?? "",
    groupBy: match[5] ?? "",
    having: match[6] ?? "",
    orderBy: match[7] ?? "",
    limitOffset: match[8] ?? "",
  };
}
