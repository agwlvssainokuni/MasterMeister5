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

import net from "node:net";
import { request as playwrightRequest } from "@playwright/test";

/**
 * E2E tests exercise real cross-Unit flows (invitation email via MailPit,
 * target RDBMS connection/schema import against devenv's PostgreSQL), so
 * they depend on devenv being up. Failing fast here with an actionable
 * message is much cheaper than debugging a mysterious test timeout deep
 * inside the main journey spec.
 */
function checkTcpPort(host: string, port: number, timeoutMs = 3000): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    const finish = (result: boolean) => {
      socket.destroy();
      resolve(result);
    };
    socket.setTimeout(timeoutMs);
    socket.once("connect", () => finish(true));
    socket.once("timeout", () => finish(false));
    socket.once("error", () => finish(false));
    socket.connect(port, host);
  });
}

export default async function globalSetup(): Promise<void> {
  const postgresUp = await checkTcpPort("localhost", 5432);
  if (!postgresUp) {
    throw new Error(
      "devenvのPostgreSQL(localhost:5432)に接続できません。" +
        "`cd devenv && docker compose --profile postgres up -d` を実行してから再度実行してください。",
    );
  }

  const mailpitContext = await playwrightRequest.newContext();
  try {
    const response = await mailpitContext.get("http://localhost:8025/api/v1/messages").catch(() => null);
    if (!response || !response.ok()) {
      throw new Error(
        "devenvのMailPit(http://localhost:8025)に接続できません。" +
          "`cd devenv && docker compose up -d` を実行してから再度実行してください。",
      );
    }
  } finally {
    await mailpitContext.dispose();
  }
}
