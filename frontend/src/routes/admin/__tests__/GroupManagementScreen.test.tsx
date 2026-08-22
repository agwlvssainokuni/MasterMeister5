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
import { GroupManagementScreen } from "../GroupManagementScreen";
import "../../../i18n/i18n";

describe("GroupManagementScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("lists groups returned by the API", async () => {
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/admin/groups")) {
        return Promise.resolve({ ok: true, json: async () => [{ id: 1, name: "sales", memberCount: 2 }] });
      }
      if (url.includes("/api/admin/users")) {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<GroupManagementScreen />);

    await waitFor(() => expect(screen.getByText("sales")).toBeInTheDocument());
    expect(screen.getByTestId("groups-row-1-select-button")).toBeInTheDocument();
  });

  it("submits a group creation and reloads the list", async () => {
    let listCallCount = 0;
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes("/api/admin/users")) {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      if (url.includes("/api/admin/groups") && init?.method === "POST") {
        return Promise.resolve({ ok: true });
      }
      if (url.includes("/api/admin/groups")) {
        listCallCount += 1;
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<GroupManagementScreen />);
    await waitFor(() => expect(listCallCount).toBe(1));

    await userEvent.click(screen.getByTestId("groups-create-button"));
    await userEvent.type(screen.getByTestId("groups-create-form-name-input"), "sales");
    await userEvent.click(screen.getByTestId("groups-create-form-submit-button"));

    await waitFor(() => expect(listCallCount).toBe(2));
  });

  it("shows members when a group is selected", async () => {
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/admin/users")) {
        return Promise.resolve({ ok: true, json: async () => [] });
      }
      if (url.includes("/api/admin/groups/1/members")) {
        return Promise.resolve({
          ok: true,
          json: async () => [{ userId: 10, email: "a@example.com", name: "Alice" }],
        });
      }
      if (url.includes("/api/admin/groups")) {
        return Promise.resolve({ ok: true, json: async () => [{ id: 1, name: "sales", memberCount: 1 }] });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(<GroupManagementScreen />);
    await waitFor(() => expect(screen.getByTestId("groups-row-1-select-button")).toBeInTheDocument());

    await userEvent.click(screen.getByTestId("groups-row-1-select-button"));

    await waitFor(() => expect(screen.getByTestId("groups-member-10")).toBeInTheDocument());
    expect(screen.getByText(/a@example.com/)).toBeInTheDocument();
  });
});
