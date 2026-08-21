# Code Generation Plan — Unit 1: デザインシステム基盤

## Unit Context

- **対応ストーリー**: なし（requirements.md §3由来の技術基盤。unit-of-work.md参照）
- **含まれるコンポーネント**: PlatformInfrastructureComponent（構造化ログ・i18n・アプリ全体
  テーマ設定）
- **依存Unit**: なし（最初のUnit）
- **後続Unitへの提供インターフェース**: SecurityConfig（FilterChain拡張ポイント）、
  GlobalExceptionHandler、CorrelationIdFilter、MessageSourceConfig（i18n）、AppShell/テーマ
  コンポーネント（フロントエンド）
- **本UnitがオーナーとなるDBエンティティ**: `AppTheme`（アプリ全体のブランドカラー・フォント設定、
  単一レコード）
- **明示的なスコープ外**: `UserLocale`（ユーザ別UI表示言語設定）の永続化実装は、`User`
  エンティティが存在しないため本Unitでは行わない。`PlatformInfrastructureComponent`の
  `getUserLocale`/`setUserLocale`はインターフェースとして定義するが、具体的な永続化実装は
  Unit 2（ユーザ管理、`User`エンティティ作成時）で行う。JWT認証フィルタも同様に、トークン
  発行・ユーザ検証ロジックの具体実装はUnit 2で行い、本Unitでは検証の骨組み（フィルタ構造・
  例外処理）のみを整備する

## 本計画で追加確定する技術選定（NFR Design未決定分）

- **DBマイグレーションツール**: Flyway（H2向け）を採用する。スキーマ変更履歴を明示的な
  バージョン管理下に置くことで、監査ログを重視する本プロジェクトの方針と整合させる
- **フロントエンドi18nライブラリ**: `react-i18next`（Reactエコシステムで最も広く使われる
  i18nライブラリ）
- **フロントエンドルーティング**: `react-router-dom`（Reactエコシステムの標準的なルーティング
  ライブラリ）
- **API仕様自動生成**: `springdoc-openapi`（requirements.md 3章「OpenAPI仕様を自動生成」の
  実装ライブラリとして採用）

## 実行ステップ

### Step 1: プロジェクト構造セットアップ（Greenfield）
- [x] 1.1 ルート`settings.gradle.kts`（`backend`、`frontend`、
      `libs/java-mustache-processor/cherry-mustache-core`をサブプロジェクトとして定義。
      実際のsubmodule構造確認により`core`から`cherry-mustache-core`へパスを修正した）
- [x] 1.2 ルート`build.gradle.kts`（共通プラグイン・リポジトリ設定、`dependencyLocking`有効化）
- [x] 1.3 `backend/build.gradle.kts`（Spring Boot 4.1、依存関係: web, security, data-jpa,
      h2, validation, flyway-core, logstash-logback-encoder, bucket4j-core, springdoc-openapi,
      jqwik、テスト: JUnit5, Mockito）
- [x] 1.4 `backend/src/main/resources/application.yml`（環境変数プレースホルダ、H2ファイル
      ベース永続化設定、Flyway設定）
- [x] 1.5 `frontend/package.json`・`vite.config.ts`・`tsconfig.json`（React 19、
      react-router-dom、react-i18next、Vitest + React Testing Library。make-you-chic-uiへの
      参照は実際のnpm workspaceパッケージ位置`packages/make-you-chic-ui`に合わせて修正した）
- [x] 1.6 git submodule追加: `libs/make-you-chic-ui`、`libs/java-mustache-processor`
      （ユーザー確認済み、実行完了）
- [x] 1.7 `devenv/docker-compose.yml`（MailPit常時起動、MySQL/MariaDB/PostgreSQLは
      Composeプロファイルで選択起動）
- [x] 1.8 ルート`.gitignore`更新（ビルド成果物、H2データファイル、`node_modules`等）

### Step 2: Business Logic Generation
- [x] 2.1 `AppTheme`ドメインモデル（ブランドカラー・フォントの値オブジェクト。実際の
      `make-you-chic-ui`ソース調査によりブランドカラー値をBLUE/GREEN/PURPLE/ORANGE、
      フォントを`sans`/`serif`の2値に確定した）
- [x] 2.2 `AppThemeService`（`getAppTheme`/`setAppTheme`のビジネスロジック、`setAppTheme`に
      `@PreAuthorize("hasRole('ADMIN')")`を付与）
- [x] 2.3 `MessageResolver`（`resolveMessage`の実装、`MessageSource`をラップ）＋
      `MessageSourceConfig`、日英メッセージリソース

