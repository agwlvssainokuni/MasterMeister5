# Code Generation Plan — Unit 2: ユーザ管理

## Unit Context

- **対応ストーリー**: US-1.0〜US-1.10（11件、stories.md Epic 1）
- **含まれるコンポーネント**: UserAccountComponent、SecurityInfrastructureComponent（Unit 1で
  骨組みのみ整備済み）、AuditLogComponent、NotificationComponent
- **依存Unit**: Unit 1（デザインシステム基盤。SecurityConfig/JwtAuthenticationFilter/
  GlobalExceptionHandler/MessageResolver/AppShell等の基盤を利用する）
- **後続Unitへの提供インターフェース**: JwtTokenValidatorImpl（Unit 3以降の保護対象APIが
  利用）、AuditLogService.recordEvent（Unit 3〜6が新たなイベント種別を記録する際に再利用）、
  RequireAuth/ロールガード（フロントエンド、以降のUnitの画面もこれを利用）
- **本UnitがオーナーとなるDBエンティティ**: `User`、`PasswordResetToken`、`RefreshToken`、
  `AuditEvent`
- **明示的なスコープ外**: AuditLogComponentの`listEvents`（監査ログ閲覧API・画面）はUnit 6で
  実装する。Unit 2では記録機構（`recordEvent`とそれを支えるデータモデル）のみを構築し、
  `listEvents`は内部メソッドとして用意するに留める

## 本計画で追加確定する技術選定（Design段階未決定分）

- **リポジトリ層のパターン**: Unit 1の`AppTheme`は単一レコードのupsert的な特殊性があり
  ポート/アダプタ（`AppThemeRepository`インタフェース＋`AppThemeRepositoryImpl`）を採用したが、
  Unit 2の`User`/`PasswordResetToken`/`RefreshToken`/`AuditEvent`は標準的なCRUD操作のみで
  あるため、Spring Data JPAリポジトリ（`UserJpaRepository`等）をサービス層から直接利用する
  （ポート/アダプタの追加抽象化は行わない）
- **AuditEventのJSON詳細列**: `details`列は`TEXT`型とし、`AttributeConverter`
  （`Map<String, Object>` ⇔ JSON文字列、Jacksonの`ObjectMapper`を利用）でエンティティ側は
  型付きMapとして扱う
- **Nimbus JOSE+JWTの具体的な利用方法**: HS256署名鍵（環境変数由来のバイト列）から
  `OctetSequenceKey`を構成し、`MACSigner`/`MACVerifier`で署名・検証する
- **既知漏洩パスワードリストの実体**: `src/main/resources/security/common-passwords.txt`
  として、頻出パスワードの一覧（数百〜数千件規模の代表的なもの）をリソースファイルで同梱する。
  起動時に1行1件読み込みメモリ上のSetに保持する。将来的な拡充・外部リスト差し替えが容易な
  ようインタフェース（`BreachedPasswordChecker`）越しに利用する
- **NotificationComponentの実装**: `java-mustache-processor`（`cherry-mustache-core`）の
  実際のAPIは、Step 2実行時に`libs/java-mustache-processor`のソース・ドキュメントを確認して
  から実装する（Unit 1での`make-you-chic-ui`統合時と同様の進め方）
- **フロントエンドのセッション復元**: リフレッシュトークンはHttpOnly Cookieのため
  JavaScriptから存在を確認できない。アプリ起動時（`AuthProvider`マウント時）に
  `POST /api/auth/refresh`を1回呼び出し、成功すればアクセストークンを復元、失敗（401）すれば
  未ログイン状態として扱う（ページリロード時に毎回この「サイレントリフレッシュ」を行う）

## REST APIエンドポイント一覧

