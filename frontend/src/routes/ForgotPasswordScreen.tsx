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
import { Button, Card, FormField, TextInput } from "make-you-chic-ui";
import { requestPasswordReset } from "../api/auth";
import "./AuthScreen.css";

/**
 * frontend-components.md ForgotPasswordScreen. BR-23: the completion message
 * is shown regardless of whether the email is registered — this screen never
 * inspects the outcome, only whether the request itself was sent.
 */
export function ForgotPasswordScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [email, setEmail] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await requestPasswordReset(email);
    } finally {
      setSubmitting(false);
      setSubmitted(true);
    }
  }

  if (submitted) {
    return (
      <div className="mm5-auth-screen">
        <Card className="mm5-auth-screen-card">
          <h1>{t("auth.forgotPassword.title")}</h1>
          <p>{t("auth.forgotPassword.completedMessage")}</p>
        </Card>
      </div>
    );
  }

  return (
    <div className="mm5-auth-screen">
      <Card className="mm5-auth-screen-card">
        <h1>{t("auth.forgotPassword.title")}</h1>
        <form onSubmit={handleSubmit} data-testid="forgot-password-form">
          <FormField label={t("auth.email")} required>
            <TextInput
              type="email"
              value={email}
              onChange={setEmail}
              required
              data-testid="forgot-password-form-email-input"
            />
          </FormField>
          <Button type="submit" loading={submitting} data-testid="forgot-password-form-submit-button">
            {t("auth.forgotPassword.submit")}
          </Button>
        </form>
      </Card>
    </div>
  );
}
