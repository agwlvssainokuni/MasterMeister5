# Deployment Architecture — Unit 3: 対象RDBMSセットアップ

Unit 1・2のdeployment-architecture.mdで確定済みの構成に、Unit 3で新たに実際に使用され
始める経路（backendから対象RDBMSコンテナへの接続）を反映する。コンテナ構成・ホスト構成
自体に変更はない。

## 開発環境

```mermaid
flowchart TD
    Dev["開発者/ユーザーのブラウザ"] --> Backend["backend (Spring Boot)<br/>./gradlew bootRun"]
    Dev --> Frontend["frontend (Vite dev server)<br/>npm run dev"]
    Backend --> H2["H2 Database<br/>ファイルベース<br/>(connection/db_schema/db_table/db_column追加)"]
    Backend -->|"招待/リセットメール送信"| Mailpit["MailPit<br/>(Docker Compose)"]
    Backend -.->|"localhost:3306等<br/>(Question 2)"| TargetDB["対象RDBMS<br/>MySQL/MariaDB/PostgreSQL<br/>(Docker Compose、プロファイル選択時)"]
    Frontend -->|プロキシ/API呼び出し| Backend

    style Dev fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Backend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style Frontend fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2 fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style Mailpit fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style TargetDB fill:#FFE082,stroke:#FF8F00,stroke-width:2px,color:#000
```

### テキスト代替表現
```
開発者/ユーザーのブラウザ → backend (Spring Boot, bootRun)
開発者 → frontend (Vite dev server) → backend (API呼び出し)
backend → H2 Database (ファイルベース、connection/db_schema/db_table/db_column追加)
backend → MailPit (招待メール・パスワードリセットメール送信)
backend --(localhost:3306/3307/5432)--> 対象RDBMS (Docker Compose、プロファイル選択時。
  Unit 3で初めて実際に接続される)
```

## 本番相当（単一コンテナ）

```mermaid
flowchart TD
    User["利用者ブラウザ"] --> Container["Dockerコンテナ<br/>eclipse-temurin:25-jre<br/>実行可能WAR (backend+frontend静的資産)"]
    Container --> H2Vol["H2 Database<br/>ボリュームマウント（ファイルベース永続化）"]
    Container --> SMTP["SMTPサーバ<br/>(コンテナ外、環境変数で設定)"]
    Container -.->|"環境変数で接続情報を受け取った<br/>登録済み接続経由"| TargetDBProd["対象RDBMS<br/>(既存の運用環境、コンテナ外。<br/>ネットワーク到達性は運用フェーズの責任)"]
    TLS["リバースプロキシ<br/>(TLS終端)"] -.-> Container

    style User fill:#CE93D8,stroke:#6A1B9A,stroke-width:2px,color:#000
    style Container fill:#4CAF50,stroke:#1B5E20,stroke-width:2px,color:#fff
    style H2Vol fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style SMTP fill:#BBDEFB,stroke:#1565C0,stroke-width:2px,color:#000
    style TargetDBProd fill:#FFE082,stroke:#FF8F00,stroke-width:2px,color:#000
    style TLS fill:#FFE082,stroke:#FF8F00,stroke-width:2px,color:#000
```

### テキスト代替表現
```
利用者ブラウザ → Dockerコンテナ (eclipse-temurin:25-jre、実行可能WAR)
Dockerコンテナ → H2 Database (ボリュームマウント、ファイルベース永続化)
Dockerコンテナ → SMTPサーバ (コンテナ外、環境変数で接続設定)
Dockerコンテナ --(登録済み接続の接続情報経由)--> 対象RDBMS (コンテナ外、既存の運用環境。
  ネットワーク到達性・ファイアウォール設定は運用フェーズの責任範囲)
リバースプロキシ（TLS終端） → Dockerコンテナ （本番運用時の前提）
```

**備考**: ロードバランサ・APIゲートウェイは単一インスタンス構成のため配置しない。
