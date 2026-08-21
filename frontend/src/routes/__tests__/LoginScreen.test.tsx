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
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "../../auth/AuthContext";
import { LoginScreen } from "../LoginScreen";
import "../../i18n/i18n";

function renderScreen() {
  return render(
    <MemoryRouter initialEntries={["/login"]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginScreen />} />
          <Route path="/" element={<div>home screen</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe("LoginScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("navigates to / on a successful login", async () => {
    const user = { id: 1, email: "user@example.com", name: "Taro", role: "GENERAL" };
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/auth/refresh")) return Promise.resolve({ ok: false, status: 401 });
      if (url.includes("/api/auth/login")) {
        return Promise.resolve({ ok: true, json: async () => ({ accessToken: "tok", user }) });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    renderScreen();
    await waitFor(() => expect(screen.getByTestId("login-form-submit-button")).toBeInTheDocument());

    await userEvent.type(screen.getByTestId("login-form-email-input"), "user@example.com");
    await userEvent.type(screen.getByTestId("login-form-password-input"), "correctHorseBattery1");
    await userEvent.click(screen.getByTestId("login-form-submit-button"));

    await waitFor(() => expect(screen.getByText("home screen")).toBeInTheDocument());
  });

  it("shows the server error message on a failed login", async () => {
    globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/api/auth/refresh")) return Promise.resolve({ ok: false, status: 401 });
      if (url.includes("/api/auth/login")) {
        return Promise.resolve({
          ok: false,
          status: 401,
          json: async () => ({ errorCode: "AUTHENTICATION_FAILED", message: "invalid" }),
        });
      }
      throw new Error(`unexpected fetch: ${url}`);
    }) as unknown as typeof fetch;

    renderScreen();
    await waitFor(() => expect(screen.getByTestId("login-form-submit-button")).toBeInTheDocument());

    await userEvent.type(screen.getByTestId("login-form-email-input"), "user@example.com");
    await userEvent.type(screen.getByTestId("login-form-password-input"), "wrong");
    await userEvent.click(screen.getByTestId("login-form-submit-button"));

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("invalid"));
  });
});
