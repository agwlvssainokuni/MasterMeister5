# API Layer Summary — Unit 1: デザインシステム基盤

## 生成したクラス（`cherry.mastermeister5.platform.security`）
- `SecurityConfig`: FilterChain定義。ステートレスセッション、CSRF無効（Cookieセッションを
  使わないJWT APIのため）、`anyRequest().authenticated()`（Unit 1時点では公開エンドポイント
  なし）、CSP/HSTS/X-Content-Type-Options/X-Frame-Options/Referrer-Policyの各セキュリティ
  ヘッダ、カスタム認証エントリポイント・アクセス拒否ハンドラ
- `CorrelationIdFilter`（`cherry.mastermeister5.platform.logging`）、`RateLimitFilter`、
  `RateLimitConfig`、`RateLimitBucketSource`、`RateLimitProperties`（bucket4j、IPアドレス
  単位、デフォルト1分10リクエスト）
- `JwtAuthenticationFilter`・`JwtTokenValidator`・`JwtAuthentication`・
  `NoopJwtTokenValidator`: JWT検証の骨組み。**Unit 2でNoopJwtTokenValidatorを実際の実装に
  置き換える**（`@ConditionalOnMissingBean`により、Unit 2が本実装のBeanを追加すると自動的に
  置き換わる設計）
- `RestAuthenticationEntryPoint`・`RestAccessDeniedHandler`: 未認証(401)・権限不足(403)時に
  統一エラーレスポンスを返す

## 生成したクラス（`cherry.mastermeister5.platform.web`）
- `ErrorResponse`・`ErrorResponseFactory`: 統一エラーレスポンス構造
  `{errorCode, message, correlationId}`
- `GlobalExceptionHandler`: バリデーションエラー(400)・未処理例外(500)を捕捉。スタック
  トレース等の内部情報は応答に含めない
- `OpenApiConfig`: springdoc-openapiの基本情報設定（Swagger UI・`/v3/api-docs`は
  springdoc-openapi-starter-webmvc-uiの自動設定により有効化される）

## 生成したクラス（`cherry.mastermeister5.platform.theme`）
- `AppThemeController`: `GET /api/theme`（全認証ユーザー）、`PUT /api/theme`（管理者限定、
  `AppThemeServiceImpl`の`@PreAuthorize`で強制）
- `AppThemeRequest`・`AppThemeResponse`: リクエスト/レスポンスDTO

## 生成したテスト
- `AppThemeControllerTest`（`@WebMvcTest`、正常系・バリデーションエラー）
- `GlobalExceptionHandlerTest`（未処理例外時に内部詳細を漏らさないことを検証）
- `RateLimitFilterTest`（閾値超過時に429を返すことを検証）

## 既知の制約（Unit 2への申し送り）
- Unit 1時点では公開（未認証で叩ける）エンドポイントが存在しないため、`GET /api/theme`も
  認証が必須。ログイン前の画面（招待受諾・パスワードリセット画面等）でのテーマ適用方法は
  Unit 2で`SecurityConfig`に`permitAll()`ルールを追加する際にあわせて検討する
