# Logical Components — Unit 2: ユーザ管理

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 2で新設する論理コンポーネント
（Spring Bean/Controller/Filter/Entity）を定義する。詳細な実装（クラス名・パッケージ配置等）
はCode Generationで確定する。unit-of-work.mdのパッケージ構成方針（ハイブリッド）に従い、
UserAccountComponent関連は`useraccount`、SecurityInfrastructureComponent関連は`security`、
AuditLogComponentは`audit`、NotificationComponentは`notification`パッケージ配下に配置する
想定。

## コントローラ（REST API）

| 論理コンポーネント | 役割 |
|---|---|
| AuthController | ログイン（`POST /api/auth/login`）、トークンリフレッシュ（`POST /api/auth/refresh`）、ログアウト（`POST /api/auth/logout`）。UserAccountComponent.authenticateを直接呼び出す（Spring SecurityのAuthenticationManagerを経由しない） |
| RegistrationController | 本登録完了（`POST /api/auth/register`） |
| PasswordController | パスワードリセット申請/実行（`POST /api/auth/password/reset-request`、`POST /api/auth/password/reset`）、パスワード変更（`PUT /api/account/password`） |
| AdminUserController | ユーザー招待/一覧/招待再送/ロール変更/無効化/再有効化（`/api/admin/users/**`）。ADMINロール限定 |

## UserAccountComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| UserAccountService | UserAccountComponentの実装。招待・本登録・ロール変更・無効化/再有効化・認証判定・ログイン失敗記録・パスワードリセット/変更・初期管理者作成のビジネスロジック |
| UserRepository | Userエンティティ（domain-entities.md）のJPAリポジトリ |
| PasswordResetTokenRepository | PasswordResetTokenエンティティのJPAリポジトリ |
| InitialAdminBootstrapper | アプリ起動時（`ApplicationRunner`等）に`ensureInitialAdmin`を呼び出す起動フック |

## SecurityInfrastructureComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| JwtTokenProvider | Nimbus JOSE+JWT（Question 4）を用いたJWTアクセストークンの発行（`issueAccessToken`）。sub/role/iat/expクレームを設定しHS256で署名する |
| JwtTokenValidatorImpl | Unit 1のプレースホルダ`NoopJwtTokenValidator`を置き換える実装。Nimbus JOSE+JWTでJWTの署名・有効期限を検証する。`@ConditionalOnMissingBean`により自動的に置き換わる |
| RefreshTokenService | リフレッシュトークンの発行・ローテーション・失効・再利用検知（`issueRefreshToken`/`rotateRefreshToken`/`revokeRefreshToken`/`detectReuseAndRevokeFamily`）。SHA-256でハッシュ化してRefreshTokenRepositoryに保存する |
| RefreshTokenRepository | RefreshTokenエンティティ（domain-entities.md）のJPAリポジトリ |
| PasswordHasher | BCryptPasswordEncoder（strength=10）のラッパー。`hashPassword`/`verifyPassword` |
| BreachedPasswordChecker | アプリケーション埋め込みの静的漏洩パスワードリスト（起動時にメモリへロード）と照合する`checkBreachedPassword`実装 |
| SecureTokenGenerator | SecureRandom 256bitトークン生成（Base64URL）。招待トークン・パスワードリセットトークンの生成に共用する |
| AuthCookieSupport | リフレッシュトークンをHttpOnly/Secure/SameSite=Strict Cookieとして`Set-Cookie`するヘルパー（発行時・失効時） |

## AuditLogComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| AuditLogService | AuditLogComponentの実装。`recordEvent`（作成のみ）。`listEvents`（Unit 6で閲覧API実装時に利用、Unit 2では内部メソッドとして用意するのみ） |
| AuditEventRepository | AuditEventエンティティ（単一テーブル+JSON詳細列、tech-stack-decisions.md）のJPAリポジトリ |

## NotificationComponent利用（Unit 1で確立済みの`java-mustache-processor`連携を利用）

| 論理コンポーネント | 役割 |
|---|---|
| NotificationService | 招待メール・パスワードリセットメールのテンプレート処理・送信。Unit 1のNotificationComponent実装を利用し、Unit 2ではメールテンプレート（招待/リセット、日英2言語）を追加する |

## 依存関係

```
AuthController → UserAccountService（authenticate/recordLoginFailure）
AuthController → JwtTokenProvider（issueAccessToken）
AuthController → RefreshTokenService（issueRefreshToken/rotateRefreshToken/revokeRefreshToken）
AuthController → AuthCookieSupport（Cookie発行・失効）
AuthController → AuditLogService（recordEvent）

RegistrationController → UserAccountService（completeRegistration）
RegistrationController → PasswordHasher（hashPassword、UserAccountService経由）
RegistrationController → BreachedPasswordChecker（UserAccountService経由）

PasswordController → UserAccountService（requestPasswordReset/resetPassword/changePassword）
AdminUserController → UserAccountService（inviteUser/resendInvitation/changeRole/deactivateUser/reactivateUser）

UserAccountService → SecureTokenGenerator（招待/リセットトークン生成）
UserAccountService → PasswordHasher（hashPassword/verifyPassword）
UserAccountService → BreachedPasswordChecker（checkBreachedPassword）
UserAccountService → NotificationService（招待/リセットメール送信）
UserAccountService → AuditLogService（recordEvent）
UserAccountService → RefreshTokenService（deactivateUser時の全RefreshToken失効）

RefreshTokenService → SecureTokenGenerator（トークン値生成）
JwtAuthenticationFilter（Unit 1確立済み） → JwtTokenValidatorImpl（アクセストークン検証）
```

循環依存はない。すべて一方向の依存である。
