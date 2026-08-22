# Tech Stack Decisions — Unit 6: その他機能

requirements.md 3章およびUnit 1〜5のNFR Requirementsで既に確定済みの技術スタックに加え、
Unit 6のNFR Requirementsで新たに確定した項目を記録する。

## 既存確定事項（参考）

- 対象RDBMSアクセス: `NamedParameterJdbcTemplate`（requirements.md 3章、Unit 3の
  `ConnectionPoolRegistry`が提供するプールを利用）。`:paramName`形式のプレースホルダを
  ネイティブサポートするため、クエリ実行のパラメータバインディングにもそのまま使う
  （Functional Design Question 4）
- 依存関係管理: Gradle `dependencyLocking` + GitHub Dependabot（Unit 1確立済み）
- 環境変数管理: `.env`/環境変数方式（Unit 1・2確立済み）

## Unit 6で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| クエリ実行結果の上限・タイムアウト | 最大1,000件、`Statement#setQueryTimeout`で30秒 | Question 1: 意図しない巨大結果セット・長時間実行を防ぐ |
| 読み取り専用の多層防御 | `Connection#setReadOnly(true)`をブロックリスト方式に追加 | Question 2: JDBCドライバレベルでの追加防御 |
| 大量データ取得閾値 | `MM5_BULK_ACCESS_THRESHOLD`環境変数（デフォルト100） | Question 3: Unit 5・Unit 6で共通の設定値を参照する |
| ページングのデフォルト件数 | 50件（OFFSET/LIMIT方式） | Question 4: Unit 5の`listRecords`と統一する |
| フィルタ列のインデックス | `AuditEvent`/`QueryExecutionHistory`の主要フィルタ列にインデックス追加 | Question 5: フィルタ性能の維持 |
| `buildSql`/`parseSqlToBuilderState`の実装言語 | TypeScript（フロントエンド） | Functional Design Question 1: バックエンドにSQLパーサを持たない |
| パラメータ検出 | Spring `NamedParameterUtils` | Functional Design Question 4: 独自正規表現実装を避ける |

## Unit 5への変更申し送り

`MasterMaintenanceServiceImpl#listRecords`に、返却件数が
`MM5_BULK_ACCESS_THRESHOLD`（デフォルト100）以上の場合に「大量データ取得」監査
イベントを記録する処理を追加する（Functional Design Question 6、business-rules.md
BR-15）。
