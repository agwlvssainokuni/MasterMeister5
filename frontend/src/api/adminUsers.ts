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

import { authenticatedJson, authenticatedVoid, type UserRole } from "./auth";

export type UserStatus = "INVITED" | "ACTIVE" | "DEACTIVATED";

export interface UserSummaryDto {
  id: number;
  email: string;
  name: string | null;
  role: UserRole;
  status: UserStatus;
  invitedAt: string | null;
  registeredAt: string | null;
}

export function listUsers(): Promise<UserSummaryDto[]> {
  return authenticatedJson<UserSummaryDto[]>("/api/admin/users");
}

export function inviteUser(email: string, role: UserRole): Promise<void> {
  return authenticatedVoid("/api/admin/users/invitations", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, role }),
  });
}

export function resendInvitation(userId: number): Promise<void> {
  return authenticatedVoid(`/api/admin/users/${userId}/invitations/resend`, { method: "POST" });
}

export function changeRole(userId: number, role: UserRole): Promise<void> {
  return authenticatedVoid(`/api/admin/users/${userId}/role`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role }),
  });
}

export function deactivateUser(userId: number): Promise<void> {
  return authenticatedVoid(`/api/admin/users/${userId}/deactivate`, { method: "POST" });
}

export function reactivateUser(userId: number): Promise<void> {
  return authenticatedVoid(`/api/admin/users/${userId}/reactivate`, { method: "POST" });
}
