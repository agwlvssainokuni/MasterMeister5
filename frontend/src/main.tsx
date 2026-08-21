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

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { ThemeProvider, ToastProvider, ModalStackProvider } from "make-you-chic-ui";
import "make-you-chic-ui/style.css";
import "@fontsource/noto-sans-jp/japanese-400.css";
import "@fontsource/noto-sans-jp/japanese-500.css";
import "@fontsource/noto-sans-jp/japanese-600.css";
import "@fontsource/noto-sans-jp/japanese-700.css";
import "@fontsource/noto-serif-jp/japanese-400.css";
import "@fontsource/noto-serif-jp/japanese-500.css";
import "@fontsource/noto-serif-jp/japanese-600.css";
import "@fontsource/noto-serif-jp/japanese-700.css";
import "./i18n/i18n";
import { AppThemeSync } from "./theme/AppThemeSync";
import { AuthProvider } from "./auth/AuthContext";
import { App } from "./App";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <AppThemeSync>
        <ToastProvider>
          <ModalStackProvider>
            <BrowserRouter>
              <AuthProvider>
                <App />
              </AuthProvider>
            </BrowserRouter>
          </ModalStackProvider>
        </ToastProvider>
      </AppThemeSync>
    </ThemeProvider>
  </StrictMode>,
);
