# Tech Stack Decisions — Unit 2: ユーザ管理

requirements.md 3章およびUnit 1のNFR Requirementsで既に確定済みの技術スタックに加え、
Unit 2のNFR Requirementsで新たに確定した項目を記録する。

## 既存確定事項（参考）
- 認証・認可基盤: Spring Security（JWT用カスタムフィルタ、Unit 1でSecurityConfig骨格を整備。
  Unit 1時点では`NoopJwtTokenValidator`がプレースホルダとして`@ConditionalOnMissingBean`で
  配置されており、Unit 2で実際のJWT検証実装に置き換える）
- 構造化ログ: Logback + logstash-logback-encoder
- レート制限: bucket4j（全リクエスト対象、per-IP）
- 依存関係管理: Gradle `dependencyLocking` + GitHub Dependabot

## Unit 2で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| パスワードハッシュアルゴリズム | BCrypt（`BCryptPasswordEncoder`） | Question 1: Spring Securityのデフォルトであり実績が豊富。推奨案（Argon2id）に対しユーザーの判断でBCryptを採用 |
| 既知漏洩パスワード照合 | アプリケーション埋め込みの静的リスト | Question 2: 外部ネットワーク依存を持たず、Docker等の閉域環境でも動作する |
| リフレッシュトークンのハッシュ方式 | SHA-256（高速な決定的ハッシュ） | Question 3: トークン自体が高エントロピーな乱数値であるため、低速な適応型ハッシュは不要 |
| JWT署名アルゴリズム | 対称鍵HS256 | Question 4: 単一WARのモノリス構成で発行者・検証者が同一プロセス内にあるため、非対称鍵の鍵配布は不要。署名鍵は環境変数で設定 |
| メール送信失敗時のリトライ | 自動リトライなし | Question 5: resiliency-baseline拡張不適用の方針と整合。管理者の招待再送・ユーザーの再申請が実質的なリトライ手段 |
| 監査ログの改ざん防止 | アプリケーションレベルのみ（更新・削除メソッドを実装しない） | Question 6: AuditLogComponentは`recordEvent`/`listEvents`のみを公開する |
| AuditLogComponentのデータモデル | 単一`AuditEvent`テーブル＋JSON詳細列 | Question 7: Unit 3〜6で追加されるイベント種別にスキーマ変更なく対応できる |
| 機微設定の管理方法 | `.env`/環境変数方式（Unit 1踏襲） | Question 8: 専用シークレットマネージャの導入はrequirements.mdのスコープ外 |

## Unit 3以降への申し送り
- AuditLogComponentの`AuditEvent`テーブル（単一テーブル＋JSON詳細列方式）は、Unit 3〜6が
  新たなイベント種別を記録する際もそのまま再利用する
- SecurityInfrastructureComponentのJWT発行・検証実装（HS256、環境変数署名鍵）は、Unit 3の
  ConnectionSchemaComponent等、以降のUnitからも共通利用する
- `.env.example`にUnit 2関連の環境変数（初期管理者メールアドレス/パスワード、JWT署名鍵、
  各種トークン有効期限・ロック設定等）をCode Generationで追加する
