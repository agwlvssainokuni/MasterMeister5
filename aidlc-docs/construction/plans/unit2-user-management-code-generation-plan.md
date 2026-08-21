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
- [x] 5.1 `AuthController`（login/refresh/logout。`UserAccountService`/`JwtTokenProvider`/
      `RefreshTokenService`/`AuthCookieSupport`を直接呼び出す。AuthenticationManagerは
      経由しない）
- [x] 5.2 `RegistrationController`（register）
- [x] 5.3 `PasswordController`（password/reset-request、password/reset、account/password）
- [x] 5.4 `AdminUserController`（ユーザー一覧・招待・招待再送・ロール変更・無効化・再有効化）
- [x] 5.5 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与）
- [x] 5.6 `SecurityConfig`更新（既存ファイルを修正）: `permitAll()`対象に
      `/api/auth/login`、`/api/auth/refresh`、`/api/auth/logout`、`/api/auth/register`、
      `/api/auth/password/**`を追加。`/api/admin/**`に`hasRole("ADMIN")`を追加。それ以外は
      既存の`anyRequest().authenticated()`を維持。`UserAccountException`用の
      `GlobalExceptionHandler`ハンドラも追加した

### Step 6: API Layer Unit Testing
- [x] 6.1 `AuthControllerTest`（ログイン成功/失敗、リフレッシュのCookie未提示/再利用検知、
      ログアウト）
- [x] 6.2 `RegistrationControllerTest`（正常登録、期限切れトークン、入力必須違反）
- [x] 6.3 `PasswordControllerTest`（リセット申請は常に同一応答、リセット実行、パスワード変更・
      現パスワード不一致）
- [x] 6.4 `AdminUserControllerTest`（一覧・招待・ロール変更・無効化がactorUserIdとして
      認証済み管理者IDを渡すことを検証、招待重複エラー）
- [x] 6.5 `SecurityConfig`のpermitAll/hasRole設定・ADMIN限定の実効性検証は`@WebMvcTest`
      スライスでは`@PreAuthorize`のAOPプロキシが読み込まれないため不可（Unit 1の
      `AppThemeServiceImplTest`と同じ制約）。Build and Testステージの結合テストで検証する

### Step 7: API Layer Summary
- [x] 7.1 `aidlc-docs/construction/unit2-user-management/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [x] 8.1 `UserJpaRepository`（メールアドレスでの検索を含む。Step 2でサービス層と合わせて
      先行生成済み）
- [x] 8.2 `PasswordResetTokenJpaRepository`（同上）
- [x] 8.3 `RefreshTokenJpaRepository`（familyId単位の一括更新を含む。同上）
- [x] 8.4 `AuditEventJpaRepository`（同上）

### Step 9: Repository Layer Unit Testing
- [x] 9.1 各リポジトリの`@DataJpaTest`（一意制約、検索クエリ、AuditEventのJSON詳細列の
      ラウンドトリップ）。Unit 1と同様、テストの前提として`Step 14: Database Migration
      Scripts`をここで先行生成した

### Step 10: Repository Layer Summary
- [x] 10.1 `aidlc-docs/construction/unit2-user-management/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [x] 11.1 `AuthContext`（アクセストークン・現在ユーザー情報をメモリ保持。マウント時に
       サイレントリフレッシュを試行）
- [x] 11.2 `RequireAuth`（未認証時`/login`へリダイレクト）、ロールガード（ADMIN限定ルート用）
- [x] 11.3 APIクライアント関数（`api/auth.ts`、`api/adminUsers.ts`。fetchラッパーに
       アクセストークンのAuthorizationヘッダ付与、401時の自動リフレッシュ再試行を含む）
- [x] 11.4 `LoginScreen`
- [x] 11.5 `RegistrationCompletionScreen`
- [x] 11.6 `ForgotPasswordScreen`
- [x] 11.7 `ResetPasswordScreen`
- [x] 11.8 `AdminUserListScreen`（make-you-chic-uiのTableコンポーネントは外部ページング等
       Unit 2では不要な機能を持つため採用せず、素のHTMLテーブルとした）
- [x] 11.9 `ChangePasswordScreen`
- [x] 11.10 `App.tsx`ルーティング更新（レイアウトルート外に認証前画面、`RequireAuth`配下に
       認証後画面を追加）、`AppLayout`更新（ADMIN限定navItem「ユーザー管理」、AppShellの
       `userMenuItems`に「パスワード変更」「ログアウト」を追加）、`main.tsx`に`AuthProvider`を追加
- [x] 11.11 i18nメッセージ追加（ja/en、Unit 2の全画面文言・エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [x] 12.1 `AuthContext.test.tsx`/`RequireAuth.test.tsx`（未ログイン時リダイレクト、
       ロールガード）。既存の`AppLayout.test.tsx`も`AuthProvider`必須化に伴い更新した
