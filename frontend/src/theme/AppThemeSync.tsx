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

import { useEffect, type ReactNode } from "react";
import { useTheme } from "make-you-chic-ui";
import { fetchAppTheme, toThemeBrand, toThemeFontFamily } from "../api/theme";

export interface AppThemeSyncProps {
  children: ReactNode;
}

/**
 * requirements.md: brand color / font are admin-managed and shared by all
 * users (unlike theme mode / font size, which stay per-user localStorage
 * settings owned entirely by make-you-chic-ui's ThemeProvider). On mount,
 * this overwrites the ThemeProvider's brand/fontFamily with the
 * backend-authoritative value, so a stale localStorage value never wins
 * over what an admin has configured.
 *
 * Must be rendered inside <ThemeProvider>.
 */
export function AppThemeSync({ children }: AppThemeSyncProps): React.JSX.Element {
  const { setBrand, setFontFamily } = useTheme();

  useEffect(() => {
    let cancelled = false;
    fetchAppTheme()
      .then((dto) => {
        if (cancelled) return;
        setBrand(toThemeBrand(dto));
        setFontFamily(toThemeFontFamily(dto));
      })
      .catch(() => {
        // Unit 1: no public endpoints yet, so this 401s until Unit 2 adds
        // login. Falling back to the ThemeProvider's local default is
        // acceptable — it is not a user-facing error.
      });
    return () => {
      cancelled = true;
    };
  }, [setBrand, setFontFamily]);

  return <>{children}</>;
}
