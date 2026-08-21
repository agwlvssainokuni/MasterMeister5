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
import { ChangePasswordScreen } from "../ChangePasswordScreen";
import "../../i18n/i18n";

describe("ChangePasswordScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("shows a success message after changing the password", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true }) as unknown as typeof fetch;

    render(<ChangePasswordScreen />);
    await userEvent.type(screen.getByTestId("change-password-form-current-password-input"), "old");
    await userEvent.type(
      screen.getByTestId("change-password-form-new-password-input"),
      "newCorrectHorse1",
    );
    await userEvent.type(
      screen.getByTestId("change-password-form-new-password-confirm-input"),
      "newCorrectHorse1",
    );
    await userEvent.click(screen.getByTestId("change-password-form-submit-button"));

    await waitFor(() => expect(screen.getByText(/変更しました/)).toBeInTheDocument());
  });

  it("shows the server error on a current-password mismatch", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ errorCode: "CURRENT_PASSWORD_MISMATCH", message: "mismatch" }),
    }) as unknown as typeof fetch;

    render(<ChangePasswordScreen />);
    await userEvent.type(screen.getByTestId("change-password-form-current-password-input"), "wrong");
    await userEvent.type(
      screen.getByTestId("change-password-form-new-password-input"),
      "newCorrectHorse1",
    );
    await userEvent.type(
      screen.getByTestId("change-password-form-new-password-confirm-input"),
      "newCorrectHorse1",
    );
    await userEvent.click(screen.getByTestId("change-password-form-submit-button"));

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("mismatch"));
  });
});
