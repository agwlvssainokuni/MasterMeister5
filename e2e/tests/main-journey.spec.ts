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

import { test, expect, type Page } from "@playwright/test";
import { findLinkInLatestMail } from "./support/mailpit";

/**
 * Automates integration-test-instructions.md's manual結合スモークテスト
 * scenarios 1〜10 end-to-end against the real packaged app, the real devenv
 * PostgreSQL (seeded by devenv/initdb/postgres/01-schema-and-data.sql), and
 * the real devenv MailPit. One long, ordered test — each step depends on
 * state the previous step created (invited user, registered connection,
 * imported schema, granted permission), so it is written as a single
 * `test()` with `test.step()` sections rather than independent tests.
 */

const ADMIN_EMAIL = "e2e-admin@example.com";
const ADMIN_PASSWORD = "E2eAdminPass123!";

const GENERAL_USER_EMAIL = "e2e-user@example.com";
const GENERAL_USER_NAME = "E2E User";
const GENERAL_USER_PASSWORD = "E2eUserPass123!";

const GROUP_NAME = "E2E Testers";
const CONNECTION_NAME = "E2E Postgres";

async function login(page: Page, email: string, password: string): Promise<void> {
  await page.goto("/login");
  await page.getByTestId("login-form-email-input").fill(email);
  await page.getByTestId("login-form-password-input").fill(password);
  await page.getByTestId("login-form-submit-button").click();
  await expect(page.getByTestId("app-shell")).toBeVisible();
}

async function logout(page: Page): Promise<void> {
  await page.getByTestId("dropdown-trigger").click();
  await page.getByRole("menuitem", { name: "ログアウト" }).click();
  await expect(page.getByTestId("login-form")).toBeVisible();
}

/** Selects an option in a native <select> by matching a substring of its visible text. */
async function selectOptionContaining(page: Page, testId: string, text: string): Promise<void> {
  const select = page.getByTestId(testId);
  const value = await select.locator("option", { hasText: text }).first().getAttribute("value");
  if (!value) {
    throw new Error(`No option containing "${text}" found in ${testId}`);
  }
  await select.selectOption(value);
}

