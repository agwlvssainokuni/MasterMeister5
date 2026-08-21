# Repository Layer Summary — Unit 2: ユーザ管理

## 生成したリポジトリ

nfr-requirements.mdの決定（Spring Data JPAをサービス層から直接利用、ポート/アダプタの
追加抽象化なし）に従い、いずれも`JpaRepository`を継承するインタフェースのみを生成した。

- `cherry.mastermeister5.useraccount.repository.UserJpaRepository`（`findByEmail`、
  `findByInvitationTokenHash`、`findAllByOrderByCreatedAtAsc`）
- `cherry.mastermeister5.useraccount.repository.PasswordResetTokenJpaRepository`
  （`findByTokenHash`、`findAllByUserIdAndUsedAtIsNull`）
- `cherry.mastermeister5.platform.security.RefreshTokenJpaRepository`（`findByTokenHash`、
  `revokeFamily`/`revokeAllForUser`は`@Modifying`の一括UPDATE）
- `cherry.mastermeister5.audit.AuditEventJpaRepository`（`findAllByOrderByOccurredAtDesc`）

## DBマイグレーション（Step 14、本Stepの前提として先行生成）

- `V2__create_user.sql`（`app_user`テーブル）
- `V3__create_password_reset_token.sql`
- `V4__create_refresh_token.sql`
- `V5__create_audit_event.sql`（`details`列は`TEXT`型、`JsonMapConverter`でMap⇔JSON変換）

Unit 1からのバージョン連番を継続した（nfr-design-plan.md Question 1）。

## 生成したテスト

- `UserJpaRepositoryTest`（3件。メールアドレス一意制約違反の検証を含む）
- `PasswordResetTokenJpaRepositoryTest`（2件）
- `RefreshTokenJpaRepositoryTest`（3件。`revokeFamily`/`revokeAllForUser`が対象外の行に
  影響しないことを検証）
- `AuditEventJpaRepositoryTest`（2件。JSON詳細列のラウンドトリップを含む）

いずれもUnit 1と同様`@DataJpaTest` + `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
を使用した。
