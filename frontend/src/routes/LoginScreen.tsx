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
import { Link, useNavigate } from "react-router-dom";
import { Button, FormField, TextInput } from "make-you-chic-ui";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/auth";

/** frontend-components.md LoginScreen. Rendered outside <AppLayout> (layout-route pattern). */
export function LoginScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await login(email, password);
      navigate("/", { replace: true });
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : t("auth.login.error"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mm5-auth-screen">
      <h1>{t("auth.login.title")}</h1>
      <form onSubmit={handleSubmit} data-testid="login-form">
        <FormField label={t("auth.email")} required>
          <TextInput
            type="email"
            value={email}
            onChange={setEmail}
            required
            data-testid="login-form-email-input"
          />
        </FormField>
        <FormField label={t("auth.password")} required>
          <TextInput
            type="password"
            value={password}
            onChange={setPassword}
            required
            data-testid="login-form-password-input"
          />
        </FormField>
        {errorMessage && <p role="alert">{errorMessage}</p>}
        <Button type="submit" loading={submitting} data-testid="login-form-submit-button">
          {t("auth.login.submit")}
        </Button>
      </form>
      <Link to="/password/forgot" data-testid="login-form-forgot-password-link">
        {t("auth.login.forgotPassword")}
      </Link>
    </div>
  );
}
