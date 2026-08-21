# 技術スタック・アーキテクチャ制約

## 1. 技術スタック

| 項目 | 内容 |
|---|---|
| バックエンド言語・バージョン | [継承] Java 25（最新LTS。次期LTS登場時に移行） |
| フロントエンド言語・バージョン | [継承] Node.js 24（最新LTS。次期LTS登場時に移行） |
| バックエンドフレームワーク | [継承] Spring Boot 4.1 |
| フロントエンドフレームワーク | [継承] React 19 |
| ビルドツール（バックエンド） | [継承] Gradle 9.6。マルチモジュール構成で`frontend`をサブプロジェクトとして
  取り込み、リリースビルド時はGradle Node Pluginでフロントエンドをビルドし単一WARに内包する |
| ビルドツール（フロントエンド） | [継承] Vite（`frontend/`配下で直接`npm run dev`する開発体験は維持） |
| DBアクセス方式（内部DB） | [継承] JPA |
| DBアクセス方式（対象RDBMS） | [継承] NamedParameterJdbcTemplate |
| 内部データベース | [継承] H2 Database |
| コネクションプール | [継承] DriverManager直結ではなく、対象RDBMS用にDBコネクションプールを使用 |
| 日時型の扱い | [継承] 対象RDBMSの日時データにはjava.time APIを使用 |
| 認証方式 | [継承] JWT（アクセストークン＋リフレッシュトークン。詳細は`02-functional-requirements.md`） |
| キャッシュ | [継承] Caffeine（実効権限解決結果のキャッシュに使用） |