- [x] 12.2 `LoginScreen.test.tsx`（成功、エラー表示）
- [x] 12.3 `RegistrationCompletionScreen.test.tsx`（バリデーション、期限切れ表示）
- [x] 12.4 `ForgotPasswordScreen.test.tsx`/`ResetPasswordScreen.test.tsx`
- [x] 12.5 `AdminUserListScreen.test.tsx`（招待モーダル、各操作ボタンのAPI呼び出し）
- [x] 12.6 `ChangePasswordScreen.test.tsx`

### Step 13: Frontend Components Summary
- [x] 13.1 `aidlc-docs/construction/unit2-user-management/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [x] 14.1 `V2__create_user.sql`（Step 9で先行生成済み）
- [x] 14.2 `V3__create_password_reset_token.sql`（同上）
- [x] 14.3 `V4__create_refresh_token.sql`（同上）
- [x] 14.4 `V5__create_audit_event.sql`（同上）

### Step 15: Documentation Generation
- [x] 15.1 `README.md`更新（初期管理者アカウントの環境変数、Swagger UIの認証必須範囲の記述更新）

### Step 16: Deployment Artifacts Generation
- [x] 16.1 `.env.example`更新（JWT署名鍵、初期管理者メールアドレス/パスワード、招待/
       リセットトークン有効期限、アカウントロック閾値・時間、アクセス/リフレッシュトークン
       有効期限、SMTP設定、アプリベースURL）

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。

## 完了後の実動作検証（Unit 1と同様、コード生成直後に前倒しで実施）

16ステップ完了後、`./gradlew :backend:test`を実行し以下13件の失敗を検出・修正した:

- **`BreachedPasswordChecker.loadList()`がpackage-private**: 別パッケージのテストから
  直接呼び出せなかったため`public`に変更
- **`org.mockito.eq`は存在しないクラス**: `import static org.mockito.ArgumentMatchers.eq`
  を追加し`eq(...)`に修正（`RefreshTokenServiceTest`）
- **PBTプロパティがBCryptの72byte上限を超過**: jqwikが生成した長いマルチバイト文字列が
  BCryptの入力上限（72byte）を超えIllegalArgumentExceptionになった。`@AlphaChars`で
  生成文字種をASCII英字に制限（`PasswordHasherTest`）
- **`@Modifying`一括UPDATEとJPA第一階層キャッシュの不整合**: `revokeFamily`/
  `revokeAllForUser`実行後に同一永続化コンテキスト内で`findById`すると、DBは更新済みでも
  キャッシュされた古いエンティティが返る（`AssertionError: Expecting actual not to be null`）。
  `@Modifying(clearAutomatically = true)`を追加して修正（`RefreshTokenJpaRepository`）
- **`@ExtendWith(MockitoExtension.class)`+`@Mock`とjqwikの`@Property`の混在は動作しない**:
  `@Property`メソッドはjqwikエンジンで実行されJUnit Jupiter拡張（MockitoExtension）が
  適用されないため、`@Mock`フィールドがnullのままNullPointerExceptionになった
  （`UserAccountServiceImplTest`）。`@Mock`/`@BeforeEach`をやめ、モックとサービスを
  フィールド初期化子（`Mockito.mock(...)`を直接呼び出し）に置き換え、両エンジンで
  確実に初期化されるようにした
- **`@WebMvcTest`+`addFilters=false`では`Authentication`引数が解決されない**:
  `SecurityContextHolder`に直接設定してもコントローラの`Authentication authentication`
  引数は`HttpServletRequest.getUserPrincipal()`経由で解決され、これはSpring Securityの
  フィルタ（無効化済み）が本来ブリッジする。`MockMvcRequestBuilders`の`.principal(...)`で
  直接設定するよう修正（`AdminUserControllerTest`、`PasswordControllerTest`）

修正後、`./gradlew :backend:test`（92件）、`npm test`（frontend、21件）、
`npx tsc --noEmit`がすべて成功することを確認した。

### 追加検証: `bootWar`がUnit 2のフロントエンド変更を反映するか

`./gradlew :backend:bootWar`を実行したところ`npmBuildFrontend`が`UP-TO-DATE`と判定され
Viteビルドがスキップされた。生成物（`static/assets/*.js`）にUnit 2の文言が含まれていない
ことを確認し、`npmBuildFrontend`タスクに入力（`inputs`）が一切宣言されていないため
Gradleが変更を検知できず、成果物が存在する限り永久にUP-TO-DATE扱いになる不具合と特定した。
`frontend/src`・設定ファイル・`make-you-chic-ui`のビルド成果物を`inputs`として明示的に
宣言し修正した。`--rerun`で再ビルドし、生成物にUnit 2のテキスト
（`admin-users-invite`、`invitation_token_expired`等）が含まれることを確認。さらに
再度`bootWar`を実行し、変更がない状態では正しく`UP-TO-DATE`になることも確認した。
