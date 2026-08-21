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
import { Link, useParams } from "react-router-dom";
import { Button, FormField, TextInput } from "make-you-chic-ui";
import { ApiError, resetPassword } from "../api/auth";

/** frontend-components.md ResetPasswordScreen (US-1.9). */
export function ResetPasswordScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const { token } = useParams<{ token: string }>();

  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [completed, setCompleted] = useState(false);

  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token || passwordMismatch) return;
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await resetPassword(token, password);
      setCompleted(true);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : t("auth.resetPassword.error"));
    } finally {
      setSubmitting(false);
    }
  }

  if (completed) {
    return (
      <div className="mm5-auth-screen">
        <h1>{t("auth.resetPassword.completedTitle")}</h1>
        <Link to="/login" data-testid="reset-password-form-login-link">
          {t("auth.login.title")}
        </Link>
      </div>
    );
  }

  return (
    <div className="mm5-auth-screen">
      <h1>{t("auth.resetPassword.title")}</h1>
      <form onSubmit={handleSubmit} data-testid="reset-password-form">
        <FormField label={t("auth.newPassword")} required>
          <TextInput
            type="password"
            value={password}
            onChange={setPassword}
            required
            minLength={8}
            data-testid="reset-password-form-password-input"
          />
        </FormField>
        <FormField
          label={t("auth.passwordConfirm")}
          required
          error={passwordMismatch ? t("auth.passwordMismatch") : undefined}
        >
          <TextInput
            type="password"
            value={passwordConfirm}
            onChange={setPasswordConfirm}
            required
            data-testid="reset-password-form-password-confirm-input"
          />
        </FormField>
        {errorMessage && <p role="alert">{errorMessage}</p>}
        <Button
          type="submit"
          loading={submitting}
          disabled={passwordMismatch}
          data-testid="reset-password-form-submit-button"
        >
          {t("auth.resetPassword.submit")}
        </Button>
      </form>
    </div>
  );
}
