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

export type UserRole = "ADMIN" | "GENERAL";

export interface AuthenticatedUserDto {
  id: number;
  email: string;
  name: string;
  role: UserRole;
}

export class ApiError extends Error {
  constructor(
    public readonly errorCode: string,
    message: string,
  ) {
    super(message);
  }
}

/**
 * nfr-design-plan.md Question 3: the access token lives only in memory (this
 * module-level variable), never in localStorage/sessionStorage. It is lost
 * on reload — {@link refresh} (backed by the HttpOnly refresh cookie)
 * restores it, see AuthContext.
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

function setAccessToken(token: string | null): void {
  accessToken = token;
}

async function parseErrorAndThrow(response: Response): Promise<never> {
  const body = await response.json().catch(() => null);
  throw new ApiError(
    body?.errorCode ?? "UNKNOWN",
    body?.message ?? `Request failed: ${response.status}`,
  );
}

async function handleJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    await parseErrorAndThrow(response);
  }
  return (await response.json()) as T;
}

async function handleVoid(response: Response): Promise<void> {
  if (!response.ok) {
    await parseErrorAndThrow(response);
  }
}

export interface LoginResult {
  accessToken: string;
  user: AuthenticatedUserDto;
}

export async function login(email: string, password: string): Promise<LoginResult> {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const result = await handleJson<LoginResult>(response);
  setAccessToken(result.accessToken);
  return result;
}

/** Silent refresh using the HttpOnly cookie. Returns null (never throws) on failure. */
export async function refresh(): Promise<LoginResult | null> {
  const response = await fetch("/api/auth/refresh", { method: "POST" });
  if (!response.ok) {
    setAccessToken(null);
    return null;
  }
  const result = (await response.json()) as LoginResult;
  setAccessToken(result.accessToken);
  return result;
}

export async function logout(): Promise<void> {
  await fetch("/api/auth/logout", { method: "POST" });
  setAccessToken(null);
}

export async function register(token: string, name: string, password: string): Promise<void> {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, name, password }),
  });
  await handleVoid(response);
}

export async function requestPasswordReset(email: string): Promise<void> {
  const response = await fetch("/api/auth/password/reset-request", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
  await handleVoid(response);
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  const response = await fetch("/api/auth/password/reset", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, newPassword }),
  });
  await handleVoid(response);
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  const response = await authenticatedFetch("/api/account/password", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
  await handleVoid(response);
}

/**
 * Attaches the in-memory access token and retries once via {@link refresh}
 * on a 401 (e.g. the 10-minute access token expired but the refresh cookie
 * is still valid). Other API modules (admin users, etc.) build on this.
 */
export async function authenticatedFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const withAuth = (token: string | null): RequestInit => ({
    ...init,
    headers: { ...(init.headers ?? {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });

  let response = await fetch(input, withAuth(accessToken));
  if (response.status === 401) {
    const refreshed = await refresh();
    if (refreshed) {
      response = await fetch(input, withAuth(refreshed.accessToken));
    }
  }
  return response;
}

export async function authenticatedJson<T>(input: string, init: RequestInit = {}): Promise<T> {
  return handleJson<T>(await authenticatedFetch(input, init));
}

export async function authenticatedVoid(input: string, init: RequestInit = {}): Promise<void> {
  await handleVoid(await authenticatedFetch(input, init));
}