| メソッド/パス | 認可 | 対応ストーリー |
|---|---|---|
| `POST /api/auth/login` | 公開 | US-1.0, US-1.7 |
| `POST /api/auth/refresh` | 公開（Cookie必須） | US-1.7 |
| `POST /api/auth/logout` | 公開（Cookie必須） | US-1.8 |
| `POST /api/auth/register` | 公開 | US-1.6 |
| `POST /api/auth/password/reset-request` | 公開 | US-1.9 |
| `POST /api/auth/password/reset` | 公開 | US-1.9 |
| `PUT /api/account/password` | 認証必須 | US-1.10 |
| `GET /api/admin/users` | ADMIN限定 | US-1.1〜1.5 |
| `POST /api/admin/users/invitations` | ADMIN限定 | US-1.1 |
| `POST /api/admin/users/{userId}/invitations/resend` | ADMIN限定 | US-1.2 |
| `PUT /api/admin/users/{userId}/role` | ADMIN限定 | US-1.3 |
| `POST /api/admin/users/{userId}/deactivate` | ADMIN限定 | US-1.4 |
| `POST /api/admin/users/{userId}/reactivate` | ADMIN限定 | US-1.5 |

## 実行ステップ

### Step 1: 依存関係・設定の追加
- [x] 1.1 `backend/build.gradle.kts`に`com.nimbusds:nimbus-jose-jwt`を追加（`spring-boot-starter-mail`も追加）
- [x] 1.2 `backend/src/main/resources/application.yml`にUnit 2の設定項目（トークン有効期限、
      アカウントロック閾値・時間、SMTP設定等）を追加
- [x] 1.3 `src/main/resources/security/common-passwords.txt`（既知漏洩/頻出パスワード
      リソース）を追加

### Step 2: Business Logic Generation
- [x] 2.1 `User`エンティティ（JPA、状態遷移: INVITED/ACTIVE/DEACTIVATED、ロール:
      ADMIN/GENERAL）
- [x] 2.2 `PasswordResetToken`エンティティ
- [x] 2.3 `RefreshToken`エンティティ
- [x] 2.4 `AuditEvent`エンティティ（JSON詳細列、`AttributeConverter`）
- [x] 2.5 `PasswordHasher`（BCryptPasswordEncoder strength=10のラッパー）
- [x] 2.6 `BreachedPasswordChecker`（起動時にcommon-passwords.txtをロード、完全一致照合）
- [x] 2.7 `SecureTokenGenerator`（SecureRandom 256bit、Base64URLエンコード）
- [x] 2.8 `JwtTokenProvider`（Nimbus JOSE+JWT、HS256署名、sub/role/iat/expクレーム）
- [x] 2.9 `JwtTokenValidatorImpl`（Nimbus JOSE+JWTで検証、`JwtTokenValidator`実装。
      `@ConditionalOnMissingBean`により`NoopJwtTokenValidator`を自動的に置き換える）
- [x] 2.10 `RefreshTokenService`（発行・ローテーション・失効・再利用検知・familyId単位
       一括失効。SHA-256でハッシュ化して保存）
- [x] 2.11 `AuthCookieSupport`（リフレッシュトークンのSet-Cookieヘルパー、HttpOnly/Secure/
       SameSite=Strict、失効時はMax-Age=0）
- [x] 2.12 `AuditLogService`（`recordEvent`: AuditEventテーブルへの記録＋構造化ログ出力。
       `listEvents`は内部メソッドのみ用意しUnit 6まで未使用）
- [x] 2.13 `NotificationService`（`java-mustache-processor`によるテンプレート処理、SMTP送信。
       招待メール・パスワードリセットメールのテンプレート（日英2言語）を追加）
- [x] 2.14 `UserAccountService`（`UserAccountComponent`実装: inviteUser/resendInvitation/
       completeRegistration/changeRole/deactivateUser/reactivateUser/authenticate/
       recordLoginFailure/requestPasswordReset/resetPassword/changePassword/
       ensureInitialAdmin。business-rules.md BR-1〜BR-33に従う。changeRole/deactivateUser/
       reactivateUserは監査ログの実行者記録のためactorUserId引数を追加した）
