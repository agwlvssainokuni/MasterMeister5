# Infrastructure Design Plan — Unit 6: その他機能

Unit 1〜5で確定済みの基盤（クラウド不使用・自己完結型WAR/Dockerコンテナ、H2ファイルベース
永続化、devenvのDocker Compose、単一インスタンス構成）をそのまま踏襲する。Unit 6は
新規の外部インフラを導入しない。

## 実行チェックリスト

- [x] Step 1: nfr-design/logical-components.mdを分析する（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [x] Step 6: Infrastructure Design成果物生成
  - [x] `infrastructure-design.md`
  - [x] `deployment-architecture.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。Unit 1〜5と同じ根拠
- **Compute Infrastructure**: N/A。Unit 1〜5と同じ根拠（単一インスタンス）
- **Storage Infrastructure**: 該当。Unit 6で追加する新規テーブル（`saved_query`、
  `query_execution_history`）と、既存テーブル（`audit_event`）へのインデックス追加の
  Flywayマイグレーション構成をQuestion 1・2で具体化する
- **Messaging Infrastructure**: N/A。Unit 6はUnit 3〜5のような循環依存回避目的の
  イベント駆動パターンを必要としない（`query`パッケージは`audit`パッケージへ一方向に
  依存するのみで、既存Unit全体で確立済みの依存方向と同じ）
- **Networking Infrastructure**: N/A。Unit 6は新規の外部ネットワーク経路を追加しない
  （既存の`/api/**`エンドポイント体系に`/api/query/**`・`/api/admin/audit-events/**`
  を追加するのみ）
- **Monitoring Infrastructure**: N/A。Unit 1〜5と同じ根拠
- **Shared Infrastructure（マルチテナンシー等）**: N/A。Unit 1〜5と同じ根拠

---

## 質問

### Question 1: Flywayマイグレーションのバージョニング

Unit 1〜5で`V1`〜`V16`を使用済み。Unit 6で追加する新規テーブル（`saved_query`、
`query_execution_history`）のバージョン番号をどう採番するか。

A) （推奨）Unit 1〜5からの連番を継続する（`V17`から開始）。全Unit共通の単一
マイグレーション履歴とする方針を継続する

B) Unitごとにマイグレーション番号の帯を予約する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 既存テーブル（`audit_event`）へのインデックス追加マイグレーション

NFR Design Question 4の決定（既存テーブルへのインデックス追加は別マイグレーション
ファイルとする）を踏まえ、具体的なファイル構成を確認する。

A) （推奨）新規テーブルのマイグレーション（`V17`、`V18`）の後に、`audit_event`への
インデックス追加のみを行う単独のマイグレーションファイル（`V19`、`CREATE INDEX`文の
みを含む）を追加する。`query_execution_history`のインデックスは、そのテーブルを
作成する`CREATE TABLE`マイグレーション内にインラインで定義する

B) `audit_event`へのインデックス追加も新規テーブルのマイグレーションと同じファイルに
まとめる

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Infrastructure Design成果物を生成する。
