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

import { Outlet } from "react-router-dom";
import { AppShell, type AppShellNavItem } from "make-you-chic-ui";
import { useTranslation } from "react-i18next";

/**
 * Common layout for every screen except the (future, Unit 2) login screen —
 * integration-guide.md's "layout route" pattern via react-router's
 * path-less <Route element={<AppLayout />}>. navItems will grow as later
 * units add screens.
 */
export function AppLayout(): React.JSX.Element {
  const { t } = useTranslation();

  const navItems: AppShellNavItem[] = [{ label: t("nav.home"), href: "/" }];

  return (
    <AppShell navItems={navItems}>
      <Outlet />
    </AppShell>
  );
}
