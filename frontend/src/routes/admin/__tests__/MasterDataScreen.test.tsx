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
import { ToastProvider } from "make-you-chic-ui";
import { MasterDataScreen } from "../MasterDataScreen";
import "../../../i18n/i18n";

const CONNECTIONS = [
  { id: 1, name: "conn1", rdbmsType: "MYSQL", host: "localhost", port: 3306, databaseName: "db", status: "ACTIVE" },
];
const TABLES = [{ schemaName: "public", tableName: "t1", tableType: "TABLE", comment: null }];
const COLUMNS = [
  {
    columnName: "id",
    displayLabel: "id",
    dataType: "BIGINT",
    primaryKey: true,
    effectiveLevel: "READ",
    readOnly: true,
    inputWidget: "TEXT",
    selectOptionsJson: null,
  },
  {
    columnName: "name",
    displayLabel: "name",
    dataType: "VARCHAR",
    primaryKey: false,
    effectiveLevel: "UPDATE",
    readOnly: false,
    inputWidget: "TEXT",
    selectOptionsJson: null,
  },
];
const TABLE_METADATA = { columns: COLUMNS, primaryKeyColumns: ["id"], canCreate: true, canDelete: true };
const RECORD_PAGE = {
  rows: [{ id: 1, name: "Alice" }],
  page: 0,
  pageSize: 50,
  totalCount: 1,
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

describe("MasterDataScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("renders records after selecting a connection and table", async () => {
    installFetch([
      { url: "/tables/public/t1/records", method: "POST", respond: async () => ({ ok: true, json: async () => RECORD_PAGE }) },
      { url: "/tables/public/t1/metadata", respond: async () => ({ ok: true, json: async () => TABLE_METADATA }) },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(<MasterDataScreen />);

    await waitFor(() => expect(screen.getByTestId("master-data-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("master-data-table-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-table-select"), "public.t1");

    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());
  });

  it("submits an apply request when a delete is confirmed", async () => {
    let applyCallBody: string | null = null;
    installFetch([
      {
        url: "/apply",
        method: "POST",
        respond: async () => ({ ok: true, json: async () => ({ createdCount: 0, updatedCount: 0, deletedCount: 1 }) }),
      },
      { url: "/tables/public/t1/records", method: "POST", respond: async () => ({ ok: true, json: async () => RECORD_PAGE }) },
      { url: "/tables/public/t1/metadata", respond: async () => ({ ok: true, json: async () => TABLE_METADATA }) },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);
    const original = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes("/apply")) {
        applyCallBody = String(init?.body);
      }
      return original(input, init);
    }) as unknown as typeof fetch;

    render(<MasterDataScreen />);
    await waitFor(() => expect(screen.getByTestId("master-data-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("master-data-table-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-table-select"), "public.t1");
    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());

    const deleteButton = screen.getByTestId(`master-data-row-${JSON.stringify([1])}-delete-button`);
    await userEvent.click(deleteButton);
    await userEvent.click(screen.getByTestId("master-data-apply-button"));

    await waitFor(() => expect(applyCallBody).not.toBeNull());
    expect(JSON.parse(applyCallBody as unknown as string)).toEqual({
      changes: [{ operation: "DELETE", primaryKeyValues: { id: 1 }, columnValues: {} }],
    });
  });

  it("still identifies rows for delete when the primary key column itself has no read permission", async () => {
    let applyCallBody: string | null = null;
    installFetch([
      {
        url: "/apply",
        method: "POST",
        respond: async () => ({ ok: true, json: async () => ({ createdCount: 0, updatedCount: 0, deletedCount: 1 }) }),
      },
      { url: "/tables/public/t1/records", method: "POST", respond: async () => ({ ok: true, json: async () => RECORD_PAGE }) },
      {
        url: "/tables/public/t1/metadata",
        respond: async () => ({
          ok: true,
          // "id" (the PK) is excluded from `columns` — no READ permission on it — but must
          // still appear in `primaryKeyColumns` so the row stays identifiable for delete.
          json: async () => ({
            columns: COLUMNS.filter((c) => c.columnName !== "id"),
            primaryKeyColumns: ["id"],
            canCreate: true,
            canDelete: true,
          }),
        }),
      },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);
    const original = globalThis.fetch as unknown as ReturnType<typeof vi.fn>;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).includes("/apply")) {
        applyCallBody = String(init?.body);
      }
      return original(input, init);
    }) as unknown as typeof fetch;

    render(<MasterDataScreen />);
    await waitFor(() => expect(screen.getByTestId("master-data-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("master-data-table-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-table-select"), "public.t1");
    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());

    const deleteButton = screen.getByTestId(`master-data-row-${JSON.stringify([1])}-delete-button`);
    await userEvent.click(deleteButton);
    await userEvent.click(screen.getByTestId("master-data-apply-button"));

    await waitFor(() => expect(applyCallBody).not.toBeNull());
    expect(JSON.parse(applyCallBody as unknown as string)).toEqual({
      changes: [{ operation: "DELETE", primaryKeyValues: { id: 1 }, columnValues: {} }],
    });
  });

  it("hides the create and delete controls when the user lacks that permission", async () => {
    installFetch([
      {
        url: "/tables/public/t1/records",
        method: "POST",
        respond: async () => ({ ok: true, json: async () => RECORD_PAGE }),
      },
      {
        url: "/tables/public/t1/metadata",
        respond: async () => ({
          ok: true,
          json: async () => ({ columns: COLUMNS, primaryKeyColumns: ["id"], canCreate: false, canDelete: false }),
        }),
      },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(<MasterDataScreen />);
    await waitFor(() => expect(screen.getByTestId("master-data-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("master-data-table-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-table-select"), "public.t1");

    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());
    expect(screen.queryByTestId("master-data-create-button")).not.toBeInTheDocument();
    expect(screen.queryByTestId(`master-data-row-${JSON.stringify([1])}-delete-button`)).not.toBeInTheDocument();
  });

  it("shows a toast when applying changes is rejected by the API", async () => {
    installFetch([
      {
        url: "/apply",
        method: "POST",
        respond: async () => ({
          ok: false,
          json: async () => ({ errorCode: "MASTER_DATA_PERMISSION_DENIED", message: "権限がありません。" }),
        }),
      },
      { url: "/tables/public/t1/records", method: "POST", respond: async () => ({ ok: true, json: async () => RECORD_PAGE }) },
      { url: "/tables/public/t1/metadata", respond: async () => ({ ok: true, json: async () => TABLE_METADATA }) },
      { url: "/tables", method: "POST", respond: async () => ({ ok: true, json: async () => TABLES }) },
      { url: "/api/connections", respond: async () => ({ ok: true, json: async () => CONNECTIONS }) },
    ]);

    render(
      <ToastProvider>
        <MasterDataScreen />
      </ToastProvider>,
    );
    await waitFor(() => expect(screen.getByTestId("master-data-connection-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-connection-select"), "1");
    await waitFor(() => expect(screen.getByTestId("master-data-table-select")).toBeInTheDocument());
    await userEvent.selectOptions(screen.getByTestId("master-data-table-select"), "public.t1");
    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());

    const deleteButton = screen.getByTestId(`master-data-row-${JSON.stringify([1])}-delete-button`);
    await userEvent.click(deleteButton);
    await userEvent.click(screen.getByTestId("master-data-apply-button"));

    await waitFor(() => expect(screen.getByText("権限がありません。")).toBeInTheDocument());
  });
});
