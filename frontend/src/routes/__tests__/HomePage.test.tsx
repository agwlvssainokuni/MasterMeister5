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
import { MemoryRouter } from "react-router-dom";
import { HomePage } from "../HomePage";
import { AuthProvider } from "../../auth/AuthContext";
import "../../i18n/i18n";

describe("HomePage", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("shows only the general cards for a GENERAL user", async () => {
    const user = { id: 1, email: "user@example.com", name: "Taro", role: "GENERAL" };
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => ({ accessToken: "tok", user }) }) as unknown as typeof fetch;

    render(
      <MemoryRouter>
        <AuthProvider>
          <HomePage />
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByTestId("home-card-masterData")).toBeInTheDocument());
    expect(screen.getByTestId("home-card-query")).toBeInTheDocument();
    expect(screen.getByTestId("home-card-queryHistory")).toBeInTheDocument();
    expect(screen.queryByTestId("home-card-users")).not.toBeInTheDocument();
  });

  it("shows the admin-only cards for an ADMIN user", async () => {
    const user = { id: 1, email: "admin@example.com", name: "Admin", role: "ADMIN" };
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue({ ok: true, json: async () => ({ accessToken: "tok", user }) }) as unknown as typeof fetch;

    render(
      <MemoryRouter>
        <AuthProvider>
          <HomePage />
        </AuthProvider>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByTestId("home-card-users")).toBeInTheDocument());
    expect(screen.getByTestId("home-card-connections")).toBeInTheDocument();
    expect(screen.getByTestId("home-card-auditLog")).toBeInTheDocument();
  });
});
