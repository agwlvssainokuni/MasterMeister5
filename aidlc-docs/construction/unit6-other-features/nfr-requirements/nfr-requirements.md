# NFR Requirements — Unit 6: その他機能

## Scalability / Performance / Availability

requirements.md 5章に基づき、Unit 6固有の特別な追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- クエリ実行結果は最大1,000件で打ち切り、実行タイムアウトは30秒とする
  （`Statement#setQueryTimeout`、Question 1）
- クエリ実行履歴・監査ログのページングはUnit 5と同じOFFSET/LIMIT方式、
  デフォルト50件を踏襲する（Question 4）
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない（Unit 1〜5と
  同じ方針）

## Security（security-baseline拡張との対応）

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | AuditLogService（Unit 2確立済み）がクエリ保存/論理非表示・クエリ実行・大量データ取得イベントを記録する |
| SECURITY-05（入力検証） | クエリ実行のスキーマ名はUnit 3の許可リスト（`isSchemaAllowed`）で検証する。SQL文字列はUnit 5と同じブロックリスト方式（`;`、`--`、`/*`検出）で検証する |
| SECURITY-09（ハードニング） | クエリ実行に使用するJDBCコネクションに`Connection#setReadOnly(true)`を設定し、ブロックリストと合わせた多層防御とする（Question 2） |
| SECURITY-14（監査ログ改ざん防止） | Unit 2で確立済みの「アプリケーションコードから更新・削除できない」制約をそのまま維持する。本Unitは閲覧専用のAPI（`listEvents`拡張）のみ追加する |

## Reliability

- クエリ実行は読み取り専用（ブロックリスト＋`setReadOnly(true)`の多層防御）のため、
  対象RDBMSへの書き込み事故のリスクは低い。実行タイムアウト（30秒）により長時間
  実行クエリが接続プールを占有し続けることを防ぐ

## Maintainability

- テストフレームワーク: JUnit5 + Mockito + jqwik（バックエンド）、Vitest + React
  Testing Library（フロントエンド）— Unit 1〜5で確定済みの方針を踏襲
- property-based-testing拡張: functional-design/business-logic-model.mdの「テスト対象
  プロパティ」節（PBT-01）で識別した4件をCode Generationで実装する（jqwik使用）
- `AuditEvent`の`eventType`/`actorUserId`/`occurredAt`、`QueryExecutionHistory`の
  `executedByUserId`/`connectionId`/`executedAt`にインデックスを追加する
  （Question 5、フィルタ性能の維持）

## Usability

requirements.md 5章の多言語対応（日英2言語）に従い、`QueryScreen`/
`QueryHistoryScreen`/`AuditLogScreen`の文言・エラーメッセージをi18n基盤経由で
多言語化する。
