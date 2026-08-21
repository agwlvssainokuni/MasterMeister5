# API Layer Summary — Unit 2: ユーザ管理

## 生成したコントローラ

| コントローラ | エンドポイント | 認可 |
|---|---|---|
| `AuthController` | `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` | 公開 |
| `RegistrationController` | `POST /api/auth/register` | 公開 |
| `PasswordController` | `POST /api/auth/password/reset-request`, `POST /api/auth/password/reset`, `PUT /api/account/password` | 前2つ公開／`account/password`は認証必須 |
| `AdminUserController` | `GET/POST/PUT /api/admin/users/**` | ADMIN限定（`SecurityConfig`のパスマッチャ＋`@PreAuthorize`の多層防御） |

## SecurityConfig更新

`authorizeHttpRequests`に以下を追加した（既存の`anyRequest().authenticated()`は維持）:
- `permitAll()`: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`,
  `/api/auth/register`, `/api/auth/password/**`
- `hasRole("ADMIN")`: `/api/admin/**`

`GlobalExceptionHandler`に`UserAccountException`用のハンドラを追加し、業務ルール違反
（重複招待、トークン期限切れ、パスワードポリシー違反、認証失敗等）を統一エラーレスポンス
構造で返すようにした。

## トークンの伝達経路（nfr-design-patterns.md）

- アクセストークン: レスポンスボディ（`LoginResponse`/`RefreshResponse`）
- リフレッシュトークン: `AuthCookieSupport`経由でHttpOnly/Secure/SameSite=StrictのCookie
  （`/api/auth`パス限定）
- `AuthController.refresh`は再利用検知（`RefreshTokenReuseDetectedException`）時に
  Cookieを即座に失効させ、`AuditLogService`に`REFRESH_TOKEN_REUSE_DETECTED`を記録する

## 生成したDTO

`LoginRequest`/`LoginResponse`/`RefreshResponse`/`AuthenticatedUserResponse`/
`RegisterRequest`/`PasswordResetRequestRequest`/`PasswordResetRequest`/
`ChangePasswordRequest`/`InviteUserRequest`/`ChangeRoleRequest`/`UserSummaryResponse`
（すべてrecord、SECURITY-05の入力検証アノテーション付与）

## 生成したテスト

- `AuthControllerTest`（5件）、`RegistrationControllerTest`（3件）、
  `PasswordControllerTest`（4件）、`AdminUserControllerTest`（5件）
- ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能なため
  （Unit 1の`AppThemeServiceImplTest`と同じ制約）、Build and Testの結合テストに委ねる
