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
import { ResetPasswordScreen } from "../ResetPasswordScreen";
import "../../i18n/i18n";

function renderScreen() {
  return render(
    <MemoryRouter initialEntries={["/password/reset/tok-123"]}>
      <Routes>
        <Route path="/password/reset/:token" element={<ResetPasswordScreen />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ResetPasswordScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("shows a completion link to /login on success", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true }) as unknown as typeof fetch;

    renderScreen();
    await userEvent.type(screen.getByTestId("reset-password-form-password-input"), "correctHorseBattery1");
    await userEvent.type(
      screen.getByTestId("reset-password-form-password-confirm-input"),
      "correctHorseBattery1",
    );
    await userEvent.click(screen.getByTestId("reset-password-form-submit-button"));

    await waitFor(() => expect(screen.getByTestId("reset-password-form-login-link")).toBeInTheDocument());
  });

  it("disables submission while the confirmation does not match", async () => {
    renderScreen();

    await userEvent.type(screen.getByTestId("reset-password-form-password-input"), "correctHorseBattery1");
    await userEvent.type(screen.getByTestId("reset-password-form-password-confirm-input"), "different");

    expect(screen.getByTestId("reset-password-form-submit-button")).toBeDisabled();
  });
});
