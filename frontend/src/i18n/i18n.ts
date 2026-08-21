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

import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import ja from "./locales/ja/common.json";
import en from "./locales/en/common.json";

/**
 * requirements.md: UI language is persisted per-user in the backend (not
 * browser-detected) — see PlatformInfrastructureComponent#getUserLocale.
 * Unit 1 has no logged-in user yet, so Japanese is the default until Unit 2
 * wires the account's saved locale in on login.
 */
void i18n.use(initReactI18next).init({
  resources: {
    ja: { common: ja },
    en: { common: en },
  },
  lng: "ja",
  fallbackLng: "ja",
  defaultNS: "common",
  interpolation: { escapeValue: false },
});

export default i18n;
