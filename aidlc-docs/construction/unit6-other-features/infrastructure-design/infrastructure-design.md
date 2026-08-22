# Infrastructure Design — Unit 6: その他機能

Unit 1〜5で確立済みの実行環境（クラウドプロバイダ不使用、自己完結型WAR/Dockerコンテナ、
`eclipse-temurin:25-jre`、H2ファイルベース永続化、単一インスタンス構成）をそのまま踏襲する。
本ドキュメントではUnit 6固有の追加事項（DBマイグレーション）を扱う。

## 内部データベース（H2）へのテーブル追加・インデックス追加

- Flywayマイグレーションは、Unit 1〜5からの連番を継続する（`V17`から、Question 1）
- `V17`: `saved_query`テーブルを作成する
- `V18`: `query_execution_history`テーブルを作成する。フィルタ列
  （`executed_by_user_id`/`connection_id`/`executed_at`）へのインデックスは、この
  `CREATE TABLE`マイグレーション内にインラインで定義する（Question 2）
- `V19`: 既存テーブル`audit_event`のフィルタ列（`event_type`/`actor_user_id`/
  `occurred_at`）へのインデックスを追加する単独マイグレーション（`CREATE INDEX`文の
  みを含む、Question 2）
- 具体的なDDL（列定義・インデックス定義の詳細）はCode Generationで確定する

## 既存依存の再利用

- クエリ実行のJDBCコネクション取得はUnit 3の`ConnectionPoolRegistry`をそのまま再利用する
- パラメータ検出はSpring `NamedParameterUtils`（Spring Frameworkの標準機能）を利用する。
  新規ライブラリの追加は不要

## N/A項目

Deployment Environment（クラウドプロバイダ）、Compute Infrastructure、Messaging
Infrastructure、Networking Infrastructure、Monitoring Infrastructure、Shared
Infrastructure（マルチテナンシー）は、unit6-other-features-infrastructure-design-plan.md
に記載の根拠（Unit 1〜5と同一根拠）によりN/A。ロードバランサ・APIゲートウェイも
単一インスタンス構成のためN/A。
