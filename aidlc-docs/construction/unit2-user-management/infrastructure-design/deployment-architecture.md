# Deployment Architecture — Unit 2: ユーザ管理

Unit 1のdeployment-architecture.mdで確定済みの構成（開発環境・本番相当コンテナ）に、
Unit 2で新たに実際に使用され始める経路（MailPit経由のメール送信、リフレッシュトークン
Cookie）を反映する。コンテナ構成・ホスト構成自体に変更はない。

## 開発環境

```mermaid
flowchart TD
    Dev["開発者/ユーザーのブラウザ"] -->|"アクセストークンはメモリ保持<br/>リフレッシュトークンはSecure Cookie"| Backend["backend (Spring Boot)<br/>./gradlew bootRun"]
    Dev --> Frontend["frontend (Vite dev server)<br/>npm run dev"]
    Backend --> H2["H2 Database<br/>ファイルベース<br/>(user/refresh_token/password_reset_token/audit_event追加)"]
    Backend -->|"招待/リセットメール送信"| Mailpit["MailPit<br/>(Docker Compose)"]
    Frontend -->|プロキシ/API呼び出し| Backend

    style Dev fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Backend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style Frontend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2 fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style Mailpit fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
```

### テキスト代替表現
```
開発者/ユーザーのブラウザ → backend (アクセストークン: メモリ、リフレッシュトークン: Secure Cookie)
開発者 → frontend (Vite dev server) → backend (API呼び出し)
backend → H2 Database (ファイルベース、user/refresh_token/password_reset_token/audit_event)
backend → MailPit (招待メール・パスワードリセットメール送信)
```

## 本番相当（単一コンテナ）

```mermaid
flowchart TD
    User["利用者ブラウザ<br/>(アクセストークン: メモリ<br/>リフレッシュトークン: Secure Cookie)"] --> Container["Dockerコンテナ<br/>eclipse-temurin:25-jre<br/>実行可能WAR (backend+frontend静的資産)"]
    Container --> H2Vol["H2 Database<br/>ボリュームマウント（ファイルベース永続化）"]
    Container --> SMTP["SMTPサーバ<br/>(招待/リセットメール、環境変数で設定、コンテナ外)"]
    TLS["リバースプロキシ<br/>(TLS終端)"] -.->|"本番運用時の前提<br/>(Secure Cookie送受信のため)"| Container

    style User fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Container fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2Vol fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style SMTP fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style TLS fill:#FFE082,stroke:#FF8F00,stroke-width:2px,color:#000
```

### テキスト代替表現
```
利用者ブラウザ（アクセストークン: メモリ、リフレッシュトークン: Secure Cookie）
  → Dockerコンテナ (eclipse-temurin:25-jre、実行可能WAR)
Dockerコンテナ → H2 Database (ボリュームマウント、ファイルベース永続化)
Dockerコンテナ → SMTPサーバ (コンテナ外、環境変数で接続設定)
リバースプロキシ（TLS終端） → Dockerコンテナ （本番運用時の前提。Secure Cookieの送受信に必要）
```

**備考**: 対象RDBMS接続（Unit 3以降）はUnit 1のdeployment-architecture.mdに準じ本図では
省略。ロードバランサ・APIゲートウェイは単一インスタンス構成のため配置しない。
