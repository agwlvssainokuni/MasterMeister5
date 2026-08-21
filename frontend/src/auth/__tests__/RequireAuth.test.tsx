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
import { AuthProvider } from "../AuthContext";
import { RequireAuth } from "../RequireAuth";

function renderWithAuth(initialPath: string, adminOnly = false) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>login screen</div>} />
          <Route path="/" element={<div>home screen</div>} />
          <Route element={<RequireAuth role={adminOnly ? "ADMIN" : undefined} />}>
            <Route path="/protected" element={<div>protected screen</div>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("RequireAuth", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("redirects to /login when there is no authenticated user", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 }) as unknown as typeof fetch;

    renderWithAuth("/protected");

    await waitFor(() => expect(screen.getByText("login screen")).toBeInTheDocument());
  });

  it("renders the protected route once the silent refresh succeeds", async () => {
    const user = { id: 1, email: "user@example.com", name: "Taro", role: "GENERAL" };
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => ({ accessToken: "tok", user }) }) as unknown as typeof fetch;

    renderWithAuth("/protected");

    await waitFor(() => expect(screen.getByText("protected screen")).toBeInTheDocument());
  });

  it("redirects a non-admin user away from an admin-only route", async () => {
    const user = { id: 1, email: "user@example.com", name: "Taro", role: "GENERAL" };
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => ({ accessToken: "tok", user }) }) as unknown as typeof fetch;

    renderWithAuth("/protected", true);

    await waitFor(() => expect(screen.getByText("home screen")).toBeInTheDocument());
  });
});
