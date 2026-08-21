# Infrastructure Design Plan — Unit 1: デザインシステム基盤

MasterMeister5はクラウドプロバイダを使用せず、自己完結型実行可能WAR（Twelve-Factor App準拠）
またはDockerコンテナとしてデプロイする方針が requirements.md 3章で既に確定している。Unit 1の
Infrastructure Designでは、NFR Designの論理コンポーネント（SecurityConfig、LoggingConfig等）を
実際の開発・実行環境（Docker Compose devenv、コンテナビルド）にどうマッピングするかを定める。

## 実行チェックリスト

- [ ] Step A: nfr-design/logical-components.md、requirements.md 3章（デプロイ・開発環境）を分析する
- [ ] Step B: 承認された回答（下記Clarifying Questions参照）を反映する
- [ ] Step C: `aidlc-docs/construction/unit1-design-system-foundation/infrastructure-design/infrastructure-design.md` を生成する
- [ ] Step D: `aidlc-docs/construction/unit1-design-system-foundation/infrastructure-design/deployment-architecture.md` を生成する

## 既にN/A判定（回答不要、根拠を明記）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。requirements.md 3章に
  「クラウドプロバイダは使用せず、自己完結型実行可能WAR／Dockerコンテナ／将来的なTomcat
  WARデプロイ」と明記済み。AWS/Azure/GCP固有のサービス選定は行わない
- **Messaging Infrastructure**: N/A。メッセージキュー等の非同期基盤は
  component-dependency.mdで「本プロジェクトのスコープでは使用しない」と明記済み
- **Networking Infrastructure（ロードバランサ・APIゲートウェイ）**: N/A。単一インスタンス
  構成（同時利用者数約10名）のため、ロードバランサ・APIゲートウェイは不要。CORS設定は
  NFR DesignのSecurityConfigで対応済み
- **Shared Infrastructure（マルチテナンシー等）**: N/A。単一テナントの社内ツールであり、
  リソース分離・マルチテナンシーの設計は不要
- **Monitoring Infrastructure**: 部分的にN/A。requirements.md 5章「本格的な監視ダッシュボード
  は求めず、ログベースの軽量な検知の仕組みで足りる」と明記済みのため、Unit 1ではLogback構造化
  ログの標準出力（Docker/コンテナのログドライバに委ねる）以上の監視基盤は導入しない

## Clarifying Questions

### Question 1: devenvのDocker Compose構成（対象RDBMSコンテナ）
開発環境（Docker Compose）で用意する対象RDBMSコンテナの構成をどうしますか？
（AI推奨: A — 単独開発者が必要に応じて起動するコンテナを選べる方が、開発マシンのリソース
消費を抑えられる。3種類同時起動は開発機への負荷が大きく、通常は1種類あれば開発を進められる）

A) MySQL・MariaDB・PostgreSQLの3サービスをすべて`docker-compose.yml`に定義するが、
   デフォルトでは起動しない（`docker compose up`時にプロファイル指定で選択起動する）

B) 3サービスをすべて常時起動対象として定義する（`docker compose up`で全て起動）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 2: 本番相当コンテナのベースイメージ
本番相当のDockerコンテナ化（実行可能WARを実行するコンテナ）のベースイメージをどう選定
しますか？
（AI推奨: A — Eclipse Temurinは信頼できる公式配布のOpenJDKディストリビューションであり、
JREのみのイメージでコンテナサイズを抑えられる。バージョンをピン留めしSECURITY-10
（latestタグ不使用）に準拠する）

A) `eclipse-temurin:25-jre`をバージョンピン留めして使用する（`latest`タグは使用しない）

B) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 3: 内部DB（H2）の永続化モード
requirements.mdで内部データベースはH2 Databaseと確定していますが、永続化モードはどうしますか？
（AI推奨: B — インメモリモードは再起動でデータが消えるため本番運用に適さない。単独開発・
10名規模という前提でも、ユーザデータ・権限設定・監査ログ等の運用データを永続化する必要がある
ため、ファイルベースモードを開発・本番共通のデフォルトとする）

A) インメモリモード（アプリケーション再起動でデータが消える。開発・テストのみを想定）

B) ファイルベースモード（ディスク上のファイルに永続化。開発・本番共通で使用し、Dockerでは
   ボリュームマウントでデータを永続化する）

C) 環境ごとに切り替え可能にする（開発はインメモリ、本番はファイルベース。環境変数で切替）

D) Other (please describe after [Answer]: tag below)

[Answer]:

## Mandatory Artifacts

- [ ] `aidlc-docs/construction/unit1-design-system-foundation/infrastructure-design/infrastructure-design.md`
- [ ] `aidlc-docs/construction/unit1-design-system-foundation/infrastructure-design/deployment-architecture.md`
