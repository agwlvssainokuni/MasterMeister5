# Unit 2: ユーザ管理 - Frontend Components

Unit 1で整備したAppShell（`make-you-chic-ui`）・レイアウトルートパターン
（`libs/make-you-chic-ui/docs/integration-guide.md`）を踏襲する。認証前画面（ログイン・
本登録完了・パスワードリセット申請/実行）は`<AppLayout>`の外側、認証後画面は
`<AppLayout>`配下に配置する。トークンの具体的な保存方式（Cookie/メモリ等）はNFR Design/
Infrastructure Designで確定する。

## ルーティング構成（追加分）

| パス | 画面 | レイアウト | 認証要否 |
|---|---|---|---|
| `/login` | LoginScreen | レイアウトルート外 | 不要 |
| `/register/:token` | RegistrationCompletionScreen | レイアウトルート外 | 不要 |
| `/password/forgot` | ForgotPasswordScreen | レイアウトルート外 | 不要 |
| `/password/reset/:token` | ResetPasswordScreen | レイアウトルート外 | 不要 |
| `/users` | AdminUserListScreen | AppLayout配下 | 要（ADMINロールのみ） |
| `/settings/password` | ChangePasswordScreen | AppLayout配下 | 要 |

未認証で認証必須ルートにアクセスした場合は`/login`へリダイレクトする
（`RequireAuth`ラッパーコンポーネントで実現）。

## 1. LoginScreen（US-1.0, US-1.7）

- **構造**: メールアドレス入力欄、パスワード入力欄、ログインボタン、
  「パスワードを忘れた場合」リンク（`/password/forgot`へ）
- **状態**: `email`, `password`, `submitting`, `errorMessage`
- **バリデーション**: メールアドレス形式、パスワード非空（詳細なポリシー検証はサーバ側、
  クライアント側は必須入力チェックのみ）
- **操作フロー**: 送信 → `POST /api/auth/login` → 成功時アクセス/リフレッシュトークンを
  受け取りAuthContextへ格納、ホーム（`/`）へ遷移 → 失敗時（認証エラー／ロック中）は
  一般的なエラーメッセージを表示（BR-14と同様、ロック中か認証情報誤りかを区別しない
  メッセージとする。列挙対策の思想を画面表示にも適用）
- **API**: `POST /api/auth/login`（Email, Password → AccessToken, RefreshToken）

## 2. RegistrationCompletionScreen（US-1.6）

- **構造**: URLパラメータ`:token`（招待トークン）、氏名入力欄、パスワード入力欄、
  パスワード確認入力欄、登録完了ボタン
- **状態**: `name`, `password`, `passwordConfirm`, `submitting`, `errorMessage`,
  `tokenExpiredError`（true時は「招待の有効期限が切れています。管理者に再送を依頼して
  ください」という案内を表示しフォームを無効化する）
- **バリデーション**: 氏名必須、パスワード最小8文字、パスワードとパスワード確認の一致
- **操作フロー**: 送信 → `POST /api/auth/register`（token, name, password）→ 成功時は
  ログイン画面へ誘導するメッセージを表示（自動ログインはせず、明示的にログイン操作を
  促す）→ 失敗時（期限切れ／パスワードポリシー違反／漏洩パスワード）はエラー内容を表示
- **API**: `POST /api/auth/register`（InvitationToken, UserProfile, Password → UserId）

## 3. ForgotPasswordScreen（US-1.9前半）

- **構造**: メールアドレス入力欄、送信ボタン
- **状態**: `email`, `submitted`（true時は入力フォームの代わりに「メールを送信しました」
  という固定メッセージを表示。存在確認は行わない）
- **バリデーション**: メールアドレス形式
- **操作フロー**: 送信 → `POST /api/auth/password/reset-request` → レスポンス内容に
  かかわらず常に同じ完了メッセージを表示する（BR-23）
- **API**: `POST /api/auth/password/reset-request`（Email → 常に成功レスポンス）

