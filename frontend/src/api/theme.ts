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

import type { ThemeBrand, ThemeFontFamily } from "make-you-chic-ui";

export interface AppThemeDto {
  brandColor: "BLUE" | "GREEN" | "PURPLE" | "ORANGE";
  fontFamily: "SANS" | "SERIF";
}

const BRAND_COLOR_TO_THEME: Record<AppThemeDto["brandColor"], ThemeBrand> = {
  BLUE: "blue",
  GREEN: "green",
  PURPLE: "purple",
  ORANGE: "orange",
};

const FONT_FAMILY_TO_THEME: Record<AppThemeDto["fontFamily"], ThemeFontFamily> = {
  SANS: "sans",
  SERIF: "serif",
};

export function toThemeBrand(dto: AppThemeDto): ThemeBrand {
  return BRAND_COLOR_TO_THEME[dto.brandColor];
}

export function toThemeFontFamily(dto: AppThemeDto): ThemeFontFamily {
  return FONT_FAMILY_TO_THEME[dto.fontFamily];
}

/**
 * Unit 1: every request requires authentication (backend SecurityConfig has
 * no public endpoints yet), so this call fails with 401 until Unit 2 wires
 * up login. Callers should treat rejection as "keep the local default" —
 * see AppThemeSync.
 */
export async function fetchAppTheme(): Promise<AppThemeDto> {
  const response = await fetch("/api/theme");
  if (!response.ok) {
    throw new Error(`Failed to fetch app theme: ${response.status}`);
  }
  return (await response.json()) as AppThemeDto;
}

export async function updateAppTheme(theme: AppThemeDto): Promise<AppThemeDto> {
  const response = await fetch("/api/theme", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(theme),
  });
  if (!response.ok) {
    throw new Error(`Failed to update app theme: ${response.status}`);
  }
  return (await response.json()) as AppThemeDto;
}