### UIコンポーネントライブラリ
[変更] MasterMeister4はサードパーティUIライブラリを使わず独自CSSで構築したが、本プロジェクトでは
自前のデザインシステムライブラリ [make-you-chic-ui](https://github.com/agwlvssainokuni/make-you-chic-ui)
（React + TypeScript製、Vanilla CSS採用）を使用する。

- **提供コンポーネント**: Button、FormField系（TextInput/Textarea/Select/Checkbox/Switch/
  RadioGroup）、Table、Modal、Toast、Avatar、Tabs、Dropdown、Badge、Icon、Tooltip、Card、
  Alert、AppShell（Sidebar+Topbar+Content）等
- **テーマ機能**: [追加] ライト/ダーク、4種類のブランドカラー、フォント、文字サイズの多軸
  テーマを持つ。本プロジェクトでの設定主体・永続化方式は軸ごとに分ける。
  - ライト/ダーク・文字サイズ: 利用者本人が変更可能。ブラウザストレージ（localStorage等）に
    保存する個人設定とする（サーバ側には永続化しない。デバイス・ブラウザごとに独立）
  - ブランドカラー・フォント: 管理者が設定可能。アプリ全体に適用される設定として内部DBに
    永続化する（全利用者共通。個々の利用者が上書きすることはできない）。フォントは
    `make-you-chic-ui`が提供するプリセット2種（`sans`＝ゴシック体／Noto Sans JP、
    `serif`＝明朝体／Noto Serif JP）からの選択に限定され、自由入力（任意のフォント名指定）
    には対応しない
- **組み込み方式**: [継承] git submoduleとして`libs/make-you-chic-ui/`に組み込む
  （配置方針の詳細は本ファイル「5. プロジェクト構成」参照）
- **アクセシビリティ**: [継承] 特別な準拠基準は定めない（`03-nfr.md`の記載を維持する）
- 上記の採用に伴い、他機能に先立つ「デザインシステム基盤」先行ユニットの主眼は、部品を
  ゼロから作ることではなく、ライブラリの組み込み・テーマ確定・共通レイアウト（AppShell等）の
  構成確定に変わる（優先順位自体は`00-project-overview.md`の通り維持）

### メールテンプレートエンジン
[変更] MasterMeister4はメール文面テンプレート処理に自作Mustacheエンジン
（`cherry-mustache-core`、リポジトリ内に独立サブプロジェクトとして同梱）を使っていたが、
本プロジェクトでは外部公開ライブラリ
[java-mustache-processor](https://github.com/agwlvssainokuni/java-mustache-processor)
（パッケージ`cherry.mustache`。公式Mustache仕様にフル準拠、パーシャル対応、
JSON/YAMLデータサポート）を使用する。

- 用途: 招待メール等、ユーザに送信するメール本文のテンプレート処理
  （詳細は`02-functional-requirements.md`「1. ユーザ登録・認証」）
- 配布形態: [継承] Maven Central等への公開はされていないため、`make-you-chic-ui`と同様に
  git submoduleとして組み込む（coreモジュールをGradleマルチモジュール構成に取り込む）
- MasterMeister4にあった`cherry-mustache-core/`のような自作エンジンを内蔵するサブプロジェクトは
  不要になる（外部ライブラリを利用するため。詳細は本ファイル「5. プロジェクト構成」参照）

### テストフレームワーク
[継承]
- バックエンド: JUnit5 + Mockito
- フロントエンド: Vitest + React Testing Library
- PBT: jqwik（バックエンド）、fast-check（フロントエンドに複雑ロジックがある場合）

### API仕様書の自動生成
[継承] OpenAPI仕様を自動生成し、Swagger UI等で閲覧可能にする。

### CI/CD構築
[継承] 今回のスコープには含めない。ローカルでのビルド・テスト手順の整備までとし、
開発が一通り進んだ最終段階でGitHub Actionsにより構成する（タグpushをトリガーに
GitHub Releasesを作成する仕組みを含む）。

## 2. 対象RDBMS（メンテナンス対象として必須サポート）

[継承] MySQL / MariaDB / PostgreSQL / H2 Database

## 3. データベースアーキテクチャ

- **内部DB**: [継承] アプリケーションの運用データ（ユーザ、接続設定、取込スキーマ、グループ、
  権限設定、保存クエリ、クエリ実行履歴、監査ログ等）を格納する専用データベースとする
- **対象RDBMS**: [継承] メンテナンス対象データを格納するDB。複数接続を登録・管理できる。
  スキーマ取込・権限設定・監査ログは接続ごとに独立して管理される
- **コネクションパラメータの扱い**: [継承] 接続パスワードは内部DBに可逆暗号化して保存する
  （ユーザのログインパスワードとは異なり、接続時に再利用する必要があるため）

## 4. デプロイ

- **主デプロイ形態**: [継承] Twelve-Factor App準拠、自己完結型実行可能WARファイル
  （`bootWar`、`SpringBootServletInitializer`継承）、全設定は環境変数経由、
  コンテナ環境に適した構造化ログを出力
- **その他のデプロイ形態**: [継承] 実行可能WARからのDockerコンテナ化に対応。
  Tomcatへの WARデプロイにも対応（将来対応）

## 5. プロジェクト構成

[変更] Gradleマルチモジュール構成。`frontend`を`backend`のサブプロジェクトとして取り込み、
リリースビルド時のみフロントエンドのビルド成果物を`backend`の静的リソースへコピーして
単一WARを生成する（日常のバックエンド開発・フロントエンド単体開発はそれぞれ独立して行える）。
MasterMeister4にあった`cherry-mustache-core/`（自作Mustacheエンジンの独立サブプロジェクト）は、
外部ライブラリ`java-mustache-processor`を利用するため持たない（上記「メールテンプレート
エンジン」参照）。UIライブラリ`make-you-chic-ui`・メールテンプレートエンジン
`java-mustache-processor`はgit submoduleとして`libs/`配下にまとめて配置する
（`backend`/`frontend`＝自プロジェクトの実装、`libs/`＝外部から取り込む依存ライブラリ、と
役割で分ける。将来submoduleが増えても`libs/`直下に並べるだけで済む）。

```
<project-root>/
├── settings.gradle.kts    # backend, frontend, libs/java-mustache-processor/core を
│                          # サブプロジェクトとして定義
├── backend/                # Spring Boot アプリケーション（フロントエンドのビルド成果物を内包）
├── frontend/                # React アプリケーション (Vite)
├── devenv/                  # 開発環境 (Docker Compose)
└── libs/                    # git submodule配置先
    ├── make-you-chic-ui/         # UIコンポーネントライブラリ（frontendから参照）
    └── java-mustache-processor/  # メールテンプレートエンジン（backendから参照）
```

### 開発環境
[継承] Docker Composeで構成する。
- メールサーバ: MailPit（開発時のメール送受信確認用）
- 開発用データベース: MySQL / MariaDB / PostgreSQLコンテナ、H2（組み込みのためコンテナ不要）
