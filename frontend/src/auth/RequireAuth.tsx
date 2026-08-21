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

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { UserRole } from "../api/auth";

export interface RequireAuthProps {
  /** When set, only this role may pass; others are redirected to "/". */
  role?: UserRole;
}

/** frontend-components.md: unauthenticated access to a protected route redirects to /login. */
export function RequireAuth({ role }: RequireAuthProps): React.JSX.Element | null {
  const { user, initializing } = useAuth();

  if (initializing) {
    return null;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (role && user.role !== role) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
