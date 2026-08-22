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
  addMember,
  createGroup,
  deleteGroup,
  listGroups,
  listMembers,
  removeMember,
  renameGroup,
  type GroupMemberDto,
  type GroupSummaryDto,
} from "../../api/groups";
import { listUsers, type UserSummaryDto } from "../../api/adminUsers";
import { ApiError } from "../../api/auth";

/** frontend-components.md GroupManagementScreen (US-2.7). */
export function GroupManagementScreen(): React.JSX.Element {
  const { t } = useTranslation();

  const [groups, setGroups] = useState<GroupSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState<UserSummaryDto[]>([]);

  const [createOpen, setCreateOpen] = useState(false);
  const [renameTarget, setRenameTarget] = useState<GroupSummaryDto | null>(null);
  const [groupName, setGroupName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
  const [members, setMembers] = useState<GroupMemberDto[]>([]);
  const [addUserId, setAddUserId] = useState("");

  const reload = useCallback(() => {
    setLoading(true);
    listGroups()
      .then(setGroups)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    reload();
    listUsers().then(setUsers);
  }, [reload]);

  const reloadMembers = useCallback((groupId: number) => {
    listMembers(groupId).then(setMembers);
  }, []);

  async function handleCreateSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await createGroup(groupName);
      setCreateOpen(false);
      setGroupName("");
      reload();
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : t("admin.groups.create.error"));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRenameSubmit(e: FormEvent) {
    e.preventDefault();
    if (!renameTarget) {
      return;
    }
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await renameGroup(renameTarget.id, groupName);
      setRenameTarget(null);
      setGroupName("");
      reload();
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : t("admin.groups.rename.error"));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(groupId: number) {
    await deleteGroup(groupId);
    if (selectedGroupId === groupId) {
      setSelectedGroupId(null);
      setMembers([]);
    }
    reload();
  }

  function handleSelect(groupId: number) {
    setSelectedGroupId(groupId);
    reloadMembers(groupId);
  }

  async function handleAddMember() {
    if (!selectedGroupId || !addUserId) {
      return;
    }
    await addMember(selectedGroupId, Number(addUserId));
    setAddUserId("");
    reloadMembers(selectedGroupId);
    reload();
  }

  async function handleRemoveMember(userId: number) {
    if (!selectedGroupId) {
      return;
    }
    await removeMember(selectedGroupId, userId);
    reloadMembers(selectedGroupId);
    reload();
  }

  const userOptions = users.map((u) => ({ label: `${u.name ?? ""} <${u.email}>`, value: String(u.id) }));

  return (
    <div>
      <h1>{t("admin.groups.title")}</h1>
      <Button data-testid="groups-create-button" onClick={() => setCreateOpen(true)}>
        {t("admin.groups.createButton")}
      </Button>

      {loading ? (
        <p>{t("common.loading")}</p>
      ) : (
        <table data-testid="groups-table">
          <thead>
            <tr>
              <th>{t("admin.groups.columns.name")}</th>
              <th>{t("admin.groups.columns.memberCount")}</th>
              <th>{t("admin.groups.columns.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {groups.map((group) => (
              <tr key={group.id} data-testid={`groups-row-${group.id}`}>
                <td>{group.name}</td>
                <td>{group.memberCount}</td>
                <td>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => handleSelect(group.id)}
                    data-testid={`groups-row-${group.id}-select-button`}
                  >
                    {t("admin.groups.selectButton")}
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setRenameTarget(group);
                      setGroupName(group.name);
                    }}
                    data-testid={`groups-row-${group.id}-rename-button`}
                  >
                    {t("admin.groups.renameButton")}
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => handleDelete(group.id)}
                    data-testid={`groups-row-${group.id}-delete-button`}
                  >
                    {t("admin.groups.deleteButton")}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedGroupId !== null && (
        <div data-testid="groups-members-panel">
          <h2>{t("admin.groups.members.title")}</h2>
          <ul>
            {members.map((member) => (
              <li key={member.userId} data-testid={`groups-member-${member.userId}`}>
                {member.name} &lt;{member.email}&gt;
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleRemoveMember(member.userId)}
                  data-testid={`groups-member-${member.userId}-remove-button`}
                >
                  {t("admin.groups.members.removeButton")}
                </Button>
              </li>
            ))}
          </ul>
          <Select
            options={userOptions}
            value={addUserId}
            onChange={setAddUserId}
            data-testid="groups-members-add-select"
          />
          <Button onClick={handleAddMember} data-testid="groups-members-add-button">
            {t("admin.groups.members.addButton")}
          </Button>
        </div>
      )}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={t("admin.groups.create.title")}>
        <form onSubmit={handleCreateSubmit} data-testid="groups-create-form">
          <FormField label={t("admin.groups.columns.name")} required>
            <TextInput value={groupName} onChange={setGroupName} required data-testid="groups-create-form-name-input" />
          </FormField>
          {errorMessage && <p role="alert">{errorMessage}</p>}
          <Button type="submit" loading={submitting} data-testid="groups-create-form-submit-button">
            {t("admin.groups.create.submit")}
          </Button>
        </form>
      </Modal>

      <Modal
        open={renameTarget !== null}
        onClose={() => setRenameTarget(null)}
        title={t("admin.groups.rename.title")}
      >
        <form onSubmit={handleRenameSubmit} data-testid="groups-rename-form">
          <FormField label={t("admin.groups.columns.name")} required>
            <TextInput value={groupName} onChange={setGroupName} required data-testid="groups-rename-form-name-input" />
          </FormField>
          {errorMessage && <p role="alert">{errorMessage}</p>}
          <Button type="submit" loading={submitting} data-testid="groups-rename-form-submit-button">
            {t("admin.groups.rename.submit")}
          </Button>
        </form>
      </Modal>
    </div>
  );
}
