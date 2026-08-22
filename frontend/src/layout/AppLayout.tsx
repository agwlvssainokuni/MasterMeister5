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

import { useNavigate, Outlet } from "react-router-dom";
import { AppShell, type AppShellNavItem } from "make-you-chic-ui";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";

/**
 * Common layout for every authenticated screen — integration-guide.md's
 * "layout route" pattern via react-router's path-less
 * <Route element={<AppLayout />}>. navItems will grow as later units add
 * screens; "ユーザー管理" is ADMIN-only (frontend-components.md).
 */
export function AppLayout(): React.JSX.Element {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const navItems: AppShellNavItem[] = [
    { label: t("nav.home"), href: "/" },
    { label: t("nav.masterData"), href: "/data" },
    { label: t("nav.query"), href: "/queries" },
    { label: t("nav.queryHistory"), href: "/queries/history" },
  ];
  if (user?.role === "ADMIN") {
    navItems.push({ label: t("nav.users"), href: "/users" });
    navItems.push({ label: t("nav.connections"), href: "/connections" });
    navItems.push({ label: t("nav.groups"), href: "/groups" });
    navItems.push({ label: t("nav.permissions"), href: "/permissions" });
    navItems.push({ label: t("nav.customizations"), href: "/data/customization" });
    navItems.push({ label: t("nav.auditLog"), href: "/audit-log" });
  }

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <AppShell
      navItems={navItems}
      user={user ? { name: user.name } : undefined}
      userMenuItems={[
        { label: t("nav.changePassword"), onClick: () => navigate("/settings/password") },
        { label: t("nav.logout"), onClick: handleLogout },
      ]}
    >
      <Outlet />
    </AppShell>
  );
}
