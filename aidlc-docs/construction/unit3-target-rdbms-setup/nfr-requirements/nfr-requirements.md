# NFR Requirements — Unit 3: 対象RDBMSセットアップ

## Scalability / Performance / Availability
requirements.md 5章に基づき、Unit 3固有の追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- スキーマ取込は同期処理のまま、特別なタイムアウト延長・バッチ分割は行わない（Question 4）。
  極端に大規模なスキーマ（数千テーブル等）は対象外と割り切る
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない（Unit 1〜2と同じ方針）

## Security（security-baseline拡張との対応）

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | AuditLogService（Unit 2確立済み）が接続登録・無効化/再有効化・スキーマ取込イベントを記録する。接続パスワード平文は記録しない |
| SECURITY-09（ハードニング） | 接続確認失敗時、JDBCドライバの内部例外メッセージをそのまま返さず、分類コード（ホスト到達不可／認証エラー／タイムアウト／その他）のみを返す（Question 6） |
| SECURITY-10（サプライチェーン） | 新規JDBCドライバ（mysql-connector-j、postgresql、mariadb-java-client）もGradle `dependencyLocking`・GitHub Dependabotの既存対象に含める（Question 5、追加の仕組みは不要） |
| SECURITY-12（認証・認証情報管理に準ずる機微情報管理） | 接続パスワードはAES-256-GCMで可逆暗号化する（Question 1）。暗号鍵は環境変数由来とし、Unit 2のJWT署名鍵と同様の管理方式を踏襲する |

## Reliability
- 接続確認（テスト接続）のタイムアウトは5秒とする（Question 2）
- スキーマ取込失敗時の自動リトライは行わない（resiliency-baseline不適用の方針と整合）

## Maintainability
- テストフレームワーク: JUnit5 + Mockito（バックエンド）、Vitest + React Testing Library
  （フロントエンド）— Unit 1・2で確定済みの方針を踏襲
- property-based-testing拡張: functional-design/business-logic-model.mdの「テスト対象
  プロパティ」節（PBT-01）で識別した4件をCode Generationで実装する（jqwik使用）

## Usability
requirements.md 5章の多言語対応（日英2言語）に従い、ConnectionListScreenの文言・
エラーメッセージ（分類コードに対応する表示文言）をi18n基盤経由で多言語化する。
