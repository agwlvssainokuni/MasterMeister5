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

import { Route, Routes } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";
import { HomePage } from "./routes/HomePage";
import { LoginScreen } from "./routes/LoginScreen";
import { RegistrationCompletionScreen } from "./routes/RegistrationCompletionScreen";
import { ForgotPasswordScreen } from "./routes/ForgotPasswordScreen";
import { ResetPasswordScreen } from "./routes/ResetPasswordScreen";
import { ChangePasswordScreen } from "./routes/ChangePasswordScreen";
import { AdminUserListScreen } from "./routes/admin/AdminUserListScreen";
import { ConnectionListScreen } from "./routes/admin/ConnectionListScreen";
import { GroupManagementScreen } from "./routes/admin/GroupManagementScreen";
import { PermissionScreen } from "./routes/admin/PermissionScreen";
import { MasterDataScreen } from "./routes/admin/MasterDataScreen";
import { CustomizationScreen } from "./routes/admin/CustomizationScreen";
import { RequireAuth } from "./auth/RequireAuth";

/**
 * frontend-components.md: authentication-free screens render outside
 * <AppLayout> (layout-route pattern, integration-guide.md); every other
 * screen requires a logged-in user via <RequireAuth>, and "/users" further
 * requires the ADMIN role.
 */
export function App(): React.JSX.Element {
  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route path="/register/:token" element={<RegistrationCompletionScreen />} />
      <Route path="/password/forgot" element={<ForgotPasswordScreen />} />
      <Route path="/password/reset/:token" element={<ResetPasswordScreen />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/settings/password" element={<ChangePasswordScreen />} />
          <Route path="/data" element={<MasterDataScreen />} />
          <Route element={<RequireAuth role="ADMIN" />}>
            <Route path="/users" element={<AdminUserListScreen />} />
            <Route path="/connections" element={<ConnectionListScreen />} />
            <Route path="/groups" element={<GroupManagementScreen />} />
            <Route path="/permissions" element={<PermissionScreen />} />
            <Route path="/data/customization" element={<CustomizationScreen />} />
          </Route>
        </Route>
      </Route>
    </Routes>
  );
}
