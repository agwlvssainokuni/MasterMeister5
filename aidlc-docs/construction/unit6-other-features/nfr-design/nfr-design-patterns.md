# NFR Design Patterns — Unit 6: その他機能

nfr-requirements.mdの各方針・tech-stack-decisions.mdの技術選定を、具体的な設計パターンに
落とし込む。

## Resilience / Scalability Patterns

N/A。resiliency-baseline不適用、単一インスタンス・同時利用者数約10名規模のため、
本Unitでは特別な耐障害性・スケーリングパターンを導入しない（Unit 1〜5と同じ方針）。

## Performance Patterns

### クエリ実行の上限・タイムアウト（Question 2）

- `NamedParameterJdbcTemplate`が内部で保持する`JdbcTemplate`に対し
  `setMaxRows(1000)`・`setQueryTimeout(30)`を設定する。独自のJDBC制御コード
  （`Statement`の直接操作）は書かない
- 結果件数が1,000件で打ち切られた場合、`QueryResult`に含む`rowCount`と実際に
  取得した行数の関係から「結果が多いため一部のみ表示」を判定し、UIに表示する
  （`setMaxRows`による打ち切りは例外を発生させないため、行数比較で検出する）

### ページング・インデックス（Question 4・5）

- `listExecutionHistory`・監査ログ閲覧APIはUnit 5の`listRecords`と同じ
  OFFSET/LIMIT方式、デフォルト50件を踏襲する
- フィルタ列（`AuditEvent`の`eventType`/`actorUserId`/`occurredAt`、
  `QueryExecutionHistory`の`executedByUserId`/`connectionId`/`executedAt`）に
  Flywayマイグレーションでインデックスを追加する（具体的な複合/単純の別は
  Code Generationで確定する）

## Security Patterns

### 読み取り専用の多層防御（Question 2）

- ブロックリスト方式（BR-6・BR-7、Unit 5のQuestion 3と同じ`;`/`--`/`/*`検出）に加え、
  クエリ実行に使用する生JDBCコネクションに対し実行前に`Connection#setReadOnly(true)`を
  設定する
- `NamedParameterJdbcTemplate`は上記の読み取り専用設定済みコネクションをラップした
  `DataSource`から構築する（Unit 5の`applyChanges`と同様、`SingleConnectionDataSource`を
  用いる。ただしUnit 5と異なり書き込みではないため、トランザクション制御
  （`setAutoCommit(false)`・`commit`/`rollback`）は不要）

### パラメータバインディング（Question 2、Functional Design Question 4）

- Spring `NamedParameterUtils`でSQL文字列から`:paramName`形式のプレースホルダを
  検出する（`detectParameters`）
- 検出したパラメータ名と実行時の値を`MapSqlParameterSource`に設定し、
  `NamedParameterJdbcTemplate`経由で実行する。独自の正規表現実装は行わない

### スキーマ適用（Functional Design Question 2）

- 生JDBCコネクションに対し`Connection#setSchema(schemaName)`を呼び出す。SQL文字列の
  書き換えは行わない（BR-9）

## Reliability Patterns

- クエリ実行は読み取り専用のみのため、`applyChanges`（Unit 5）のような
  検証フェーズ/実行フェーズの分離やロールバック制御は不要
- 実行タイムアウト（30秒）により、長時間実行クエリが接続プールを占有し続けることを
  防ぐ（`setQueryTimeout`が対象RDBMS側でのキャンセルを試行する。ドライバ依存の
  ベストエフォートである点はUnit 3の接続確認タイムアウトと同様の前提とする）

## Unit 2・Unit 5との連携パターン

### Unit 2（`audit`パッケージ）への非破壊的拡張（Question 1・3）

- 既存の`AuditLogService#listEvents(Pageable)`はそのまま残し、新規オーバーロード
  `listEvents(AuditEventFilterCriteria filterCriteria, Pageable pageable)`を追加する
  （Unit 4の`AccessControlService#resolveEffectivePermissionsForTable`追加と同じ
  「既存Unitへの非破壊的なメソッド追加」パターン）
- `AuditEventFilterCriteria`（`eventType`/`actorUserId`/`fromDate`/`toDate`）を条件に
  `AuditEvent`をJPQL/Specification等でフィルタする（具体的な実装方式は
  Code Generationで確定する）
- 監査ログ閲覧のREST APIは、`cherry.mastermeister5.audit`パッケージに新設する
  `controller`サブパッケージに実装する（新規トップレベルパッケージは作らない）

### Unit 5（`mastermaintenance`パッケージ）への遡及変更（Question 6、business-rules.md BR-15）

- `MasterMaintenanceServiceImpl#listRecords`に、返却件数が
  `MM5_BULK_ACCESS_THRESHOLD`（デフォルト100、Unit 6と共通の環境変数）以上の場合に
  「大量データ取得」監査イベントを記録する処理を追加する
- Unit 6の`executeQuery`も同じ環境変数・同じ判定ロジック（結果件数と閾値の比較）を
  共有する。閾値判定自体を共通ヘルパーとして切り出すか、各Unitの実装内で個別に
  判定するかはCode Generationで確定する（重複が小さいため、無理な共通化はしない
  方針をUnit 1〜5から踏襲する）

## パッケージ構成（Question 1）

- `QueryComponent`関連は新規トップレベルパッケージ`cherry.mastermeister5.query`配下に
  `entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを持つ
  （Unit 2〜5のパッケージ構成方針を踏襲）
- `AuditLogComponent`の閲覧API追加は、既存の`cherry.mastermeister5.audit`パッケージに
  `controller`サブパッケージを新設して実装する
