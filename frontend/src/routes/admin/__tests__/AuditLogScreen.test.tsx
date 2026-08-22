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
import { AuditLogScreen } from "../AuditLogScreen";
import "../../../i18n/i18n";

const EVENT_PAGE = {
  content: [
    {
      id: 1,
      eventType: "LOGIN_SUCCEEDED",
      actorUserId: 1,
      targetUserId: 1,
      details: { ip: "127.0.0.1" },
      correlationId: "corr-1",
      occurredAt: "2026-01-01T00:00:00Z",
    },
  ],
  page: 0,
  size: 50,
  totalElements: 1,
};

function installFetch(routes: Array<{ url: string; respond: () => Promise<unknown> }>) {
  globalThis.fetch = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    for (const route of routes) {
      if (url.includes(route.url)) {
        return route.respond();
      }
    }
    throw new Error(`unexpected fetch: ${url}`);
  }) as unknown as typeof fetch;
}

describe("AuditLogScreen", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it("renders audit event rows", async () => {
    installFetch([{ url: "/api/admin/audit-events", respond: async () => ({ ok: true, json: async () => EVENT_PAGE }) }]);

    render(<AuditLogScreen />);

    await waitFor(() => expect(screen.getByText("LOGIN_SUCCEEDED")).toBeInTheDocument());
  });

  it("shows the details JSON when the details button is clicked", async () => {
    installFetch([{ url: "/api/admin/audit-events", respond: async () => ({ ok: true, json: async () => EVENT_PAGE }) }]);

    render(<AuditLogScreen />);

    await waitFor(() => expect(screen.getByTestId("audit-log-row-1-details-button")).toBeInTheDocument());
    await userEvent.click(screen.getByTestId("audit-log-row-1-details-button"));

    await waitFor(() => expect(screen.getByTestId("audit-log-details-json")).toHaveTextContent("127.0.0.1"));
  });
});
