# Infrastructure Design — Unit 1: デザインシステム基盤

NFR Designの論理コンポーネントを、実際の開発・実行環境にマッピングする。

## 開発環境（devenv、Docker Compose）

`devenv/docker-compose.yml`に以下のサービスを定義する:

| サービス | イメージ | 用途 | 起動方式 |
|---|---|---|---|
| mailpit | `axllent/mailpit`（バージョンピン留め） | 招待メール・パスワードリセットメール等の送受信確認 | デフォルトで起動 |
| mysql | `mysql`（バージョンピン留め） | 対象RDBMS動作確認用 | Composeプロファイル`mysql`指定時のみ起動 |
| mariadb | `mariadb`（バージョンピン留め） | 対象RDBMS動作確認用 | Composeプロファイル`mariadb`指定時のみ起動 |
| postgres | `postgres`（バージョンピン留め） | 対象RDBMS動作確認用 | Composeプロファイル`postgres`指定時のみ起動 |

（Question 1の回答: 3種類の対象RDBMSコンテナは`docker-compose.yml`にすべて定義するが、
開発者がCompose Profilesで必要なものだけ選択起動する。H2は組み込みのためコンテナ不要）

## 本番相当のコンテナ化

- ベースイメージ: `eclipse-temurin:25-jre`（バージョンピン留め、`latest`タグは使用しない。
  Question 2の回答、SECURITY-10準拠）
- コンテナには`bootWar`で生成した実行可能WARのみを含める（マルチステージビルドで、ビルド
  ステージとランタイムステージを分離し、ランタイムイメージにビルドツール・ソースコードを
  含めない）
- 環境変数経由での設定（Twelve-Factor App準拠、requirements.md 3章）

## 内部データベース（H2）の永続化

- ファイルベースモードを開発・本番共通のデフォルトとする（Question 3の回答）
- Dockerコンテナ実行時は、H2のデータファイルを格納するディレクトリをボリュームマウントし、
  コンテナの再作成後もデータが失われないようにする
- devenv（ローカル実行、Docker Compose外）でも同様にファイルベースモードとし、リポジトリの
  `.gitignore`対象ディレクトリにデータファイルを配置する

## 監視・ログ

requirements.md 5章・NFR Requirements（Unit 1）に従い、本格的な監視ダッシュボードは導入しない。
Logback構造化ログ（JSON）を標準出力に出力し、Dockerのログドライバ（`json-file`等）に収集を
委ねる。

## N/A項目

Deployment Environment（クラウドプロバイダ）、Messaging Infrastructure、Networking
Infrastructure（ロードバランサ等）、Shared Infrastructure（マルチテナンシー）は、
unit1-design-system-foundation-infrastructure-design-plan.mdに記載の根拠によりN/A。
