# Infrastructure Design Plan — Unit 3: 対象RDBMSセットアップ

Unit 1・2で確定済みの基盤（クラウド不使用・自己完結型WAR/Dockerコンテナ、H2ファイルベース
永続化、devenvのDocker Compose）をそのまま踏襲する。Unit 3はdevenvで既に用意されている
対象RDBMSコンテナ（MySQL/MariaDB/PostgreSQL、Unit 1でComposeプロファイル定義済み）に、
Unit 3のコードから実際に接続する最初のUnitである。

## 実行チェックリスト

- [ ] Step 1: nfr-design/logical-components.md、devenv/docker-compose.ymlを分析する
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: Infrastructure Design成果物生成
  - [ ] `infrastructure-design.md`
  - [ ] `deployment-architecture.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。Unit 1・2と同じ根拠
- **Compute Infrastructure**: N/A。Unit 1・2と同じ根拠（単一インスタンス）
- **Storage Infrastructure**: 該当。Unit 3で追加するテーブル（Connection/Schema/Table/
  Column/ForeignKeyConstraint）のFlywayマイグレーション追加方針をQuestion 1で具体化する
- **Messaging Infrastructure**: N/A。Unit 1〜2と同じ根拠
- **Networking Infrastructure**: 該当。devenvの対象RDBMSコンテナへの接続情報
  （ホスト名・ポート）をQuestion 2で具体化する。ロードバランサ・APIゲートウェイは
  単一インスタンス構成のためN/A
- **Monitoring Infrastructure**: N/A。Unit 1〜2と同じ根拠（本格的な監視基盤は導入しない）
- **Shared Infrastructure（マルチテナンシー等）**: N/A。Unit 1〜2と同じ根拠

---

## 質問

### Question 1: Flywayマイグレーションのバージョニング

Unit 1〜2で`V1`〜`V5`を使用済み。Unit 3で追加するテーブル（`connection`、
`schema`（予約語のため`db_schema`等に変更が必要）、`db_table`、`db_column`、
`foreign_key_constraint`）のバージョン番号をどう採番するか。

A) （推奨）Unit 1・2からの連番を継続する（`V6`から開始）。全Unit共通の単一マイグレーション
履歴とする方針を継続する

B) Unitごとにマイグレーション番号の帯を予約する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: devenvでの対象RDBMS接続情報

devenv/docker-compose.ymlは対象RDBMSコンテナのポートをホストにマッピング済み
（MySQL: 3306、MariaDB: 3307、PostgreSQL: 5432）。devenv自体にはbackendアプリの
コンテナ定義がなく、開発時は`./gradlew :backend:bootRun`でホスト上に直接実行する構成
（Unit 1のdeployment-architecture.md参照）。

A) （推奨）開発者が管理者ダッシュボードで接続登録する際、ホストに`localhost`、ポートに
上記マッピング済みポートを指定する（backendはホスト上で直接実行されるため、コンテナ名
ではなく`localhost`で到達できる）。README.mdにこの前提を明記する

B) devenvにbackendアプリ自体のコンテナ定義を追加し、Docker Composeのサービス名で
名前解決する構成に変更する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 接続暗号鍵の環境変数管理

A) （推奨）Unit 1・2で確立済みの`.env`/環境変数方式を踏襲する。`.env.example`に
`MM5_CONNECTION_SECRET_KEY`（32byte以上）を追加する

B) 専用シークレットマネージャの導入を今のタイミングで検討する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Infrastructure Design成果物を生成する。
