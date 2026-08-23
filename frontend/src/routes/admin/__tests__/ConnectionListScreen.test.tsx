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
import { ConnectionListScreen } from "../ConnectionListScreen";
import "../../../i18n/i18n";

describe("ConnectionListScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("lists connections returned by the API", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [
        {
          id: 1,
          name: "conn1",
          rdbmsType: "MYSQL",
          host: "localhost",
          port: 3306,
          databaseName: "db",
          status: "ACTIVE",
        },
      ],
    }) as unknown as typeof fetch;

    render(<ConnectionListScreen />);

    await waitFor(() => expect(screen.getByText("conn1")).toBeInTheDocument());
    expect(screen.getByTestId("connections-row-1-deactivate-button")).toBeInTheDocument();
    expect(screen.getByTestId("connections-row-1-import-button")).toBeInTheDocument();
  });

  it("submits a registration and reloads the list", async () => {
    let listCallCount = 0;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/api/admin/connections") && init?.method === "POST") {
        return Promise.resolve({ ok: true });
      }
      if (url.endsWith("/api/admin/connections")) {
        listCallCount += 1;
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<ConnectionListScreen />);
    await waitFor(() => expect(listCallCount).toBe(1));

    await userEvent.click(screen.getByTestId("connections-register-button"));
    await userEvent.type(screen.getByTestId("connections-register-form-name-input"), "conn1");
    await userEvent.type(screen.getByTestId("connections-register-form-host-input"), "localhost");
    await userEvent.type(screen.getByTestId("connections-register-form-database-input"), "db");
    await userEvent.type(screen.getByTestId("connections-register-form-username-input"), "user");
    await userEvent.type(screen.getByTestId("connections-register-form-password-input"), "pass");
    await userEvent.click(screen.getByTestId("connections-register-form-submit-button"));

    await waitFor(() => expect(listCallCount).toBe(2));
  });

  it("submits an edit with the password left blank and reloads the list", async () => {
    let listCallCount = 0;
    let putBody: string | undefined;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/api/admin/connections/1") && init?.method === "PUT") {
        putBody = String(init?.body);
        return Promise.resolve({ ok: true });
      }
      if (url.endsWith("/api/admin/connections")) {
        listCallCount += 1;
        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              id: 1,
              name: "conn1",
              rdbmsType: "MYSQL",
              host: "localhost",
              port: 3306,
              databaseName: "db",
              schemaNameHint: null,
              extraParams: null,
              username: "user",
              status: "ACTIVE",
              lastSchemaImportAt: null,
            },
          ],
        });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<ConnectionListScreen />);
    await waitFor(() => expect(listCallCount).toBe(1));

    await userEvent.click(screen.getByTestId("connections-row-1-edit-button"));
    expect(screen.getByTestId("connections-edit-form-name-input")).toHaveValue("conn1");
    await userEvent.click(screen.getByTestId("connections-edit-form-submit-button"));

    await waitFor(() => expect(listCallCount).toBe(2));
    expect(putBody).toBeDefined();
    expect(JSON.parse(putBody as string).password).toBeUndefined();
  });

  it("shows the schema import result summary", async () => {
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("schema-import")) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            schemasImported: 1,
            tablesImported: 2,
            columnsImported: 5,
            removedTableRefs: ["public.old_table"],
            removedColumnRefs: [],
            failures: [],
            prunedCustomizationCount: 3,
          }),
        });
      }
      if (url.endsWith("/api/admin/connections")) {
        return Promise.resolve({
          ok: true,
          json: async () => [
            {
              id: 1,
              name: "conn1",
              rdbmsType: "MYSQL",
              host: "localhost",
              port: 3306,
              databaseName: "db",
              status: "ACTIVE",
            },
          ],
        });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<ConnectionListScreen />);
    await waitFor(() => expect(screen.getByTestId("connections-row-1-import-button")).toBeInTheDocument());

    await userEvent.click(screen.getByTestId("connections-row-1-import-button"));

    await waitFor(() => expect(screen.getByTestId("connections-import-result")).toBeInTheDocument());
    expect(screen.getByText("public.old_table")).toBeInTheDocument();
    expect(screen.getByTestId("connections-import-result-pruned-customizations")).toBeInTheDocument();
  });
});
