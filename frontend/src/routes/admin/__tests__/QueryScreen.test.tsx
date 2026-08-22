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

import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AuthProvider } from "../../../auth/AuthContext";
import { QueryScreen } from "../QueryScreen";
import "../../../i18n/i18n";

const CONNECTIONS = [
  { id: 1, name: "conn1", rdbmsType: "MYSQL", host: "localhost", port: 3306, databaseName: "db", status: "ACTIVE" },
];
const SCHEMAS = [{ schemaName: "PUBLIC", tables: [] }];
const SAVED_QUERIES = [
  {
    id: 10,
    name: "q1",
    sqlText: "SELECT id FROM t1",
    visibility: "PUBLIC",
    creatorUserId: 1,
    status: "ACTIVE",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
  },
];
const QUERY_RESULT = {
  columns: ["ID"],
  rows: [{ ID: 1 }],
  rowCount: 1,
  executionTimeMs: 5,
  truncated: false,
};

function installFetch(routes: Array<{ url: string; method?: string; respond: () => Promise<unknown> }>) {
  globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    for (const route of routes) {
      if (url.includes(route.url) && (!route.method || route.method === method)) {
        return route.respond();
      }
    }
    throw new Error(`unexpected fetch: ${method} ${url}`);
  }) as unknown as typeof fetch;
}

function renderScreen() {
  return render(
    <AuthProvider>
      <QueryScreen />
    </AuthProvider>,
  );
}

describe("QueryScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("executes a query and displays the result", async () => {
    let executeCallBody: string | null = null;
    installFetch([
      { url: "/api/auth/refresh", respond: async () => ({ ok: false, status: 401 }) },
      { url: "/api/query/saved-queries", method: "GET", respond: async () => ({ ok: true, json: async () => SAVED_QUERIES }) },
      { url: "/schema", respond: async () => ({ ok: true, json: async () => SCHEMAS }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
      { url: "/detect-parameters", respond: async () => ({ ok: true, json: async () => [] }) },
      {
        url: "/api/query/execute",
        method: "POST",
        respond: async () => {
          return { ok: true, json: async () => QUERY_RESULT };
        },
      },
    ]);
    const original = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes("/api/query/execute")) {
        executeCallBody = String(init?.body);
      }
      return original(input, init);
    }) as unknown as typeof fetch;

    renderScreen();

    await waitFor(() => expect(screen.getByTestId("query-connection-select")).toBeInTheDocument());
    await userEvent.click(screen.getAllByRole("tab", { name: "SQL直接入力" })[0]);
    await userEvent.clear(screen.getByTestId("query-raw-sql-textarea"));
    await userEvent.type(screen.getByTestId("query-raw-sql-textarea"), "SELECT 1");
    await userEvent.selectOptions(screen.getByTestId("query-connection-select"), "1");
    await waitFor(() => expect(screen.getByRole("option", { name: "PUBLIC" })).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("query-schema-select"), "PUBLIC");

    await userEvent.click(screen.getByTestId("query-execute-button"));

    await waitFor(() => expect(executeCallBody).not.toBeNull());
    expect(JSON.parse(executeCallBody as unknown as string).connectionId).toBe(1);
    expect(JSON.parse(executeCallBody as unknown as string).schemaName).toBe("PUBLIC");
    await waitFor(() => expect(screen.getByText("1")).toBeInTheDocument());
  });

  it("saves a new query with the entered name and visibility", async () => {
    let saveCallBody: string | null = null;
    installFetch([
      { url: "/api/auth/refresh", respond: async () => ({ ok: false, status: 401 }) },
      { url: "/api/query/saved-queries", method: "GET", respond: async () => ({ ok: true, json: async () => [] }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => [] }) },
      { url: "/detect-parameters", respond: async () => ({ ok: true, json: async () => [] }) },
      {
        url: "/api/query/saved-queries",
        method: "POST",
        respond: async () => ({ ok: true, json: async () => ({ savedQueryId: 99 }) }),
      },
    ]);
    const original = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes("/api/query/saved-queries") && init?.method === "POST") {
        saveCallBody = String(init?.body);
      }
      return original(input, init);
    }) as unknown as typeof fetch;

    renderScreen();

    await waitFor(() => expect(screen.getByTestId("query-save-button")).toBeInTheDocument());
    await userEvent.click(screen.getAllByRole("tab", { name: "SQL直接入力" })[0]);
    await userEvent.clear(screen.getByTestId("query-raw-sql-textarea"));
    await userEvent.type(screen.getByTestId("query-raw-sql-textarea"), "SELECT 1");
    await userEvent.click(screen.getByTestId("query-save-button"));
    await userEvent.type(screen.getByTestId("query-save-name-input"), "my query");
    await userEvent.click(screen.getByTestId("query-save-confirm-button"));

    await waitFor(() => expect(saveCallBody).not.toBeNull());
    const parsed = JSON.parse(saveCallBody as unknown as string);
    expect(parsed.name).toBe("my query");
    expect(parsed.sqlText).toBe("SELECT 1");
    expect(parsed.visibility).toBe("PRIVATE");
  });
});
