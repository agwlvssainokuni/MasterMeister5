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
import { QueryHistoryScreen } from "../QueryHistoryScreen";
import "../../../i18n/i18n";

const CONNECTIONS = [
  { id: 1, name: "conn1", rdbmsType: "MYSQL", host: "localhost", port: 3306, databaseName: "db", status: "ACTIVE" },
];
const HISTORY_PAGE = {
  content: [
    {
      id: 1,
      savedQueryId: null,
      sqlText: "SELECT 1",
      connectionId: 1,
      schemaName: "PUBLIC",
      params: null,
      resultRowCount: 1,
      executionTimeMs: 5,
      executedByUserId: 9,
      executedAt: "2026-01-01T00:00:00Z",
    },
  ],
  page: 0,
  size: 50,
  totalElements: 1,
};

function installFetch(routes: Array<{ url: string; respond: () => Promise<unknown> }>) {
  globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    for (const route of routes) {
      if (url.includes(route.url)) {
        return route.respond();
      }
    }
    throw new Error(`unexpected fetch: ${url}`);
  }) as unknown as typeof fetch;
}

describe("QueryHistoryScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("renders execution history rows", async () => {
    installFetch([
      { url: "/api/query/execution-history", respond: async () => ({ ok: true, json: async () => HISTORY_PAGE }) },
      { url: "/api/admin/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(<QueryHistoryScreen />);

    await waitFor(() => expect(screen.getByText("SELECT 1")).toBeInTheDocument());
  });

  it("re-fetches with the schema filter applied", async () => {
    let lastUrl = "";
    installFetch([
      {
        url: "/api/query/execution-history",
        respond: async () => {
          return { ok: true, json: async () => HISTORY_PAGE };
        },
      },
      { url: "/api/admin/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);
    const original = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      lastUrl = String(input);
      return original(input, init);
    }) as unknown as typeof fetch;

    render(<QueryHistoryScreen />);
    await waitFor(() => expect(screen.getByTestId("query-history-schema-input")).toBeInTheDocument());
    await userEvent.type(screen.getByTestId("query-history-schema-input"), "PUBLIC");

    await waitFor(() => expect(lastUrl).toContain("schemaName=PUBLIC"));
  });
});
