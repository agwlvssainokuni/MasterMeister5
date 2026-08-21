# NFR Requirements — Unit 2: ユーザ管理

## Scalability / Performance / Availability
requirements.md 5章に基づき、Unit 2固有の追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- 応答性能: 具体的な数値目標は定めない。ただしQuestion 1（パスワードハッシュアルゴリズム）
  はログイン・登録・パスワード変更の応答時間に直接影響するため、tech-stack-decisions.mdで
  補足する
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない（Question 5と整合）

## Security（security-baseline拡張との対応）

Unit 2は認証・認可の中核（UserAccountComponent、SecurityInfrastructureComponent、
AuditLogComponent、NotificationComponent）を実装するため、SECURITY-12を中心に多数の
ルールが適用される。

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | AuditLogComponent（`recordEvent`）がbusiness-rules.md BR-30の全イベントを記録する。パスワード・トークン平文は記録しない（BR-31） |
| SECURITY-08（アプリケーションレベルアクセス制御） | AdminUserListScreen等の管理系APIはADMINロール限定。`RequireAuth`によるデフォルト拒否を全認証必須ルートに適用する（frontend-components.md） |
| SECURITY-09（ハードニング） | 初期管理者パスワード等の機微設定は環境変数で注入し、ソースコード・IaCにハードコードしない（Question 8） |
| SECURITY-11（セキュアデザイン） | 認証・認可ロジックはUserAccountComponent/SecurityInfrastructureComponentに分離する。誤用ケース（メールアドレス列挙、トークン再利用）はbusiness-rules.mdで既に考慮済み |
| SECURITY-12（認証・認証情報管理） | パスワードハッシュ: BCrypt（Question 1）。既知漏洩パスワード照合: 埋め込み静的リスト（Question 2）。ブルートフォース対策: アカウントロック（BR-15）。ログアウト時の即時失効（BR-22）。管理者MFAはRequirements Analysisで文書化済みの適用除外 |
| SECURITY-13（データ完全性の検証） | 監査ログの改ざん防止はアプリケーションレベルのみで担保する。AuditLogComponentは作成・参照メソッドのみを公開し、更新・削除操作を実装しない（Question 6） |
| SECURITY-14（アラート・監視） | ログイン失敗・アカウントロック・トークン再利用検知等のセキュリティイベントはAuditLogComponent経由で記録される。本格的な監視ダッシュボード・アラート配信の仕組みはrequirements.md 5章の方針（ログベースの軽量な検知で足りるものとする）に従い、Unit 2では追加実装しない |

## Reliability
- 招待メール・パスワードリセットメールの送信失敗時、自動リトライは行わない（Question 5）。
  requirements.mdでresiliency-baseline拡張が不適用と決定済みであることと整合する。管理者の
  「招待再送」操作、ユーザの再度のリセット申請が実質的なリトライ手段となる
- リフレッシュトークンの再利用検知時は、同一familyIdの全トークンを即座に一括失効させる
  （business-rules.md BR-21）ことで、トークン窃取の被害を最小化する

## Maintainability
- テストフレームワーク: JUnit5 + Mockito（バックエンド）、Vitest + React Testing Library
  （フロントエンド）— Unit 1で確定済みの方針を踏襲
- property-based-testing拡張: functional-design/business-logic-model.mdの「テスト対象
  プロパティ」節（PBT-01）で識別した8件をCode Generationで実装する（jqwik使用）
- AuditLogComponentのデータモデルは単一テーブル＋JSON詳細列とし（Question 7）、Unit 3〜6で
  追加されるイベント種別にスキーマ変更なく対応できるようにする

## Usability
requirements.md 5章の多言語対応（日英2言語）に従い、招待メール・パスワードリセットメールの
文面、ログイン/登録/パスワード関連画面の文言をi18n基盤（Unit 1確立済み）経由で多言語化する。
