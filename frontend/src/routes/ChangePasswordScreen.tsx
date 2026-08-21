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

import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, TextInput } from "make-you-chic-ui";
import { ApiError, changePassword } from "../api/auth";

/** frontend-components.md ChangePasswordScreen (US-1.10). Rendered inside AppLayout. */
export function ChangePasswordScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const passwordMismatch = newPasswordConfirm.length > 0 && newPassword !== newPasswordConfirm;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (passwordMismatch) return;
    setSubmitting(true);
    setErrorMessage(null);
    setSuccessMessage(null);
    try {
      await changePassword(currentPassword, newPassword);
      setSuccessMessage(t("auth.changePassword.success"));
      setCurrentPassword("");
      setNewPassword("");
      setNewPasswordConfirm("");
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : t("auth.changePassword.error"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <h1>{t("auth.changePassword.title")}</h1>
      <form onSubmit={handleSubmit} data-testid="change-password-form">
        <FormField label={t("auth.currentPassword")} required>
          <TextInput
            type="password"
            value={currentPassword}
            onChange={setCurrentPassword}
            required
            data-testid="change-password-form-current-password-input"
          />
        </FormField>
        <FormField label={t("auth.newPassword")} required>
          <TextInput
            type="password"
            value={newPassword}
            onChange={setNewPassword}
            required
            minLength={8}
            data-testid="change-password-form-new-password-input"
          />
        </FormField>
        <FormField
          label={t("auth.passwordConfirm")}
          required
          error={passwordMismatch ? t("auth.passwordMismatch") : undefined}
        >
          <TextInput
            type="password"
            value={newPasswordConfirm}
            onChange={setNewPasswordConfirm}
            required
            data-testid="change-password-form-new-password-confirm-input"
          />
        </FormField>
        {errorMessage && <p role="alert">{errorMessage}</p>}
        {successMessage && <p>{successMessage}</p>}
        <Button
          type="submit"
          loading={submitting}
          disabled={passwordMismatch}
          data-testid="change-password-form-submit-button"
        >
          {t("auth.changePassword.submit")}
        </Button>
      </form>
    </div>
  );
}
