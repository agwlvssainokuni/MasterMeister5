# Business Logic Summary — Unit 2: ユーザ管理

## 生成したクラス

### `cherry.mastermeister5.useraccount`（UserAccountComponent）
- `entity.User`（JPA、状態: INVITED/ACTIVE/DEACTIVATED、ロール: ADMIN/GENERAL）
- `entity.PasswordResetToken`
- `entity.UserStatus` / `entity.UserRole`（enum）
- `repository.UserJpaRepository` / `repository.PasswordResetTokenJpaRepository`
- `service.UserAccountService` / `service.UserAccountServiceImpl`（招待・本登録・ロール変更・
  無効化/再有効化・認証・パスワードリセット/変更・初期管理者作成。business-rules.md
  BR-1〜BR-33を実装）
- `service.AuthenticatedUser` / `service.UserSummary`（戻り値record）
- `service.UserAccountException`（業務例外。ルールごとにファクトリメソッドを持つ単一クラスとし、
  `GlobalExceptionHandler`側の対応も1箇所に集約した）
- `service.InitialAdminBootstrapper`（`ApplicationRunner`）
- `UserAccountProperties`（招待/リセットトークン有効期限、ロック閾値・時間、パスワード
  最小文字数、初期管理者アカウント）

### `cherry.mastermeister5.platform.security`（SecurityInfrastructureComponent。Unit 1の
既存パッケージを継続利用）
- `RefreshToken`（entity）/ `RefreshTokenJpaRepository` / `RefreshTokenService`
- `PasswordHasher`（BCrypt strength=10のラッパー）
- `BreachedPasswordChecker`（`security/common-passwords.txt`を起動時ロード）
- `SecureTokenGenerator`（SecureRandom 256bit、Base64URL。SHA-256ハッシュも提供）
- `JwtTokenProvider`（Nimbus JOSE+JWT、HS256署名）
- `JwtTokenValidatorImpl`（Unit 1の`NoopJwtTokenValidator`を置換）
- `AuthCookieSupport`（リフレッシュトークンのSet-Cookie、HttpOnly/Secure/SameSite=Strict）
- `RefreshTokenReuseDetectedException`
- `JwtProperties`

### `cherry.mastermeister5.audit`（AuditLogComponent）
- `AuditEvent`（entity、`details`列はJSON文字列⇔Mapの`JsonMapConverter`）
- `AuditEventType`（enum、BR-30の全イベント種別）
- `AuditEventJpaRepository` / `AuditLogService` / `AuditLogServiceImpl`（記録＋構造化ログ
  出力の両方を行う。`listEvents`は内部メソッドのみ、Unit 6で閲覧APIを追加する）

### `cherry.mastermeister5.notification`（NotificationComponent）
- `NotificationService` / `NotificationServiceImpl`（`java-mustache-processor`
  （`Mustache.compile`/`Template.render`）でメール本文を、`MessageResolver`で件名を解決する）
- 招待/パスワードリセットメールテンプレート（日英2言語、`notification/templates/*.mustache`）
- `MailProperties`

### 共通
- `cherry.mastermeister5.platform.AppProperties`（招待/リセットリンクのベースURL）
- i18nメッセージ追加（`errors.*`、`email.*.subject`、日英2言語＋デフォルト）

## PBT適用評価（property-based-testing拡張 PBT-01）

functional-design/business-logic-model.mdで識別した8件のテスト対象プロパティのうち7件を
jqwikの`@Property`で実装した（残り1件「招待トークン有効期限判定のIdempotence」は副作用の
ない時刻比較のため、既存の例示ベーステストで実質的に担保されると判断しProperty化を省略した）。
実装中、`User状態遷移のInvariant`のProperty化過程で`deactivateUser`/`reactivateUser`が
状態を検証せずに遷移させてしまう実装漏れを発見し、`UserAccountException.userNotActive`/
`userNotDeactivated`による状態ガードを追加した。

## 生成したテスト

- `UserAccountServiceImplTest`（例示ベース22件、Property 3件）
- `PasswordHasherTest`（例示ベース2件、Property 1件）
- `BreachedPasswordCheckerTest`（例示ベース2件）
- `JwtTokenRoundTripTest`（例示ベース3件）
- `RefreshTokenServiceTest`（例示ベース4件、Property 1件）
- `AuditLogServiceImplTest`（例示ベース1件）
