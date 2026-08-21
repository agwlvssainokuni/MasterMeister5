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
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AppLayout } from "../AppLayout";
import { AuthProvider } from "../../auth/AuthContext";
import "../../i18n/i18n";

describe("AppLayout", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("renders the AppShell with the outlet content", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 }) as unknown as typeof fetch;

    render(
      <MemoryRouter initialEntries={["/"]}>
        <AuthProvider>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<div>page content</div>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByTestId("app-shell")).toBeInTheDocument());
    expect(screen.getByText("page content")).toBeInTheDocument();
    expect(screen.getByText("ホーム")).toBeInTheDocument();
  });

  it("shows the admin-only nav item once an ADMIN user is loaded", async () => {
    const user = { id: 1, email: "admin@example.com", name: "Admin", role: "ADMIN" };
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => ({ accessToken: "tok", user }) }) as unknown as typeof fetch;

    render(
      <MemoryRouter initialEntries={["/"]}>
        <AuthProvider>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<div>page content</div>} />
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByText("ユーザー管理")).toBeInTheDocument());
  });
});
