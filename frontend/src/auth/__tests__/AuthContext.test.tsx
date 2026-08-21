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
import { AuthProvider, useAuth } from "../AuthContext";

function Probe(): React.JSX.Element {
  const { user, initializing, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="state">
        {initializing ? "initializing" : user ? `logged-in:${user.email}` : "logged-out"}
      </span>
      <button onClick={() => login("user@example.com", "correctHorseBattery1")}>login</button>
      <button onClick={() => logout()}>logout</button>
    </div>
  );
}

describe("AuthContext", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("resolves to logged-out after the initial silent refresh fails", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 }) as unknown as typeof fetch;

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByTestId("state")).toHaveTextContent("logged-out"));
  });

  it("logs in and then logs out", async () => {
    const user = { id: 1, email: "user@example.com", name: "Taro", role: "GENERAL" };
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/auth/refresh")) {
        return Promise.resolve({ ok: false, status: 401 });
      }
      if (url.includes("/api/auth/login")) {
        return Promise.resolve({ ok: true, json: async () => ({ accessToken: "tok", user }) });
      }
      if (url.includes("/api/auth/logout")) {
        return Promise.resolve({ ok: true });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByTestId("state")).toHaveTextContent("logged-out"));

    await userEvent.click(screen.getByText("login"));
    await waitFor(() =>
      expect(screen.getByTestId("state")).toHaveTextContent("logged-in:user@example.com"),
    );

    await userEvent.click(screen.getByText("logout"));
    await waitFor(() => expect(screen.getByTestId("state")).toHaveTextContent("logged-out"));
  });
});
