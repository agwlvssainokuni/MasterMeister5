# Infrastructure Design — Unit 3: 対象RDBMSセットアップ

Unit 1・2で確立済みの実行環境（クラウドプロバイダ不使用、自己完結型WAR/Dockerコンテナ、
`eclipse-temurin:25-jre`、H2ファイルベース永続化）をそのまま踏襲する。本ドキュメントでは
Unit 3固有の追加事項（DBマイグレーション、devenvでの対象RDBMS接続情報、暗号鍵）を扱う。

## 内部データベース（H2）へのテーブル追加

- Flywayマイグレーションは、Unit 1・2からの連番を継続する（`V6`から、`connection`・
  `db_schema`・`db_table`・`db_column`・`foreign_key_constraint`の各テーブルを追加する。
  `schema`・`table`はSQL予約語のため、物理テーブル名は`db_schema`・`db_table`とする、
  Question 1）
- 具体的なDDLはCode Generationで確定する

## devenvでの対象RDBMS接続情報（Question 2）

- devenv（`devenv/docker-compose.yml`）が提供する対象RDBMSコンテナは、ホストポートに
  マッピング済み: MySQL=3306、MariaDB=3307、PostgreSQL=5432
- backendアプリはdevenvにコンテナ定義を持たず、開発者のホスト上で`./gradlew :backend:bootRun`
  として直接実行される（Unit 1のdeployment-architecture.md）
- そのため、開発時に管理者ダッシュボードで対象RDBMS接続を登録する際は、ホストに
  `localhost`、ポートに上記マッピング済みポートを指定する。README.mdにこの前提を
  明記する

## 接続暗号鍵の環境変数管理（Question 3）

- Unit 1・2で確立済みの`.env`/環境変数方式を踏襲する
- `.env.example`に`MM5_CONNECTION_SECRET_KEY`（32byte以上、AES-256-GCM用）を追加する

## コネクションプール・接続確認

- 登録済み接続ごとに、HikariCPプールを遅延生成・キャッシュする（NFR Design参照）。
  対象RDBMS自体のインフラ構成（本番環境でのネットワーク到達性・ファイアウォール設定等）は
  運用フェーズの責任範囲とし、Unit 3のコードは環境変数/DB経由で受け取った接続情報を
  そのまま使用する

## N/A項目

Deployment Environment（クラウドプロバイダ）、Compute Infrastructure、Messaging
Infrastructure、Monitoring Infrastructure、Shared Infrastructure（マルチテナンシー）は、
unit3-target-rdbms-setup-infrastructure-design-plan.mdに記載の根拠（Unit 1・2と同一根拠）
によりN/A。ロードバランサ・APIゲートウェイも単一インスタンス構成のためN/A。
