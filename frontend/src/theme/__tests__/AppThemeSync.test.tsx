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

import { afterEach, describe, expect, it, vi } from "vitest";
import { render, waitFor } from "@testing-library/react";
import { ThemeProvider } from "make-you-chic-ui";
import { AppThemeSync } from "../AppThemeSync";
import type { AppThemeDto } from "../../api/theme";

describe("AppThemeSync", () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
    document.documentElement.removeAttribute("data-brand");
    document.documentElement.removeAttribute("data-font-family");
  });

  it("applies the backend-provided brand/font onto <html> once fetched", async () => {
    const dto: AppThemeDto = { brandColor: "PURPLE", fontFamily: "SERIF" };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => dto,
    }) as unknown as typeof fetch;

    render(
      <ThemeProvider>
        <AppThemeSync>
          <div>content</div>
        </AppThemeSync>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(document.documentElement.getAttribute("data-brand")).toBe("purple");
      expect(document.documentElement.getAttribute("data-font-family")).toBe("serif");
    });
  });

  it("keeps the ThemeProvider default when the fetch fails (Unit 1: no public /api/theme yet)", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 401 }) as unknown as typeof fetch;

    render(
      <ThemeProvider>
        <AppThemeSync>
          <div>content</div>
        </AppThemeSync>
      </ThemeProvider>,
    );

    await waitFor(() => {
      expect(globalThis.fetch).toHaveBeenCalled();
    });
    // ThemeProvider's own default ('blue'), unchanged since AppThemeSync's
    // fetch rejected and never called setBrand.
    expect(document.documentElement.getAttribute("data-brand")).toBe("blue");
  });
});
