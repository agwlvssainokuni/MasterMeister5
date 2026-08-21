# NFR Design Patterns — Unit 2: ユーザ管理

nfr-requirements.mdの各方針・tech-stack-decisions.mdの技術選定を、具体的な設計パターンに
落とし込む。

## Resilience / Scalability Patterns
N/A（NFR Design Plan参照。resiliency-baseline不適用、単一インスタンス・同時利用者数約10名
規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入しない）

## Performance Patterns

### パスワードハッシュのコストパラメータ
- `BCryptPasswordEncoder`はデフォルト強度（strength=10）をそのまま使う（Question 1）。
  同時利用者数規模ではCPU負荷より実装の単純さを優先する

## Security Patterns

### ログインAPIの実装パターン（Question 2）
- Spring SecurityのAuthenticationManager/UserDetailsService/AuthenticationProviderは
  経由しない。`AuthController`が直接`UserAccountComponent.authenticate`を呼び出し、
  成功時に`SecurityInfrastructureComponent`でトークンを発行する独自フロー
- Spring SecurityのFilterChainは、Unit 1で確立済みの`JwtAuthenticationFilter`による
  「保護対象エンドポイントでのJWT検証」の役割に限定する。ログイン自体はFilterChain上
  `permitAll()`の公開エンドポイントとして扱う（Unit1のnfr-design-patterns.mdの
  「デフォルト拒否・ホワイトリスト方式」を継続）

### トークン配布パターン（Question 3）
- **アクセストークン**: レスポンスボディ（JSON）で返す。フロントエンドはメモリ
  （AuthContext等のReact State）にのみ保持し、Cookie・localStorageには保存しない
- **リフレッシュトークン**: `Set-Cookie`ヘッダでHttpOnly・Secure・SameSite=Strict属性
  付きのCookieとしてサーバから発行する（`/api/auth/login`・`/api/auth/refresh`成功時）。
  JavaScriptからは一切参照できない
- **ログアウト**: `/api/auth/logout`は、対象のリフレッシュトークン（Cookieから読み取る）
  をサーバ側で失効させたうえで、`Set-Cookie`で即時失効するCookie（Max-Age=0）を返す
- **CSRF対策**: 本アプリケーションは単一WAR構成でフロントエンド静的資産とバックエンドAPIが
  同一オリジンで提供される（クロスオリジンのCORSを許可しない）ため、`SameSite=Strict`
  Cookie属性のみで実用上十分なCSRF対策となる。追加のCSRFトークン発行の仕組みは導入しない

### JWTクレーム設計
- ペイロード: `sub`（UserId）、`role`（Question 3、functional-design Question 3の推奨B
  採用によりロールクレームを含む）、`iat`、`exp`（デフォルト10分）
- 署名: HS256（tech-stack-decisions.md）。署名鍵は環境変数から注入する

### 既知漏洩パスワード照合パターン
- アプリケーション埋め込みの静的リスト（tech-stack-decisions.md）を起動時に一度読み込み、
  メモリ上のSetとして保持する。照合は正規化（小文字化等は行わない。パスワードは大文字小文字を
  区別する）した完全一致で行う

### 招待/パスワードリセットトークン生成パターン（Question 5）
- `SecureRandom`で256bit（32byte）の乱数を生成し、Base64URL（パディングなし）エンコードして
  URLに埋め込むトークン文字列とする。DB保存時はSHA-256でハッシュ化する（リフレッシュトークンと
  同じ「高エントロピーな乱数値のため高速ハッシュで十分」という考え方を適用する）

### 監査ログ記録パターン（Question 6）
- 各業務メソッド（招待・本登録・ロール変更・無効化/再有効化・ログイン成功/失敗・ログアウト・
  トークン再利用検知・パスワードリセット・パスワード変更）の処理完了直後に、当該メソッド内で
  明示的に`AuditLogComponent.recordEvent`を呼び出す。AOP等の横断的な自動記録は導入しない
