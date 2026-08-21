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

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Modal, Select, TextInput } from "make-you-chic-ui";
import {
  changeRole,
  deactivateUser,
  inviteUser,
  listUsers,
  reactivateUser,
  resendInvitation,
  type UserSummaryDto,
} from "../../api/adminUsers";
import type { UserRole } from "../../api/auth";
import { ApiError } from "../../api/auth";

/**
 * frontend-components.md AdminUserListScreen (US-1.1〜1.5). The user list is
 * small (invite-only accounts, no pagination requirement in Unit 2 scope),
 * so a plain table is used instead of make-you-chic-ui's Table component
 * (whose sort/select/inline-edit machinery would be unused overhead here).
 */
export function AdminUserListScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [users, setUsers] = useState<UserSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<UserRole>("GENERAL");
  const [inviteSubmitting, setInviteSubmitting] = useState(false);
  const [inviteErrorMessage, setInviteErrorMessage] = useState<string | null>(null);

  const reload = useCallback(() => {
    setLoading(true);
    listUsers()
      .then(setUsers)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleInviteSubmit(e: FormEvent) {
    e.preventDefault();
    setInviteSubmitting(true);
    setInviteErrorMessage(null);
    try {
      await inviteUser(inviteEmail, inviteRole);
      setInviteOpen(false);
      setInviteEmail("");
      setInviteRole("GENERAL");
      reload();
    } catch (err) {
      setInviteErrorMessage(err instanceof ApiError ? err.message : t("admin.users.invite.error"));
    } finally {
      setInviteSubmitting(false);
    }
  }

  async function handleResend(userId: number) {
    await resendInvitation(userId);
    reload();
  }

  async function handleChangeRole(userId: number, role: UserRole) {
    await changeRole(userId, role);
    reload();
  }

  async function handleDeactivate(userId: number) {
    await deactivateUser(userId);
    reload();
  }

  async function handleReactivate(userId: number) {
    await reactivateUser(userId);
    reload();
  }

  return (
    <div>
      <h1>{t("admin.users.title")}</h1>
      <Button data-testid="admin-users-invite-button" onClick={() => setInviteOpen(true)}>
        {t("admin.users.inviteButton")}
      </Button>

      {loading ? (
        <p>{t("common.loading")}</p>
      ) : (
        <table data-testid="admin-users-table">
          <thead>
            <tr>
              <th>{t("admin.users.columns.email")}</th>
              <th>{t("admin.users.columns.name")}</th>
              <th>{t("admin.users.columns.role")}</th>
              <th>{t("admin.users.columns.status")}</th>
              <th>{t("admin.users.columns.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} data-testid={`admin-users-row-${user.id}`}>
                <td>{user.email}</td>
                <td>{user.name ?? ""}</td>
                <td>
                  <Select
                    aria-label={t("admin.users.columns.role")}
                    options={[
                      { label: t("admin.users.role.admin"), value: "ADMIN" },
                      { label: t("admin.users.role.general"), value: "GENERAL" },
                    ]}
                    value={user.role}
                    onChange={(value) => handleChangeRole(user.id, value as UserRole)}
                    data-testid={`admin-users-row-${user.id}-role-select`}
                  />
                </td>
                <td>{t(`admin.users.status.${user.status.toLowerCase()}`)}</td>
                <td>
                  {user.status === "INVITED" && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleResend(user.id)}
                      data-testid={`admin-users-row-${user.id}-resend-button`}
                    >
                      {t("admin.users.resendButton")}
                    </Button>
                  )}
                  {user.status === "ACTIVE" && (
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleDeactivate(user.id)}
                      data-testid={`admin-users-row-${user.id}-deactivate-button`}
                    >
                      {t("admin.users.deactivateButton")}
                    </Button>
                  )}
                  {user.status === "DEACTIVATED" && (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleReactivate(user.id)}
                      data-testid={`admin-users-row-${user.id}-reactivate-button`}
                    >
                      {t("admin.users.reactivateButton")}
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <Modal open={inviteOpen} onClose={() => setInviteOpen(false)} title={t("admin.users.invite.title")}>
        <form onSubmit={handleInviteSubmit} data-testid="admin-users-invite-form">
          <FormField label={t("auth.email")} required>
            <TextInput
              type="email"
              value={inviteEmail}
              onChange={setInviteEmail}
              required
              data-testid="admin-users-invite-form-email-input"
            />
          </FormField>
          <FormField label={t("admin.users.columns.role")} required>
            <Select
              options={[
                { label: t("admin.users.role.admin"), value: "ADMIN" },
                { label: t("admin.users.role.general"), value: "GENERAL" },
              ]}
              value={inviteRole}
              onChange={(value) => setInviteRole(value as UserRole)}
              data-testid="admin-users-invite-form-role-select"
            />
          </FormField>
          {inviteErrorMessage && <p role="alert">{inviteErrorMessage}</p>}
          <Button
            type="submit"
            loading={inviteSubmitting}
            data-testid="admin-users-invite-form-submit-button"
          >
            {t("admin.users.invite.submit")}
          </Button>
        </form>
      </Modal>
    </div>
  );
}
