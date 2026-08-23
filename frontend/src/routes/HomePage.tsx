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

import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Badge, Card } from "make-you-chic-ui";
import { useAuth } from "../auth/AuthContext";
import "./HomePage.css";

interface HomeCard {
  id: string;
  href: string;
  titleKey: string;
  descriptionKey: string;
  adminOnly?: boolean;
}

const HOME_CARDS: HomeCard[] = [
  { id: "masterData", href: "/data", titleKey: "nav.masterData", descriptionKey: "home.cards.masterData" },
  { id: "query", href: "/queries", titleKey: "nav.query", descriptionKey: "home.cards.query" },
  {
    id: "queryHistory",
    href: "/queries/history",
    titleKey: "nav.queryHistory",
    descriptionKey: "home.cards.queryHistory",
  },
  {
    id: "connections",
    href: "/connections",
    titleKey: "nav.connections",
    descriptionKey: "home.cards.connections",
    adminOnly: true,
  },
  {
    id: "permissions",
    href: "/permissions",
    titleKey: "nav.permissions",
    descriptionKey: "home.cards.permissions",
    adminOnly: true,
  },
  {
    id: "customizations",
    href: "/data/customization",
    titleKey: "nav.customizations",
    descriptionKey: "home.cards.customizations",
    adminOnly: true,
  },
  {
    id: "users",
    href: "/users",
    titleKey: "nav.users",
    descriptionKey: "home.cards.users",
    adminOnly: true,
  },
  {
    id: "groups",
    href: "/groups",
    titleKey: "nav.groups",
    descriptionKey: "home.cards.groups",
    adminOnly: true,
  },
  {
    id: "auditLog",
    href: "/audit-log",
    titleKey: "nav.auditLog",
    descriptionKey: "home.cards.auditLog",
    adminOnly: true,
  },
];

/** frontend-components.md HomePage — a card grid linking to every screen the current user can access. */
export function HomePage(): React.JSX.Element {
  const { t } = useTranslation();
  const { user } = useAuth();

  const cards = HOME_CARDS.filter((card) => !card.adminOnly || user?.role === "ADMIN");

  return (
    <div data-testid="home-page">
      <h1>{t("nav.home")}</h1>
      <div className="mm5-home-cards">
        {cards.map((card) => (
          <Link
            key={card.id}
            to={card.href}
            className="mm5-home-card-link"
            data-testid={`home-card-${card.id}`}
          >
            <Card>
              <h2 className="mm5-home-card-title">
                {t(card.titleKey)}
                {card.adminOnly && <Badge variant="secondary">{t("home.adminBadge")}</Badge>}
              </h2>
              <p className="mm5-home-card-description">{t(card.descriptionKey)}</p>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
