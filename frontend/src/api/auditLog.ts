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

import { authenticatedJson } from "./auth";

export interface AuditEventDto {
  id: number;
  eventType: string;
  actorUserId: number | null;
  targetUserId: number | null;
  details: Record<string, unknown>;
  correlationId: string | null;
  occurredAt: string;
}

export interface AuditEventPageDto {
  content: AuditEventDto[];
  page: number;
  size: number;
  totalElements: number;
}

export interface AuditEventFilter {
  eventType?: string;
  actorUserId?: number;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export function listAuditEvents(filter: AuditEventFilter = {}): Promise<AuditEventPageDto> {
  const params = new URLSearchParams();
  if (filter.eventType) params.set("eventType", filter.eventType);
  if (filter.actorUserId !== undefined) params.set("actorUserId", String(filter.actorUserId));
  if (filter.fromDate) params.set("fromDate", filter.fromDate);
  if (filter.toDate) params.set("toDate", filter.toDate);
  params.set("page", String(filter.page ?? 0));
  params.set("size", String(filter.size ?? 50));
  return authenticatedJson<AuditEventPageDto>(`/api/admin/audit-events?${params.toString()}`);
}
