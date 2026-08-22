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

import type { APIRequestContext } from "@playwright/test";

const MAILPIT_BASE_URL = "http://localhost:8025";

interface MailpitSearchMessage {
  ID: string;
}

interface MailpitSearchResult {
  messages: MailpitSearchMessage[];
}

interface MailpitMessage {
  Text?: string;
  HTML?: string;
}

/**
 * Polls MailPit's HTTP API (devenv/docker-compose.yml) for the most recent
 * message sent to `toEmail` and extracts the first `/register/<token>` (or
 * `/password/reset/<token>`) link found in its body. E2E tests never touch
 * a real mailbox — MailPit is devenv's SMTP catch-all.
 */
export async function findLinkInLatestMail(
  request: APIRequestContext,
  toEmail: string,
  linkPathPrefix: string,
  timeoutMs = 15_000,
): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  const pattern = new RegExp(`${linkPathPrefix}[A-Za-z0-9._-]+`);

  while (Date.now() < deadline) {
    const searchResponse = await request.get(`${MAILPIT_BASE_URL}/api/v1/search`, {
      params: { query: `to:${toEmail}` },
    });
    if (searchResponse.ok()) {
      const search = (await searchResponse.json()) as MailpitSearchResult;
      if (search.messages.length > 0) {
        const messageResponse = await request.get(`${MAILPIT_BASE_URL}/api/v1/message/${search.messages[0].ID}`);
        if (messageResponse.ok()) {
          const message = (await messageResponse.json()) as MailpitMessage;
          const body = `${message.Text ?? ""}\n${message.HTML ?? ""}`;
          const match = body.match(pattern);
          if (match) {
            return match[0];
          }
        }
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`Timed out waiting for a mail to ${toEmail} containing a "${linkPathPrefix}" link`);
}