### Step 3: Business Logic Unit Testing
- [x] 3.1 `AppThemeServiceImplTest`（JUnit5 + Mockito、正常系2件）
- [x] 3.2 PBT対象の識別: 「No PBT properties identified」と判定（business-logic-summary.md
      参照）

### Step 4: Business Logic Summary
- [x] 4.1 `aidlc-docs/construction/unit1-design-system-foundation/code/business-logic-summary.md`
      を生成する

### Step 5: API Layer Generation
- [x] 5.1 `SecurityConfig`（FilterChain定義、ステートレスセッション、セキュリティヘッダ。
      Unit 1時点では公開エンドポイントがないため`anyRequest().authenticated()`のみ。CORSは
      同一オリジン配信のため未設定＝ワイルドカード許可も一切発行されない）
- [x] 5.2 `CorrelationIdFilter`
- [x] 5.3 `RateLimitFilter`（bucket4j、IPアドレス単位、1分10リクエスト）
- [x] 5.4 `JwtAuthenticationFilter`（骨組みのみ。`JwtTokenValidator`インターフェース＋
      `NoopJwtTokenValidator`（`@ConditionalOnMissingBean`）でUnit 2の実装差し替えに対応）
- [x] 5.5 `GlobalExceptionHandler`（`@RestControllerAdvice`、統一エラーレスポンス構造）＋
      `RestAuthenticationEntryPoint`・`RestAccessDeniedHandler`（フィルタ層での401/403）
- [x] 5.6 `AppThemeController`（`GET`/`PUT /api/theme`。GETは全認証ユーザー、PUTは
      サービス層の`@PreAuthorize`で管理者限定）
- [x] 5.7 springdoc-openapiの設定（`OpenApiConfig`、Swagger UI有効化）

### Step 6: API Layer Unit Testing
- [x] 6.1 `AppThemeControllerTest`（`@WebMvcTest`、正常系・バリデーションエラー）
- [x] 6.2 `GlobalExceptionHandlerTest`（内部詳細を漏らさないことを検証）
- [x] 6.3 `RateLimitFilterTest`（閾値超過時に429を返すことを検証）

### Step 7: API Layer Summary
- [x] 7.1 `aidlc-docs/construction/unit1-design-system-foundation/code/api-layer-summary.md`
      を生成する

### Step 8: Repository Layer Generation
- [x] 8.1 `AppThemeEntity`（JPA、単一レコード想定、id固定値1）
- [x] 8.2 `AppThemeJpaRepository`（Spring Data JPA）＋`AppThemeRepositoryImpl`
      （`AppThemeRepository`ポートのアダプタ）

### Step 9: Repository Layer Unit Testing
- [x] 9.1 `AppThemeRepositoryImplTest`（`@DataJpaTest`、既定値フォールバック・ラウンド
       トリップ・更新時の非重複挿入を検証）

### Step 10: Repository Layer Summary
- [x] 10.1 `aidlc-docs/construction/unit1-design-system-foundation/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [ ] 11.1 `frontend/src`ディレクトリ構成（`app/`, `shared/`, `features/`等）
- [ ] 11.2 `make-you-chic-ui`のAppShell（Sidebar+Topbar+Content）を用いた共通レイアウト
- [ ] 11.3 テーマプロバイダ（ライト/ダーク・文字サイズ: localStorage、ブランドカラー・
       フォント: バックエンドAPI `/api/admin/theme`から取得。一般ユーザは読み取りのみ、
       管理者は変更可能なUIを持つ）
- [ ] 11.4 react-i18nextセットアップ（日本語・英語のメッセージリソース、Unit 1時点では
       共通UI文言のみ）
- [ ] 11.5 react-router-domによるルーティング骨組み（各Unitが後続で画面を追加する土台）

### Step 12: Frontend Components Unit Testing
- [ ] 12.1 AppShell・テーマ切り替えのコンポーネントテスト（Vitest + React Testing Library）

### Step 13: Frontend Components Summary
- [ ] 13.1 `aidlc-docs/construction/unit1-design-system-foundation/code/frontend-summary.md`
       を生成する

### Step 14: Database Migration Scripts
- [x] 14.1 Flywayマイグレーションスクリプト（`V1__create_app_theme.sql`。Repository Layer
       Unit Testingの前提として、Step 8-9と合わせて先行生成した）

### Step 15: Documentation Generation
- [ ] 15.1 ルート`README.md`の新規作成（プロジェクト概要、開発環境セットアップ手順、
       git submodule取得手順を含む）

### Step 16: Deployment Artifacts Generation
- [ ] 16.1 `Dockerfile`（マルチステージビルド、`eclipse-temurin:25-jre`ベース）
- [ ] 16.2 `.env.example`（環境変数のサンプル）

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
