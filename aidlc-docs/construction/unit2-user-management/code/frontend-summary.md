# Frontend Summary — Unit 2: ユーザ管理

## 生成したモジュール

### 認証状態
- `src/auth/AuthContext.tsx`（`AuthProvider`/`useAuth`）: アクセストークンはReact stateで
  メモリ保持のみ（nfr-design-plan.md Question 3）。マウント時に1回`POST /api/auth/refresh`を
  呼び出すサイレントリフレッシュでセッションを復元する
- `src/auth/RequireAuth.tsx`: 未認証時`/login`へリダイレクト。`role`prop指定時はロールも
  検証し、不一致なら`/`へリダイレクト

### APIクライアント
- `src/api/auth.ts`: `login`/`refresh`/`logout`/`register`/`requestPasswordReset`/
  `resetPassword`/`changePassword`、およびアクセストークンをAuthorizationヘッダに付与し
  401時に1回リフレッシュ再試行する`authenticatedFetch`ヘルパー
- `src/api/adminUsers.ts`: `listUsers`/`inviteUser`/`resendInvitation`/`changeRole`/
  `deactivateUser`/`reactivateUser`（`authenticatedFetch`ベース）

### 画面
- `src/routes/LoginScreen.tsx`
- `src/routes/RegistrationCompletionScreen.tsx`
- `src/routes/ForgotPasswordScreen.tsx`
- `src/routes/ResetPasswordScreen.tsx`
- `src/routes/admin/AdminUserListScreen.tsx`（招待モーダル、ロール変更・招待再送・無効化・
  再有効化を1画面に統合。make-you-chic-uiの`Table`は外部ページング等Unit 2では不要な機能を
  持つため採用せず、素のHTMLテーブルを使用した）
- `src/routes/ChangePasswordScreen.tsx`

### ルーティング・レイアウト更新（既存ファイル修正）
- `src/App.tsx`: 認証前4画面をレイアウトルート外に、`RequireAuth`配下に`/`
  `/settings/password`を、さらに`RequireAuth role="ADMIN"`配下に`/users`を追加
- `src/layout/AppLayout.tsx`: `useAuth`からロールを参照しADMIN限定で`navItems`に
  「ユーザー管理」を追加。AppShellの`userMenuItems`に「パスワード変更」「ログアウト」を追加
- `src/main.tsx`: `AuthProvider`で`<App />`をラップ

### i18n
`src/i18n/locales/{ja,en}/common.json`に`auth.*`/`admin.users.*`/`nav.*`/`common.loading`/
`errors.invitation_token_expired`を追加

## 生成したテスト

- `AuthContext.test.tsx`（2件）、`RequireAuth.test.tsx`（3件）
- `LoginScreen.test.tsx`（2件）、`RegistrationCompletionScreen.test.tsx`（3件）、
  `ForgotPasswordScreen.test.tsx`（1件）、`ResetPasswordScreen.test.tsx`（2件）、
  `ChangePasswordScreen.test.tsx`（2件）、`AdminUserListScreen.test.tsx`（2件）
- 既存の`AppLayout.test.tsx`を`AuthProvider`必須化に対応させ、ADMIN限定navItem表示の
  検証を追加した
