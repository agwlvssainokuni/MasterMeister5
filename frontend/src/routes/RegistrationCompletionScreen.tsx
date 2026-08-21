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
import { ApiError, register } from "../api/auth";

/** frontend-components.md RegistrationCompletionScreen (US-1.6). */
export function RegistrationCompletionScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const { token } = useParams<{ token: string }>();

  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [tokenExpired, setTokenExpired] = useState(false);
  const [completed, setCompleted] = useState(false);

  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token || passwordMismatch) return;
    setSubmitting(true);
    setErrorMessage(null);
    setTokenExpired(false);
    try {
      await register(token, name, password);
      setCompleted(true);
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === "INVITATION_TOKEN_EXPIRED") {
        setTokenExpired(true);
      } else {
        setErrorMessage(err instanceof ApiError ? err.message : t("auth.register.error"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (completed) {
    return (
      <div className="mm5-auth-screen">
        <h1>{t("auth.register.completedTitle")}</h1>
        <p>{t("auth.register.completedMessage")}</p>
        <Link to="/login" data-testid="registration-form-login-link">
          {t("auth.login.title")}
        </Link>
      </div>
    );
  }

  return (
    <div className="mm5-auth-screen">
      <h1>{t("auth.register.title")}</h1>
      {tokenExpired ? (
        <p role="alert">{t("errors.invitation_token_expired")}</p>
      ) : (
        <form onSubmit={handleSubmit} data-testid="registration-form">
          <FormField label={t("auth.name")} required>
            <TextInput
              value={name}
              onChange={setName}
              required
              data-testid="registration-form-name-input"
            />
          </FormField>
          <FormField label={t("auth.password")} required>
            <TextInput
              type="password"
              value={password}
              onChange={setPassword}
              required
              minLength={8}
              data-testid="registration-form-password-input"
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
              data-testid="registration-form-password-confirm-input"
            />
          </FormField>
          {errorMessage && <p role="alert">{errorMessage}</p>}
          <Button
            type="submit"
            loading={submitting}
            disabled={passwordMismatch}
            data-testid="registration-form-submit-button"
          >
            {t("auth.register.submit")}
          </Button>
        </form>
      )}
    </div>
  );
}
