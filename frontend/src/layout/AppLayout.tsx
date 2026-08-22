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

  // Sidebar renders navItems as plain <a href> (make-you-chic-ui's
  // AppShellNavItem.onClick exists precisely for this: "call
  // event.preventDefault() inside to take over navigation yourself"). Without
  // wiring it up here, every sidebar click was a full page reload — each one
  // re-running AuthProvider's mount-time silent refresh() via the httpOnly
  // cookie. Found via E2E testing: navigating through several admin screens
  // then logging in as a second user drove enough refresh() calls to trip
  // RateLimitFilter's budget for /api/auth/refresh (429), which the frontend
  // has no handling for, so RequireAuth just saw user=null and bounced to
  // /login. Client-side navigation avoids the reload (and its refresh call)
  // entirely for in-app link clicks.
  const goTo = (href: string) => (event: React.MouseEvent) => {
    event.preventDefault();
    navigate(href);
  };

  const navItems: AppShellNavItem[] = [
    { label: t("nav.home"), href: "/", onClick: goTo("/") },
    { label: t("nav.masterData"), href: "/data", onClick: goTo("/data") },
    { label: t("nav.query"), href: "/queries", onClick: goTo("/queries") },
    { label: t("nav.queryHistory"), href: "/queries/history", onClick: goTo("/queries/history") },
  ];
  if (user?.role === "ADMIN") {
    navItems.push({ label: t("nav.users"), href: "/users", onClick: goTo("/users") });
    navItems.push({ label: t("nav.connections"), href: "/connections", onClick: goTo("/connections") });
    navItems.push({ label: t("nav.groups"), href: "/groups", onClick: goTo("/groups") });
    navItems.push({ label: t("nav.permissions"), href: "/permissions", onClick: goTo("/permissions") });
    navItems.push({
      label: t("nav.customizations"),
      href: "/data/customization",
      onClick: goTo("/data/customization"),
    });
    navItems.push({ label: t("nav.auditLog"), href: "/audit-log", onClick: goTo("/audit-log") });
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
