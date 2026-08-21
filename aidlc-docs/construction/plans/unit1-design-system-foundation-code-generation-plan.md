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
- [x] 11.1 `frontend/src`ディレクトリ構成（`api/`, `theme/`, `layout/`, `routes/`, `i18n/`）
- [x] 11.2 `make-you-chic-ui`のAppShell（Sidebar+Topbar+Content）を用いた共通レイアウト
      （`AppLayout`、react-routerのレイアウトルートパターン）
- [x] 11.3 テーマプロバイダ（`make-you-chic-ui`の`ThemeProvider`をライト/ダーク・文字
      サイズの管理にそのまま利用。ブランドカラー・フォントは`AppThemeSync`が
      `GET /api/theme`の結果で`setBrand`/`setFontFamily`を上書きし、管理者設定を
      全利用者に反映する。integration-guide.mdの実際のAPI仕様（`ThemeBrand`/
      `ThemeFontFamily`の値、`resolve.dedupe`要件、Webフォントの自己ホスティング手順）を
      踏まえて実装した）
- [x] 11.4 react-i18nextセットアップ（`i18n/i18n.ts`、日本語・英語のメッセージリソース。
      Unit 1時点では共通UI文言のみ。既定言語は日本語、ユーザ別言語設定との連携はUnit 2で
      ログイン時に反映する）
- [x] 11.5 react-router-domによるルーティング骨組み（`App.tsx`、`AppLayout`をレイアウト
      ルートとする`HomePage`のみのプレースホルダ）

### Step 12: Frontend Components Unit Testing
- [x] 12.1 `AppThemeSync.test.tsx`（バックエンド取得値の反映、取得失敗時のフォールバック）
- [x] 12.2 `AppLayout.test.tsx`（AppShellレンダリングとナビゲーション文言の確認）

### Step 13: Frontend Components Summary
- [x] 13.1 `aidlc-docs/construction/unit1-design-system-foundation/code/frontend-summary.md`
       を生成する

### Step 14: Database Migration Scripts
- [x] 14.1 Flywayマイグレーションスクリプト（`V1__create_app_theme.sql`。Repository Layer
       Unit Testingの前提として、Step 8-9と合わせて先行生成した）

### Step 15: Documentation Generation
- [x] 15.1 ルート`README.md`の新規作成（プロジェクト概要、開発環境セットアップ手順、
       git submodule取得・ビルド手順、devenv・テスト・API仕様書・本番ビルド手順を含む）
- [x] 15.2（計画外の追加対応）`backend/build.gradle.kts`にGradle Node Pluginを追加し、
       `bootWar`/`war`実行時にフロントエンドのビルド（`npm run build`）を自動実行するよう
       配線した（requirements.md 3章「リリースビルド時はGradle Node Pluginでフロントエンドを
       ビルドし単一WARに内包する」に対応。README作成時に未実装であることに気づき追加した）

### Step 16: Deployment Artifacts Generation
- [x] 16.1 `Dockerfile`（マルチステージビルド、`eclipse-temurin:25-jdk`でビルド→
       `eclipse-temurin:25-jre`で実行。submoduleチェックアウトが前提である旨を明記）
- [x] 16.2 `.env.example`（内部DB・レート制限の環境変数サンプル）

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。

## 完了後の実動作検証（ユーザーからの確認要求を受けて実施）

Code Generation完了後、ユーザーから「テストは全て通ったか」と問われたため、本来Build and
Testステージの役割である実行検証を前倒しで実施した。以下の問題を検出・修正した:

- **Gradle wrapper未生成**（Step 1の抜け）: `gradlew`/`gradlew.bat`/`gradle/wrapper/`を
  Gradle 9.6.1で生成し追加した
- **`flyway-database-h2`は存在しないアーティファクト**: H2サポートは`flyway-core`に内蔵されて
  いるため削除した
- **`providedRuntime("spring-boot-starter-tomcat")`が`spring-web`をランタイムクラスパスから
  除外する問題**（ユーザーからの直接指摘）: `spring-boot-starter-tomcat-runtime`に変更した
- **`UsernamePasswordAuthenticationFilter`のimportパッケージ誤り**:
  `org.springframework.security.web.authentication`に修正
- **`HttpServletResponse.SC_TOO_MANY_REQUESTS`は存在しない**:
  `HttpStatus.TOO_MANY_REQUESTS.value()`に置換
- **Spring Boot 4.1でのテストスライスAPI移動**: `@WebMvcTest`/`@AutoConfigureMockMvc`は
  `org.springframework.boot.webmvc.test.autoconfigure`、`@DataJpaTest`は
  `org.springframework.boot.data.jpa.test.autoconfigure`パッケージに移動しており、対応する
  `spring-boot-starter-webmvc-test`/`spring-boot-starter-data-jpa-test`をtestImplementationに
  追加した
- **`@WebMvcTest`が`jakarta.servlet.Filter`実装（`JwtAuthenticationFilter`等）を巻き込む問題**:
  `cherry.mastermeister5.platform.security`パッケージ全体を`excludeFilters`で除外した
- **`@DataJpaTest`はFlyway自動設定を含まない**: `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
  を明示追加し、`spring-boot-flyway`をtestImplementationに追加した
- **Mockito厳格スタビング**: `RateLimitFilterTest`の`Locale`引数を`null`固定から`any(Locale.class)`に修正
- **Gradle Node Pluginのタスク検証エラー**: `npmBuildFrontend`と`processResources`の暗黙的
  依存関係を`mustRunAfter`で明示した

修正後、`./gradlew :backend:test`（9件全て成功）、`npm test`（frontend、3件全て成功）、
`npx tsc --noEmit`（型エラーなし）、`./gradlew :backend:bootWar`（フロントエンド自動ビルドを
含め成功、WAR生成確認）をすべて実行し成功を確認した。これらの修正内容はBuild and Testステージ
でも参照する。
