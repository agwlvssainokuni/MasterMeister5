# MasterMeister5

対象RDBMSのマスタデータ管理・アクセス制御・監査を行うWebアプリケーション。詳細な要件は
`aidlc-docs/inception/requirements/requirements.md` を参照。

## プロジェクト構成

```
backend/    Spring Boot アプリケーション（フロントエンドのビルド成果物を内包）
frontend/   React アプリケーション (Vite)
devenv/     開発環境 (Docker Compose)
libs/       git submodule配置先（make-you-chic-ui、java-mustache-processor）
```

## 開発環境セットアップ

### 1. リポジトリの取得（submodule込み）

```bash
git clone --recurse-submodules <このリポジトリのURL>
# 既にcloneしている場合
git submodule update --init --recursive
```

### 2. `libs/make-you-chic-ui` のビルド

frontendが依存する`make-you-chic-ui`はビルド成果物（`dist/`）を消費する構成のため、
初回セットアップ時と更新時にビルドが必要（詳細は
`libs/make-you-chic-ui/docs/integration-guide.md` 参照）。

`./gradlew :backend:bootWar`／`:backend:war`実行時はGradle（`npmBuildMakeYouChicUi`タスク）
が自動的にビルドするため、本番相当ビルドではこの手順は不要。フロントエンドを`npm run dev`
（Vite開発サーバ）で個別に起動する場合はGradleを介さないため、以下を手動で実行しておくこと。

```bash
cd libs/make-you-chic-ui
npm install
npm run build
cd ../..
```

### 3. `libs/java-mustache-processor` の確認

Gradleマルチモジュール構成に`cherry-mustache-core`サブプロジェクトとして直接組み込むため、
追加のビルド操作は不要（`./gradlew build`実行時に他のサブプロジェクトと同様にビルドされる）。

### 4. バックエンドの起動

```bash
./gradlew :backend:bootRun
```

内部データベース（H2、ファイルベース）は`./data/mastermeister5`に作成される
（`MM5_INTERNAL_DB_PATH`環境変数で変更可能）。

初回起動時、`MM5_INITIAL_ADMIN_EMAIL`/`MM5_INITIAL_ADMIN_PASSWORD`環境変数で指定した
初期管理者アカウントが自動作成される（Unit 2、招待フローを経ない）。未設定の場合は
作成をスキップする。JWT署名鍵（`MM5_JWT_SECRET`、32byte以上）も必須。詳細は
`.env.example`を参照。

### 5. フロントエンドの起動

```bash
cd frontend
npm install
npm run dev
```

`/api`へのリクエストは`vite.config.ts`のプロキシ設定によりバックエンド（`localhost:8080`）
へ転送される。

### 6. 開発用外部サービス（Docker Compose）

```bash
cd devenv
docker compose up               # MailPitのみ起動
docker compose --profile mysql up     # MySQLも起動
docker compose --profile mariadb up   # MariaDBも起動
docker compose --profile postgres up  # PostgreSQLも起動
```

MailPit Web UI: http://localhost:8025

対象RDBMSコンテナはホストポートにマッピング済み（MySQL: 3306、MariaDB: 3307、
PostgreSQL: 5432）。backendアプリ自体はdevenvにコンテナ定義を持たずホスト上で直接
実行されるため、管理者ダッシュボードで対象RDBMS接続を登録する際はホストに`localhost`、
ポートに上記の値を指定する（Unit 3）。接続パスワードの暗号化には`MM5_CONNECTION_SECRET_KEY`
環境変数（32byte以上）が必須。詳細は`.env.example`を参照。

対象RDBMSの接続登録・スキーマ取込（Unit 3）に続き、管理者ダッシュボードの
「グループ管理」画面でユーザグループの作成・所属管理、「アクセス権限」画面で
スキーマ/テーブル/カラム単位の主権限（NONE/READ/UPDATE）・補助権限
（作成可/削除可）の設定、およびYAML形式でのエクスポート/インポートが行える（Unit 4）。
追加の環境変数設定は不要（実効権限キャッシュはプロセス内Caffeineキャッシュのため）。

アクセス権限モデル（Unit 4）に基づき、一般ユーザ・管理者を問わず「データ表示」画面で
権限のあるテーブル/ビューの一覧・レコード閲覧・フィルタ（WHERE句手入力）・インライン
編集・作成・削除が行える。管理者ダッシュボードの「表示・入力カスタマイズ」画面では、
対象RDBMS接続ごとに表示ラベル・列順・入力ウィジェット種別・簡易バリデーションルールを
YAML形式でエクスポート/インポートできる（Unit 5）。追加の環境変数設定は不要。

## テスト

```bash
./gradlew test          # バックエンド（JUnit5 + jqwik）
cd frontend && npm test  # フロントエンド（Vitest + React Testing Library）
```

## API仕様書

バックエンド起動後、Swagger UIで確認できる: http://localhost:8080/swagger-ui.html
（ログイン等の一部エンドポイントを除き認証が必要。Unit 2でログイン機能が実装済み）

## ビルド（本番相当）

```bash
./gradlew :backend:bootWar
```

`make-you-chic-ui`のビルド（未実施の場合）→ frontendのビルド → `backend/src/main/resources/static`
への出力 → 単一WARへの内包、まで1コマンドで完結する。Dockerコンテナ化する場合は`Dockerfile`を
参照。
