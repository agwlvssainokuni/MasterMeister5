# Deployment Architecture — Unit 1: デザインシステム基盤

## 開発環境

```mermaid
flowchart TD
    Dev["開発者"] --> Backend["backend (Spring Boot)<br/>./gradlew bootRun"]
    Dev --> Frontend["frontend (Vite dev server)<br/>npm run dev"]
    Backend --> H2["H2 Database<br/>ファイルベース"]
    Backend -.->|Composeプロファイル選択時| TargetDB["対象RDBMS<br/>MySQL/MariaDB/PostgreSQL<br/>(Docker Compose)"]
    Backend --> Mailpit["MailPit<br/>(Docker Compose)"]
    Frontend -->|プロキシ/API呼び出し| Backend

    style Dev fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Backend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style Frontend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2 fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style TargetDB fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style Mailpit fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
```

### テキスト代替表現
```
開発者 → backend (Spring Boot, bootRun) → H2 Database (ファイルベース)
開発者 → backend                        → 対象RDBMS (Docker Compose、プロファイル選択時)
開発者 → backend                        → MailPit (Docker Compose)
開発者 → frontend (Vite dev server)      → backend (API呼び出し)
```

## 本番相当（単一コンテナ）

```mermaid
flowchart TD
    User["利用者ブラウザ"] --> Container["Dockerコンテナ<br/>eclipse-temurin:25-jre<br/>実行可能WAR (backend+frontend静的資産)"]
    Container --> H2Vol["H2 Database<br/>ボリュームマウント（ファイルベース永続化）"]
    Container --> TargetDBProd["対象RDBMS<br/>(既存の運用環境、コンテナ外)"]
    Container --> SMTP["SMTPサーバ<br/>(環境変数で設定、コンテナ外)"]

    style User fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Container fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2Vol fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style TargetDBProd fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style SMTP fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
```

### テキスト代替表現
```
利用者ブラウザ → Dockerコンテナ (eclipse-temurin:25-jre、実行可能WAR)
Dockerコンテナ → H2 Database (ボリュームマウント、ファイルベース永続化)
Dockerコンテナ → 対象RDBMS (コンテナ外、環境変数で接続設定)
Dockerコンテナ → SMTPサーバ (コンテナ外、環境変数で接続設定)
```

**備考**: ロードバランサ・APIゲートウェイは単一インスタンス構成のため配置しない。将来的な
Tomcatへの実行可能WARデプロイにも対応するが、その場合もコンテナ構成と同様に単一インスタンス
かつ環境変数経由の設定を維持する。
