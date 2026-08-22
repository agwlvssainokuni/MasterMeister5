# Infrastructure Design Plan — Unit 4: アクセス制御

Unit 1〜3で確定済みの基盤（クラウド不使用・自己完結型WAR/Dockerコンテナ、H2ファイルベース
永続化、devenvのDocker Compose、単一インスタンス構成）をそのまま踏襲する。Unit 4は
新規の外部インフラ（キャッシュサーバ等）を導入せず、実効権限キャッシュ（Caffeine）は
アプリケーションプロセス内で完結する（NFR Design Question 2で確定済み）。

## 実行チェックリスト

- [x] Step 1: nfr-design/logical-components.mdを分析する（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [x] Step 6: Infrastructure Design成果物生成
  - [x] `infrastructure-design.md`
  - [x] `deployment-architecture.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。Unit 1〜3と同じ根拠
- **Compute Infrastructure**: N/A。Unit 1〜3と同じ根拠（単一インスタンス）。Caffeine
  キャッシュはアプリケーションプロセス内のヒープメモリを使用するのみで、追加の
  コンピュートリソースは不要
- **Storage Infrastructure**: 該当。Unit 4で追加するテーブル（`user_group`、
  `group_membership`、`permission_entry`）のFlywayマイグレーション追加方針を
  Question 1で具体化する
- **Messaging Infrastructure**: N/A。Unit 1〜3と同じ根拠
- **Networking Infrastructure**: N/A。Unit 4は新規の外部ネットワーク経路を追加しない
  （既存のH2・対象RDBMS接続経路をUnit 3の枠組みのまま利用する）
- **Monitoring Infrastructure**: N/A。Unit 1〜3と同じ根拠
- **Shared Infrastructure（マルチテナンシー等）**: N/A。Unit 1〜3と同じ根拠

---

## 質問

### Question 1: Flywayマイグレーションのバージョニング

Unit 1〜3で`V1`〜`V10`を使用済み。Unit 4で追加するテーブル（`user_group`、
`group_membership`、`permission_entry`）のバージョン番号をどう採番するか。

A) （推奨）Unit 1〜3からの連番を継続する（`V11`から開始）。全Unit共通の単一マイグレーション
履歴とする方針を継続する

B) Unitごとにマイグレーション番号の帯を予約する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: Caffeine依存の追加方式

A) （推奨）Caffeine（`com.github.ben-manes.caffeine:caffeine`）を`backend/build.gradle.kts`
に追加する（Unit 3のJDBCドライバ追加と同じ、Gradle `dependencyLocking`・GitHub
Dependabotの既存対象にそのまま含まれる）。外部キャッシュサーバ（Redis等）は導入しない
（単一インスタンス構成のため不要、NFR Design確定済み）

B) 将来のマルチインスタンス化を見越し、この時点でRedis等の外部キャッシュサーバ導入を
検討する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Infrastructure Design成果物を生成する。
