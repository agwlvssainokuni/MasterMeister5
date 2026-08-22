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
import { CustomizationScreen } from "../CustomizationScreen";
import "../../../i18n/i18n";

const CONNECTIONS = [
  { id: 1, name: "conn1", rdbmsType: "MYSQL", host: "localhost", port: 3306, databaseName: "db", status: "ACTIVE" },
];
const TABLES = [{ schemaName: "public", tableName: "t1", tableType: "TABLE", comment: null }];

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

describe("CustomizationScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("lists tables after selecting a connection", async () => {
    installFetch([
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/admin/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(<CustomizationScreen />);

    await waitFor(() => expect(screen.getByTestId("customizations-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("customizations-connection-select"), "1");

    await waitFor(() => expect(screen.getByText("t1")).toBeInTheDocument());
  });

  it("shows an error message when YAML import fails", async () => {
    installFetch([
      {
        url: "/customizations/import",
        respond: async () => ({
          ok: false,
          status: 400,
          json: async () => ({ errorCode: "MASTER_DATA_INVALID_IDENTIFIER", message: "bad identifier" }),
        }),
      },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/admin/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(<CustomizationScreen />);
    await waitFor(() => expect(screen.getByTestId("customizations-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("customizations-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("customizations-import-input")).toBeInTheDocument());

    const file = new File(["tables: []"], "customizations.yaml", { type: "application/x-yaml" });
    await userEvent.upload(screen.getByTestId("customizations-import-input"), file);

    await waitFor(() => expect(screen.getByText("bad identifier")).toBeInTheDocument());
  });
});
