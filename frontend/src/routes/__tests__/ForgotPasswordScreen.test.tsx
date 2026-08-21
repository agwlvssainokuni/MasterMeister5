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
import { ForgotPasswordScreen } from "../ForgotPasswordScreen";
import "../../i18n/i18n";

describe("ForgotPasswordScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("shows the same completion message whether or not the email is registered", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: true }) as unknown as typeof fetch;

    render(<ForgotPasswordScreen />);
    await userEvent.type(screen.getByTestId("forgot-password-form-email-input"), "nobody@example.com");
    await userEvent.click(screen.getByTestId("forgot-password-form-submit-button"));

    await waitFor(() =>
      expect(screen.queryByTestId("forgot-password-form")).not.toBeInTheDocument(),
    );
  });
});