- [x] 2.15 `InitialAdminBootstrapper`（`ApplicationRunner`、起動時に`ensureInitialAdmin`を
       呼び出す）

### Step 3: Business Logic Unit Testing
- [x] 3.1 `UserAccountServiceImplTest`（招待重複判定、状態遷移、ロール変更、無効化時の
      RefreshToken一括失効、認証成功/失敗、アカウントロック、パスワードリセット/変更）
- [x] 3.2 `PasswordHasherTest`（BCryptハッシュ化・検証の正常系）
- [x] 3.3 `BreachedPasswordCheckerTest`（既知パスワードの検出・未知パスワードの通過）
- [x] 3.4 `JwtTokenRoundTripTest`（`JwtTokenProvider`/`JwtTokenValidatorImpl`のラウンドトリップ）
- [x] 3.5 `RefreshTokenServiceTest`（ローテーション、再利用検知によるファミリ一括失効）
- [x] 3.6 `AuditLogServiceImplTest`（recordEvent呼び出しでAuditEvent保存が行われることを検証）
- [x] 3.7 property-based-testing拡張（jqwik）: functional-design/business-logic-model.mdの
      「テスト対象プロパティ」8件を実装した
      - パスワードハッシュ検証のInvariant（PasswordHasherTest）
      - User状態遷移のInvariant（UserAccountServiceImplTest、不正遷移が常に拒否される。
        実装漏れを発見し`deactivateUser`/`reactivateUser`に状態ガードを追加した）
      - ログイン失敗カウントのInvariant（UserAccountServiceImplTest、0以上、成功時リセット）
      - アカウントロックのIdempotence（UserAccountServiceImplTest、ロック中の再試行で
        lockedUntilが延長されない）
      - リフレッシュトークンローテーションのInvariant（RefreshTokenServiceTest、familyId
        引き継ぎ・旧トークン失効）
      - 再利用検知ファミリ失効のIdempotence（RefreshTokenServiceTest）
      - パスワードリセットトークン単一有効性のInvariant（UserAccountServiceImplTest）
      - 招待トークン有効期限判定のIdempotence（副作用のない時刻比較のため、既存の
        `completeRegistrationRejectsAnExpiredToken`テストで実質的に担保。独立した
        Property化は行わなかった）

### Step 4: Business Logic Summary
- [x] 4.1 `aidlc-docs/construction/unit2-user-management/code/business-logic-summary.md`を
      生成する

### Step 5: API Layer Generation
- [ ] 5.1 `AuthController`（login/refresh/logout。`UserAccountService`/`JwtTokenProvider`/
      `RefreshTokenService`/`AuthCookieSupport`を直接呼び出す。AuthenticationManagerは
      経由しない）
- [ ] 5.2 `RegistrationController`（register）
- [ ] 5.3 `PasswordController`（password/reset-request、password/reset、account/password）
- [ ] 5.4 `AdminUserController`（ユーザー一覧・招待・招待再送・ロール変更・無効化・再有効化）
- [ ] 5.5 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与）
- [ ] 5.6 `SecurityConfig`更新（既存ファイルを修正）: `permitAll()`対象に
      `/api/auth/login`、`/api/auth/refresh`、`/api/auth/logout`、`/api/auth/register`、
      `/api/auth/password/**`を追加。`/api/admin/**`に`hasRole("ADMIN")`を追加。それ以外は
      既存の`anyRequest().authenticated()`を維持

### Step 6: API Layer Unit Testing
- [ ] 6.1 `AuthControllerTest`（ログイン成功/失敗/ロック中、リフレッシュ成功/再利用検知、
      ログアウト）
