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
import { PermissionScreen } from "../PermissionScreen";
import "../../../i18n/i18n";

const CONNECTIONS = [
  { id: 1, name: "conn1", rdbmsType: "MYSQL", host: "localhost", port: 3306, databaseName: "db", status: "ACTIVE" },
];
const USERS = [{ id: 5, email: "a@example.com", name: "Alice" }];
const SCHEMA = [
  {
    schemaName: "public",
    tables: [
      {
        tableName: "t1",
        tableType: "TABLE",
        comment: null,
        columns: [{ columnName: "id", dataType: "BIGINT", nullable: false, primaryKey: true, comment: null }],
      },
    ],
  },
];

/** Routes fetch calls by URL substring (and optional method), most-specific entry first. */
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

describe("PermissionScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("renders the schema tree after selecting a connection and subject", async () => {
    installFetch([
      { url: "/api/connections/1/schema", respond: async () => ({ ok: true, json: async () => SCHEMA }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
      { url: "/api/admin/users", respond: async () => ({ ok: true, json: async () => USERS }) },
      { url: "/api/admin/groups", respond: async () => ({ ok: true, json: async () => [] }) },
      { url: "/api/admin/permissions", respond: async () => ({ ok: true, json: async () => [] }) },
    ]);

    render(<PermissionScreen />);

    await waitFor(() => expect(screen.getByTestId("permissions-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("permissions-connection-select"), "1");
    await userEvent.selectOptions(screen.getByTestId("permissions-subject-select"), "5");

    await waitFor(() => expect(screen.getByTestId("permissions-tree")).toBeInTheDocument());
    expect(screen.getByTestId("permissions-table-public-t1")).toBeInTheDocument();
    expect(screen.getByTestId("permissions-column-public-t1-id")).toBeInTheDocument();
  });

  it("submits a primary permission change for the selected schema", async () => {
    let primaryCallBody: string | null = null;
    installFetch([
      {
        url: "/api/admin/permissions/primary",
        method: "POST",
        respond: async () => {
          return { ok: true };
        },
      },
      { url: "/api/connections/1/schema", respond: async () => ({ ok: true, json: async () => SCHEMA }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
      { url: "/api/admin/users", respond: async () => ({ ok: true, json: async () => USERS }) },
      { url: "/api/admin/groups", respond: async () => ({ ok: true, json: async () => [] }) },
      { url: "/api/admin/permissions", respond: async () => ({ ok: true, json: async () => [] }) },
    ]);
    // Capture the request body for the primary-permission POST specifically.
    const originalMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes("/api/admin/permissions/primary")) {
        primaryCallBody = String(init?.body);
      }
      return originalMock(input, init);
    }) as unknown as typeof fetch;

    render(<PermissionScreen />);
    await waitFor(() => expect(screen.getByTestId("permissions-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("permissions-connection-select"), "1");
    await userEvent.selectOptions(screen.getByTestId("permissions-subject-select"), "5");
    await waitFor(() => expect(screen.getByTestId("permissions-schema-public-primary-select")).toBeInTheDocument());

    await userEvent.selectOptions(screen.getByTestId("permissions-schema-public-primary-select"), "READ");

    await waitFor(() => expect(primaryCallBody).not.toBeNull());
    expect(JSON.parse(primaryCallBody as unknown as string)).toMatchObject({
      subjectType: "USER",
      subjectId: 5,
      connectionId: 1,
      resourceLevel: "SCHEMA",
      schemaName: "public",
      primaryLevel: "READ",
    });
  });

  it("shows an error message when YAML import fails", async () => {
    installFetch([
      {
        url: "/api/admin/connections/1/permissions/import",
        respond: async () => ({
          ok: false,
          status: 400,
          json: async () => ({ errorCode: "PERMISSION_DUPLICATE_ENTRY", message: "duplicate" }),
        }),
      },
      { url: "/api/connections/1/schema", respond: async () => ({ ok: true, json: async () => SCHEMA }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
      { url: "/api/admin/users", respond: async () => ({ ok: true, json: async () => USERS }) },
      { url: "/api/admin/groups", respond: async () => ({ ok: true, json: async () => [] }) },
      { url: "/api/admin/permissions", respond: async () => ({ ok: true, json: async () => [] }) },
    ]);

    render(<PermissionScreen />);
    await waitFor(() => expect(screen.getByTestId("permissions-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("permissions-connection-select"), "1");

    const file = new File(["entries: []"], "permissions.yaml", { type: "application/x-yaml" });
    await userEvent.upload(screen.getByTestId("permissions-import-input"), file);

    await waitFor(() => expect(screen.getByText("duplicate")).toBeInTheDocument());
  });
});