test("full cross-Unit journey: invite, connect, permission, data, query, audit", async ({ page, request }) => {
  await test.step("1. 初期管理者でログイン（Unit 2）", async () => {
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await expect(page.getByText("ホーム")).toBeVisible();
  });

  await test.step("2. 一般ユーザを招待する（Unit 2）", async () => {
    await page.getByTestId("sidebar-nav-/users").click();
    await expect(page.getByTestId("admin-users-table")).toBeVisible();
    await page.getByTestId("admin-users-invite-button").click();
    await page.getByTestId("admin-users-invite-form-email-input").fill(GENERAL_USER_EMAIL);
    await page.getByTestId("admin-users-invite-form-role-select").selectOption("GENERAL");
    await page.getByTestId("admin-users-invite-form-submit-button").click();
    await expect(page.getByText(GENERAL_USER_EMAIL)).toBeVisible();
  });

  await test.step("3. グループを作成し、招待済みユーザを追加する（Unit 2）", async () => {
    await page.getByTestId("sidebar-nav-/groups").click();
    await page.getByTestId("groups-create-button").click();
    await page.getByTestId("groups-create-form-name-input").fill(GROUP_NAME);
    await page.getByTestId("groups-create-form-submit-button").click();
    await expect(page.getByText(GROUP_NAME)).toBeVisible();

    const groupRow = page.locator("tr", { hasText: GROUP_NAME });
    await groupRow.getByRole("button", { name: "選択" }).click();
    await expect(page.getByTestId("groups-members-panel")).toBeVisible();

    await selectOptionContaining(page, "groups-members-add-select", GENERAL_USER_EMAIL);
    await page.getByTestId("groups-members-add-button").click();
    // Scoped to <li> only: the still-present <option> inside the same panel's
    // add-member <select> also contains this email text (strict-mode violation
    // otherwise).
    await expect(
        page.getByTestId("groups-members-panel").locator("li", { hasText: GENERAL_USER_EMAIL }),
    ).toBeVisible();
  });

  await test.step("4. 対象RDBMS接続を登録する（Unit 3、devenvのPostgreSQL）", async () => {
    await page.getByTestId("sidebar-nav-/connections").click();
    await page.getByTestId("connections-register-button").click();
    await page.getByTestId("connections-register-form-name-input").fill(CONNECTION_NAME);
    await page.getByTestId("connections-register-form-rdbms-select").selectOption("POSTGRESQL");
    await page.getByTestId("connections-register-form-host-input").fill("localhost");
    await page.getByTestId("connections-register-form-port-input").fill("5432");
    await page.getByTestId("connections-register-form-database-input").fill("mastermeister5_target");
    await page.getByTestId("connections-register-form-username-input").fill("postgres");
    await page.getByTestId("connections-register-form-password-input").fill("mastermeister5");
    await page.getByTestId("connections-register-form-submit-button").click();
    await expect(page.getByText(CONNECTION_NAME)).toBeVisible();
  });

  await test.step("5. スキーマを取込む（Unit 3）", async () => {
    const connectionRow = page.locator("tr", { hasText: CONNECTION_NAME });
    await connectionRow.getByRole("button", { name: "スキーマ取込" }).click();
    await expect(page.getByTestId("connections-import-result")).toBeVisible();
    await page.getByTestId("connections-import-result-close-button").click();
  });

  await test.step("6. グループにpublicスキーマの読み取り権限を付与する（Unit 4）", async () => {
    await page.getByTestId("sidebar-nav-/permissions").click();
    await selectOptionContaining(page, "permissions-connection-select", CONNECTION_NAME);
    await page.getByRole("radio", { name: "グループ" }).check();
    await selectOptionContaining(page, "permissions-subject-select", GROUP_NAME);
    await expect(page.getByTestId("permissions-tree")).toBeVisible();
    await page.getByTestId("permissions-schema-public-primary-select").selectOption("READ");
  });

  await test.step("7. 招待メールをMailPitから取得し、一般ユーザが本登録する（Unit 2）", async () => {
    const registerLink = await findLinkInLatestMail(request, GENERAL_USER_EMAIL, "/register/");
    await page.goto(registerLink);
    await page.getByTestId("registration-form-name-input").fill(GENERAL_USER_NAME);
    await page.getByTestId("registration-form-password-input").fill(GENERAL_USER_PASSWORD);
    await page.getByTestId("registration-form-password-confirm-input").fill(GENERAL_USER_PASSWORD);
    await page.getByTestId("registration-form-submit-button").click();
    await expect(page.getByTestId("registration-form-login-link")).toBeVisible();
    await page.getByTestId("registration-form-login-link").click();
    await expect(page.getByTestId("login-form")).toBeVisible();
  });

  await test.step("8. 一般ユーザでログインし、権限付与された範囲のデータを閲覧する（Unit 4・5）", async () => {
    await login(page, GENERAL_USER_EMAIL, GENERAL_USER_PASSWORD);
    await page.getByTestId("sidebar-nav-/data").click();
    await selectOptionContaining(page, "master-data-connection-select", CONNECTION_NAME);
    await page.getByTestId("master-data-table-select").selectOption("public.customers");
    await page.getByTestId("master-data-raw-where-input").fill("id = 1");
    // exact: true — "Customer 1" is a substring of "Customer 10"/"Customer 11"
    // etc., so a non-exact match hits Playwright's strict-mode violation once
    // more than one row is in the (unfiltered client-side) result set.
    await expect(page.getByText("Customer 1", { exact: true })).toBeVisible();
  });

  let executedSqlText = "";
  await test.step("9. クエリ画面でSQLを実行する（Unit 6）", async () => {
    await page.getByTestId("sidebar-nav-/queries").click();
    await page.getByRole("tab", { name: "SQL直接入力" }).click();
    executedSqlText = "SELECT id, name FROM customers WHERE id = 1";
    await page.getByTestId("query-raw-sql-textarea").fill(executedSqlText);
    await selectOptionContaining(page, "query-connection-select", CONNECTION_NAME);
    await page.getByTestId("query-schema-select").selectOption("public");
    await page.getByTestId("query-execute-button").click();
    await expect(page.getByText("Customer 1", { exact: true })).toBeVisible();
  });

  await test.step("10. クエリ実行履歴に記録されていることを確認する（Unit 6）", async () => {
    await page.getByTestId("sidebar-nav-/queries/history").click();
    // Wait for the history screen itself (its history table loads
    // asynchronously) before searching by text — otherwise this can catch a
    // transient moment where QueryScreen (with the same SQL text in its own
    // textareas) hasn't fully unmounted yet, hitting a strict-mode violation
    // that Playwright throws immediately rather than retrying.
    await expect(page.getByTestId("query-history-connection-select")).toBeVisible();
    await expect(page.getByText(executedSqlText)).toBeVisible();
  });

  await test.step("11. 管理者で再ログインし、監査ログに一連の操作が記録されていることを確認する（Unit 2・6）", async () => {
    await logout(page);
    await login(page, ADMIN_EMAIL, ADMIN_PASSWORD);
    await page.getByTestId("sidebar-nav-/audit-log").click();
    // .first(): USER_REGISTERED always appears twice per run (the initial
    // admin bootstrap plus the invited general user's own registration) —
    // scoping to .first() avoids a strict-mode violation. Applied to the rest
    // too as a precaution against the same kind of duplication.
    await expect(page.getByText("USER_INVITED").first()).toBeVisible();
    await expect(page.getByText("USER_REGISTERED").first()).toBeVisible();
    await expect(page.getByText("CONNECTION_REGISTERED").first()).toBeVisible();
    await expect(page.getByText("SCHEMA_IMPORTED").first()).toBeVisible();
    await expect(page.getByText("PERMISSION_CHANGED").first()).toBeVisible();
    await expect(page.getByText("QUERY_EXECUTED").first()).toBeVisible();

    const queryExecutedRow = page.locator("tr", { hasText: "QUERY_EXECUTED" }).first();
    await queryExecutedRow.getByTestId(/audit-log-row-\d+-details-button/).click();
    await expect(page.getByTestId("audit-log-details-json")).toContainText("resultRowCount");
  });
});
