# Business Logic Summary — Unit 6: その他機能

## 生成したクラス

### `cherry.mastermeister5.query`（QueryComponent）
- `entity.SavedQuery`（`name`/`sqlText`/`visibility`/`creatorUserId`/`status`/
  `createdAt`/`updatedAt`。`sqlText`はスキーマ非修飾、BR-9）
- `entity.QueryExecutionHistory`（`savedQueryId`は任意、`sqlText`は実行時点の
  スナップショット、`params`はJSONカラム。`QueryParamsJsonConverter`で変換する。
  Unit 2の`JsonMapConverter`はパッケージプライベートで再利用できないため、
  同型の変換器を本パッケージに独自実装した）
- `entity.QueryVisibility`（PUBLIC/PRIVATE）、`entity.QueryStatus`（ACTIVE/RETIRED）
- `repository.SavedQueryJpaRepository`（`findVisibleTo`: PUBLIC全件＋自分の
  PRIVATE件のみをJPQLで一括取得）、`repository.QueryExecutionHistoryJpaRepository`
  （`search`: 実行者・対象接続・スキーマ・SQLテキスト部分一致・実行日時範囲による
  絞り込み、Unit 4の`PermissionEntryJpaRepository.findForResolution`と同型の
  JPQL `@Query`パターン）
- `service.QueryService`/`QueryServiceImpl`（`listSavedQueries`/`saveQuery`
  [新規作成・更新の自動判別、作成者検証]/`retireQuery`[論理非表示、作成者検証]/
  `detectParameters`/`executeQuery`[読み取り専用検証・スキーマ許可検証・
  `setSchema`・`setMaxRows(1000)`/`setQueryTimeout(30)`/`setReadOnly(true)`・
  実行・履歴記録・監査ログ記録（クエリ実行＋大量データ取得の閾値判定）]/
  `listExecutionHistory`。business-rules.md BR-1〜BR-19を実装）
- `service.QueryException`（業務例外、既存Unitと同型パターン）
- `service.ParameterDescriptor`/`QueryResult`/`ExecutionHistoryFilterCriteria`
  （サービス層の入出力record）

### 既存Unitへの変更
- Unit 2の`AuditLogService`インタフェースに
  `listEvents(AuditEventFilterCriteria, Pageable)`オーバーロードを追加した
  （既存の`listEvents(Pageable)`は変更しない、非破壊的拡張、Unit 4の
  `resolveEffectivePermissionsForTable`追加と同じパターン）。`AuditLogServiceImpl`
  に実装を追加し、`AuditEventJpaRepository`に`search`（JPQL `@Query`、実カラムの
  みを対象、BR-17）を追加した
- Unit 2に新規`audit.AuditEventFilterCriteria`（値オブジェクト）を追加した
- `AuditEventType`に`QUERY_SAVED`/`QUERY_RETIRED`/`QUERY_EXECUTED`/
  `BULK_DATA_ACCESSED`を追加した
- Unit 5の`MasterMaintenanceServiceImpl#listRecords`に、返却件数（`totalCount`、
  ページ内の行数ではなくフィルタ条件に合致する全件数）が`MM5_BULK_ACCESS_THRESHOLD`
  以上の場合の「大量データ取得」監査イベント記録を追加した（NFR Requirements
  Question 3・6の申し送り事項）
- 新規`platform.BulkAccessProperties`（`@ConfigurationProperties`、
  `mastermeister5.bulk-access.threshold`）を追加し、Unit 5・Unit 6の両方から
  参照する共通設定値とした（Unit固有パッケージではなく共有の`platform`パッケージに
  配置し、どちらのUnitにも新規依存が発生しないようにした）
- i18nメッセージ追加（`errors.query_*`、日英2言語＋デフォルト）

## パラメータ検出の実装上の制約（発見事項）

Functional Design Question 4はSpring `NamedParameterUtils`を使う方針としていたが、
実装時にコンパイルエラーで判明した通り、`NamedParameterUtils.parseSqlStatement`が
返す`ParsedSql#getParameterNames()`はパッケージプライベートで、アプリケーションコード
からは呼び出せない（Spring Framework内部専用のAPI）。このため`detectParameters`は
`NamedParameterJdbcTemplate`自身が認識する`:paramName`構文と同じ正規表現
（`(?<!:):([A-Za-z][A-Za-z0-9_]*)`）でパラメータ名を検出する。実行時のバインディング
自体は引き続き`NamedParameterJdbcTemplate`＋`MapSqlParameterSource`（内部で
`NamedParameterUtils`を使用）にすべて委譲しており、独自のSQL書き換えは行わない。

## クエリ実行の読み取り専用多層防御（executeQuery）

ブロックリスト（`;`/`--`/`/*`検出、コメント除去後の先頭がSELECT/WITHであることの
検証）に加え、対象RDBMSへの生JDBC接続に対し`setReadOnly(true)`を設定したうえで
`Connection#setSchema(schemaName)`を呼び出し、`SingleConnectionDataSource`
（`suppressClose=true`）でラップした`NamedParameterJdbcTemplate`で実行する。
読み取り専用のみでUnit 5の`applyChanges`のようなトランザクション制御
（`setAutoCommit(false)`・`commit`/`rollback`）は不要なため実装していない。

## PBT適用評価（property-based-testing拡張 PBT-01）

functional-design/business-logic-model.mdで識別した4件全てをjqwikの`@Property`で
実装した:
- 読み取り専用SQL検証（セミコロン混入は常に拒否、`anySqlContainingASemicolonIsAlwaysRejected`）
- 大量データ取得の閾値判定（`bulkDataAccessedEventIsRecordedWheneverThresholdIsMet`。
  jqwikは1つの`@Property`メソッド内の全試行を同一テストインスタンスで実行する
  （JUnit Jupiterのメソッド単位インスタンス生成とは異なる）ため、共有モックの
  呼び出し履歴を各試行の先頭で`clearInvocations`する必要があった。実装中に
  `TooManyActualInvocations`で発見・修正した）
- 保存クエリの編集権限（`nonCreatorCanNeverUpdateOrRetireASavedQuery`）
- パラメータ検出の網羅性（`detectParametersAlwaysIncludesEveryNamedPlaceholderInTheSql`）

## 生成したテスト

- `QueryServiceImplTest`（例示ベース10件、Property 4件。実際のH2データベースを
  「対象RDBMS」として使用し、`executeQuery`のSQL生成・実行の正しさをモックではなく
  実クエリで検証した）
- `QueryRepositoriesTest`（`@DataJpaTest`、SavedQuery/QueryExecutionHistory
  合わせて5件）
- Unit 2の`AuditLogServiceImplTest`に`listEvents(AuditEventFilterCriteria, Pageable)`
  委譲の検証を1件追加、`AuditEventJpaRepositoryTest`に`search`のフィルタ検証を1件追加
- Unit 5の`MasterMaintenanceServiceImplTest`に、大量データ取得監査イベントが
  閾値以上の件数で記録されることを検証するテストケースを1件追加
