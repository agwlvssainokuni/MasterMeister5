# Logical Components — Unit 1: デザインシステム基盤

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 1で新設する論理コンポーネント
（Spring Bean/Filter/Config）を定義する。詳細な実装（クラス名・パッケージ配置等）は
Code Generationで確定する。unit-of-work.mdのパッケージ構成方針（ハイブリッド）に従い、
これらは`platform`パッケージ配下に配置する想定。

## フィルタ（Servlet Filter / Spring Security Filter）

| 論理コンポーネント | 役割 | FilterChain上の位置 |
|---|---|---|
| CorrelationIdFilter | リクエストごとに相関ID（UUID）を生成しMDCに格納、レスポンスヘッダに付与 | 最前段 |
| RateLimitFilter | bucket4jによるIPアドレス単位のレート制限（429応答） | CorrelationIdFilterの直後、認証処理より前 |
| JwtAuthenticationFilter | JWTアクセストークンを検証し、SecurityContextに認証情報を設定する | Spring SecurityのFilterChain内、`UsernamePasswordAuthenticationFilter`の前 |

## 設定コンポーネント（Spring Configuration）

| 論理コンポーネント | 役割 |
|---|---|
| SecurityConfig | Spring SecurityのFilterChain定義（ステートレスセッション、認可ルール、
  セキュリティヘッダ、CORS設定） |
| LoggingConfig | Logback + logstash-logback-encoderの設定（JSON出力、MDC連携） |
| RateLimitConfig | bucket4jのバケット定義（IPアドレスごとのバケット生成・キャッシュ） |
| MessageSourceConfig | i18nメッセージリソース（`ResourceBundleMessageSource`等）の設定。
  日本語・英語のメッセージバンドルを登録する |
| DependencyLockingConfig | Gradleの`dependencyLocking`をルートプロジェクト・各サブ
  プロジェクトに適用する設定（`build.gradle.kts`側の設定であり、Spring Beanではない） |

## 例外処理コンポーネント

| 論理コンポーネント | 役割 |
|---|---|
| GlobalExceptionHandler | `@RestControllerAdvice`。未処理例外を捕捉し、統一エラーレスポンス
  構造（errorCode/message/correlationId）を生成する。`message`はi18nメッセージ解決を通す |

## PlatformInfrastructureComponentとの対応

component-methods.mdで定義した`PlatformInfrastructureComponent`のメソッドは、上記の論理
コンポーネントの一部として実装される:
- `log` → LoggingConfig（Logback）+ CorrelationIdFilter（MDC）
- `resolveMessage` → MessageSourceConfig
- `getUserLocale`/`setUserLocale`、`getAppTheme`/`setAppTheme` → Unit 1のCode Generationで
  内部DBエンティティ・リポジトリとして実装する（本ステージでは論理コンポーネントの範囲外、
  Functional Design相当の詳細はCode Generation計画で扱う）

## 依存関係

```
CorrelationIdFilter → (MDCへ書き込み) → LoggingConfig（Logback）
RateLimitFilter → RateLimitConfig（bucket4jバケット参照）
JwtAuthenticationFilter → SecurityConfig（FilterChain登録）
GlobalExceptionHandler → MessageSourceConfig（resolveMessage）、CorrelationIdFilter（相関ID取得）
```

循環依存はない。すべて一方向の依存である。