## 4. ResetPasswordScreen（US-1.9後半）

- **構造**: URLパラメータ`:token`（リセットトークン）、新パスワード入力欄、パスワード確認
  入力欄、設定ボタン
- **状態**: `password`, `passwordConfirm`, `submitting`, `errorMessage`,
  `tokenExpiredError`
- **バリデーション**: パスワード最小8文字、パスワードとパスワード確認の一致
- **操作フロー**: 送信 → `POST /api/auth/password/reset`（token, newPassword）→ 成功時は
  ログイン画面へ誘導 → 失敗時（期限切れ／使用済み／ポリシー違反）はエラー内容を表示
- **API**: `POST /api/auth/password/reset`（ResetToken, NewPassword → void）

## 5. ログアウト（US-1.8）

独立画面ではなく、AppShellのユーザーメニュー（Topbar）に「ログアウト」操作を追加する
（既存のテーマ切り替え等と同じユーザーメニュー領域）。

- **操作フロー**: クリック → `POST /api/auth/logout` → AuthContextのトークンをクリアし
  `/login`へ遷移
- **API**: `POST /api/auth/logout`（RefreshToken → void）

## 6. AdminUserListScreen（US-1.1〜1.5）

- **構造**: ユーザー一覧テーブル（メールアドレス／氏名／ロール／状態／招待日時等）、
  「ユーザーを招待」ボタン（モーダルでメールアドレス・ロールを入力）、各行に
  「招待再送」（INVITED状態のみ活性）／「ロール変更」／「無効化」（ACTIVE状態のみ活性）／
  「再有効化」（DEACTIVATED状態のみ活性）操作
- **状態**: `users`（一覧）、`loading`、招待モーダルの`inviteEmail`/`inviteRole`/
  `inviteSubmitting`/`inviteErrorMessage`
- **バリデーション（招待モーダル）**: メールアドレス形式、ロール選択必須
- **操作フロー**:
  - 招待: モーダル送信 → `POST /api/admin/users/invitations` → 成功時一覧を再取得しモーダルを
    閉じる → 重複エラー時（登録済み／招待済み）はモーダル内にエラーメッセージを表示する
    （BR-2, BR-3に基づき、管理者には具体的な理由を明示する）
  - 招待再送: `POST /api/admin/users/{userId}/invitations/resend`
  - ロール変更: `PUT /api/admin/users/{userId}/role`
  - 無効化/再有効化: `POST /api/admin/users/{userId}/deactivate` /
    `POST /api/admin/users/{userId}/reactivate`
  - いずれの操作後も一覧を再取得して最新状態を反映する
- **API**: component-methods.mdの`inviteUser`/`resendInvitation`/`changeRole`/
  `deactivateUser`/`reactivateUser`に対応するAPI群
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth`に加えロールガードを適用）

## 7. ChangePasswordScreen（US-1.10）

- **構造**: 現在のパスワード入力欄、新パスワード入力欄、新パスワード確認入力欄、変更ボタン
- **状態**: `currentPassword`, `newPassword`, `newPasswordConfirm`, `submitting`,
  `errorMessage`, `successMessage`
- **バリデーション**: 新パスワード最小8文字、新パスワードと確認の一致
- **操作フロー**: 送信 → `PUT /api/account/password`（currentPassword, newPassword）→
  成功時に完了メッセージを表示（画面遷移はしない）→ 失敗時（現パスワード誤り／ポリシー
  違反）はエラーメッセージを表示
- **API**: `PUT /api/account/password`（CurrentPassword, NewPassword → void）

## AppLayout（既存）への追加

`navItems`（`frontend/src/layout/AppLayout.tsx`）にADMINロール限定で「ユーザー管理」
（`/users`）を追加し、ユーザーメニューに「パスワード変更」（`/settings/password`）と
「ログアウト」を追加する。ロールに応じたnavItemsの出し分けにはAuthContextが保持する
現在ユーザーのロール情報を用いる。
