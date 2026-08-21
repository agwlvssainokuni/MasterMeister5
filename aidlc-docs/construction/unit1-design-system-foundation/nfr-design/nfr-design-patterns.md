# NFR Design Patterns — Unit 1: デザインシステム基盤

nfr-requirements.mdの各SECURITYルールへの対応方針を、具体的な設計パターンに落とし込む。

## Resilience / Scalability / Performance Patterns
N/A（NFR Design Plan参照。resiliency-baseline不適用、単一インスタンス、性能目標値なしの
方針が確定済みのため、本Unitでは特別な耐障害性・スケーリング・性能最適化パターンを導入しない）

## Security Patterns

### 認証・認可（SECURITY-08）
- Spring SecurityのFilterChainに、JWTを検証するカスタムフィルタ（`JwtAuthenticationFilter`）
  を`UsernamePasswordAuthenticationFilter`の前段に追加する
- セッションポリシーは`STATELESS`とする（JWTベースのためサーバ側セッションを持たない）
- デフォルト拒否パターン: `anyRequest().authenticated()`を基本とし、公開エンドポイント
  （ログイン、招待受諾、パスワードリセット申請/実行等）のみ`permitAll()`で明示的に許可する
  ホワイトリスト方式

### HTTPセキュリティヘッダ（SECURITY-04）
- Spring Securityの`headers {}` DSLで以下を設定する:
  - CSP: `default-src 'self'; script-src 'self'; style-src 'self'`（Question 2の回答により
    追加の許可ソースなし。`unsafe-inline`/`unsafe-eval`は使用しない）
  - HSTS: `max-age=31536000; includeSubDomains`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`

### レート制限（SECURITY-11）
- bucket4jによるトークンバケットパターンを、`RateLimitFilter`（Servletフィルタ）として
  FilterChainの早い段階（認証処理より前）に配置する
- 制限単位: IPアドレス単位（`X-Forwarded-For`優先、なければリモートアドレス）
- 初期閾値: 1分あたり10リクエスト、毎分補充（Question 1の回答）。対象は未認証で叩ける公開
  エンドポイント全般（ログイン、パスワードリセット申請/実行、招待受諾）
- 制限超過時は`429 Too Many Requests`を返す（統一エラーレスポンス構造に従う）

### 構造化ログ・相関ID（SECURITY-03）
- Logback + logstash-logback-encoderでJSON構造化ログを出力する
- 相関ID生成パターン: `CorrelationIdFilter`（Servletフィルタ）がリクエストごとにUUIDを生成し、
  MDC（Mapped Diagnostic Context）に格納する。ログ出力時にMDCの値が自動的に含まれる。
  レスポンスヘッダ（`X-Correlation-Id`）にも付与する（Question 3の回答: バックエンド生成、
  フロントエンドからの伝播は求めない）
- ログに機微情報（パスワード、トークン、Authorizationヘッダ）を出力しないよう、
  ログ出力前のマスキング/除外ルールを定める

### エラーハンドリング（SECURITY-15、SECURITY-09）
- `@RestControllerAdvice`によるグローバル例外ハンドラパターンを採用する
- 統一エラーレスポンス構造（Question 4の回答）:
  ```json
  {
    "errorCode": "string",
    "message": "string（i18n対応、ロケールに応じて解決）",
    "correlationId": "string（相関IDフィルタが生成したUUID）"
  }
  ```
- 本番相当の設定では、未処理例外に対して汎用メッセージ（`errorCode: INTERNAL_ERROR`等）を返し、
  スタックトレース・内部パス・フレームワークバージョンは応答に含めない
- `message`はPlatformInfrastructureComponentの`resolveMessage`を通じてロケール別に解決する

### サプライチェーン（SECURITY-10）
- Gradleの`dependencyLocking`機能を全モジュール（`backend`、`frontend`のnpm依存は別途
  `package-lock.json`）で有効化し、lockファイルをコミットする
- GitHub Dependabotをリポジトリ設定で有効化し、Gradle（Maven形式）・npm双方の依存関係を
  対象にする
