# Security Test Instructions

対象: MasterMeister5（Unit 1〜6、全機能完了時点）。security-baseline拡張
（SECURITY-01〜15）が全Unitに適用されている（管理者MFAのみ文書化された適用除外、
requirements.md 5章）。

## 依存関係の脆弱性スキャン（SECURITY-10）

### 現状の実装

- `dependencyLocking`（全Gradleモジュール共通のロックファイル、Unit 1確立済み）
  によりバージョンを固定している
- `.env.example`に将来のDependabot設定に関するコメントは無いが、
  tech-stack-decisions.md（Unit 2）では「Gradle dependencyLocking + GitHub
  Dependabot」を依存関係管理方針として決定していた

### 検証結果（ギャップ、2026-08-22実施）

リポジトリ全体を確認したが、`.github/dependabot.yml`（またはSnyk/OWASP Dependency
-Check等の代替スキャナ設定）は**存在しない**。SECURITY-10は「脆弱性スキャナの設定
必須」を要求しており、現時点でこの要件を満たしていない。

**手動での代替確認手順**（自動スキャナ導入までの暫定手段）:

```bash
./gradlew dependencies --configuration runtimeClasspath > /tmp/deps.txt
# 出力を手動で既知の脆弱性データベース（OSV.dev等）と照合する
cd frontend && npm audit
```

npm側は`npm audit`で既知の脆弱性を確認できるが、Gradle/Java側には同等の
組み込みコマンドがない。**このギャップの解消（`.github/dependabot.yml`の追加、
またはOWASP Dependency-Check Gradleプラグインの導入）は本Build and Testの
スコープ外であり、対応するかどうかはユーザーの判断に委ねる。**

## 認証・認可のテスト（SECURITY-08・SECURITY-12）

### 自動テストで検証済みの項目

- JWTアクセストークン/リフレッシュトークンの発行・検証・ローテーション・
  再利用検知（Unit 2、`SecurityInfrastructureComponent`関連のテスト群）
- アカウントロックアウト（`MM5_MAX_FAILED_LOGIN_ATTEMPTS`超過、Unit 2）
- ADMIN限定操作の`@PreAuthorize`アノテーション付与（コントローラ単体テストで
  アノテーション自体は確認できるが、`@WebMvcTest`スライスでは実際の403応答は
  検証できない制約がある。Unit 2〜6共通の既知の制約）
- テーブル/カラム単位の実効権限（NONE/READ/UPDATE、Unit 4の
  `AccessControlServiceImplTest`）

### 手動での確認が必要な項目

```bash
# ADMIN限定APIを一般ユーザのトークンで呼び出し、403が返ることを確認する
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/admin/audit-events \
  -H "Authorization: Bearer <一般ユーザのアクセストークン>"
```

## 入力検証のテスト（SECURITY-05）

- Bean Validation（`@NotBlank`/`@NotNull`等）による基本検証は各DTOで実装済み、
  `MethodArgumentNotValidException`は`GlobalExceptionHandler`で汎用エラーに
  変換される（SECURITY-15、スタックトレース非露出）
- SQLインジェクション対策: Unit 5・6のいずれも識別子（テーブル名・カラム名・
  スキーマ名）は許可文字パターン（`^[A-Za-z0-9._-]+$`）で検証し、値は
  バインドパラメータ化する。手入力WHERE句・SQL全文はブロックリスト
  （`;`/`--`/`/*`検出）で検証する（`MasterMaintenanceServiceImplTest`/
  `QueryServiceImplTest`のProperty-basedテストで任意の混入パターンに対する
  拒否を検証済み）

## セキュリティヘッダー（SECURITY-04）

```bash
curl -sI http://localhost:8080/ | grep -i "content-security-policy\|strict-transport-security\|x-content-type-options\|x-frame-options"
```

`SecurityConfig`でCSP・HSTS・X-Content-Type-Options・X-Frame-Optionsを設定済み
（Unit 2）。

## 監査ログの改ざん防止（SECURITY-13・SECURITY-14）

- `AuditLogService`に更新・削除メソッドが一切存在しないこと（インタフェースの
  静的な確認、`recordEvent`/`listEvents`のみ）
- Unit 6でも監査ログ閲覧APIのみを追加し、書き込み系メソッドは追加していない
  ことを`AuditLogController`のソースで確認済み

## まとめ

| SECURITY項目 | 状態 |
|---|---|
| SECURITY-01〜09、11〜15 | 各Unitの実装・テストで作り込み済み（詳細は各UnitのNFR Design参照） |
| SECURITY-10（依存関係脆弱性スキャン） | **未達成（ギャップ）**。`.github/dependabot.yml`等が未導入 |