- [ ] 6.2 `RegistrationControllerTest`（正常登録、期限切れトークン、パスワードポリシー違反）
- [ ] 6.3 `PasswordControllerTest`（リセット申請は常に同一応答、リセット実行、パスワード変更）
- [ ] 6.4 `AdminUserControllerTest`（ADMIN限定であることの検証を含む、招待重複エラー）
- [ ] 6.5 `SecurityConfig`のpermitAll/hasRole設定を検証するテスト（未認証で公開エンドポイントに
      アクセスできること、管理APIがGENERALロールで403になること）

### Step 7: API Layer Summary
- [ ] 7.1 `aidlc-docs/construction/unit2-user-management/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [ ] 8.1 `UserJpaRepository`（メールアドレスでの検索を含む）
- [ ] 8.2 `PasswordResetTokenJpaRepository`
- [ ] 8.3 `RefreshTokenJpaRepository`（familyId単位の一括更新を含む）
- [ ] 8.4 `AuditEventJpaRepository`

### Step 9: Repository Layer Unit Testing
- [ ] 9.1 各リポジトリの`@DataJpaTest`（一意制約、検索クエリ、AuditEventのJSON詳細列の
      ラウンドトリップ）

### Step 10: Repository Layer Summary
- [ ] 10.1 `aidlc-docs/construction/unit2-user-management/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [ ] 11.1 `AuthContext`（アクセストークン・現在ユーザー情報をメモリ保持。マウント時に
       サイレントリフレッシュを試行）
- [ ] 11.2 `RequireAuth`（未認証時`/login`へリダイレクト）、ロールガード（ADMIN限定ルート用）
- [ ] 11.3 APIクライアント関数（`api/auth.ts`、`api/admin/users.ts`。fetchラッパーに
       アクセストークンのAuthorizationヘッダ付与、401時の自動リフレッシュ再試行を含む）
- [ ] 11.4 `LoginScreen`
- [ ] 11.5 `RegistrationCompletionScreen`
- [ ] 11.6 `ForgotPasswordScreen`
- [ ] 11.7 `ResetPasswordScreen`
- [ ] 11.8 `AdminUserListScreen`
- [ ] 11.9 `ChangePasswordScreen`
- [ ] 11.10 `App.tsx`ルーティング更新（レイアウトルート外に認証前画面、`RequireAuth`配下に
       認証後画面を追加）、`AppLayout`更新（ADMIN限定navItem「ユーザー管理」、ユーザーメニューに
       「パスワード変更」「ログアウト」を追加）
- [ ] 11.11 i18nメッセージ追加（ja/en、Unit 2の全画面文言・エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [ ] 12.1 `AuthContext`/`RequireAuth`のテスト（未ログイン時リダイレクト、ロールガード）
- [ ] 12.2 `LoginScreen.test.tsx`（成功、エラー表示）
- [ ] 12.3 `RegistrationCompletionScreen.test.tsx`（バリデーション、期限切れ表示）
- [ ] 12.4 `ForgotPasswordScreen.test.tsx`/`ResetPasswordScreen.test.tsx`
- [ ] 12.5 `AdminUserListScreen.test.tsx`（招待モーダル、各操作ボタンのAPI呼び出し）
- [ ] 12.6 `ChangePasswordScreen.test.tsx`

### Step 13: Frontend Components Summary
- [ ] 13.1 `aidlc-docs/construction/unit2-user-management/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [ ] 14.1 `V2__create_user.sql`
- [ ] 14.2 `V3__create_password_reset_token.sql`
- [ ] 14.3 `V4__create_refresh_token.sql`
- [ ] 14.4 `V5__create_audit_event.sql`

### Step 15: Documentation Generation
- [ ] 15.1 `README.md`更新（初期管理者アカウントの環境変数、ログインが必要になった旨）

### Step 16: Deployment Artifacts Generation
- [ ] 16.1 `.env.example`更新（JWT署名鍵、初期管理者メールアドレス/パスワード、招待/
       リセットトークン有効期限、アカウントロック閾値・時間、アクセス/リフレッシュトークン
       有効期限）

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
