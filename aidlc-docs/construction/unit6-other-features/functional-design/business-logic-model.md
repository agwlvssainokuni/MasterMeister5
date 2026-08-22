# Unit 6: その他機能 - Business Logic Model

component-methods.md記載のQueryComponent・AuditLogComponent（閲覧APIの追加実装）の
メソッドを実現する業務フローを定義する。技術非依存（具体的なAPI形式・SQL実行方式は
Code Generationで確定）。

## 1. クエリビルダー（`buildSql`/`parseSqlToBuilderState`、US-4.1・US-4.2）

Question 1により、両メソッドはフロントエンドに実装する。バックエンドは関与しない。

1. `buildSql`: タブ構成（SELECT/FROM/JOIN/WHERE/GROUP BY/HAVING/ORDER BY/
   LIMIT OFFSET）の入力状態から、スキーマ非修飾のSQL文字列を組み立てる
2. `parseSqlToBuilderState`: 手入力または保存済みのSQL文字列を解析し、対応する
   タブ構成に反映する（クエリビルダー自身が生成した形式および一般的な単純SELECT文を
   対象としたベストエフォート。複雑な結合・サブクエリ等は完全な逆変換を保証しない）

## 2. クエリ保存（`saveQuery`、US-4.3）

1. 呼び出し元が`saveQuery(SqlText, Name, Visibility, SavedQueryId?)`を実行する
2. `SavedQueryId`が未指定なら新規`SavedQuery`を作成する（Question 5）
3. `SavedQueryId`が指定されていれば、対象`SavedQuery`の`creatorUserId`が呼び出し元と
   一致することを検証したうえで`sqlText`/`name`/`visibility`を更新する。一致しなければ
   拒否する（US-4.3「編集は作成者のみ可能」）
4. AuditLogComponentに保存イベントを記録する
5. `SavedQueryId`を返す

## 3. クエリの論理非表示（`retireQuery`、US-4.4）

1. 呼び出し元が`retireQuery(SavedQueryId)`を実行する
2. 対象`SavedQuery`の`creatorUserId`が呼び出し元と一致することを検証する
3. `status`を`RETIRED`に更新する（一覧から除外されるが、`QueryExecutionHistory`から
   の参照は保たれる。物理削除は行わない）

## 4. パラメータ検出（`detectParameters`、US-4.5）

1. `detectParameters(SqlText)`は、Question 4のとおりSpring
   `NamedParameterUtils`でSQL文字列を解析し、`:paramName`形式のプレースホルダ名を
   一覧として返す

## 5. クエリ実行（`executeQuery`、US-4.5）

1. 呼び出し元が`executeQuery(SqlText | SavedQueryId, ConnectionId, SchemaName,
   Params)`を実行する
2. `SavedQueryId`が指定されていれば、対応する`SavedQuery`を取得する。`visibility`が
   `PRIVATE`かつ呼び出し元が`creatorUserId`と異なる場合は拒否する
3. 実行対象のSQLが読み取り専用であることを検証する（Question 3、Unit 5のブロック
   リスト方式を踏襲: 先頭が`SELECT`/`WITH`であること、`;`・SQLコメント開始を含まない
   こと）
4. `isSchemaAllowed`（Unit 3）で対象スキーマが許可リストに含まれることを検証する
5. Unit 3の`ConnectionPoolRegistry`から取得したコネクションに対し
   `Connection#setSchema(SchemaName)`を呼び出したうえで（Question 2）、
   `NamedParameterJdbcTemplate`でSQLを実行する（Question 4のバインディング方式）
6. 実行時間・結果件数を計測し、`QueryExecutionHistory`に記録する
7. AuditLogComponentに「クエリ実行」イベントを常に記録する（結果件数・実行時間を
   含む、requirements.md 4.5）。結果件数が閾値（デフォルト100件、Question 6）以上の
   場合は、追加で「大量データ取得」イベントも記録する
8. `QueryResult`（列定義・行データ・件数・実行時間）を返す

## 6. クエリ実行履歴の閲覧（`listExecutionHistory`、US-4.6）

1. `listExecutionHistory(ExecutionHistoryFilterCriteria, Page)`は、実行日時・実行者・
   対象スキーマ・SQLテキスト（部分一致）で絞り込んだ`QueryExecutionHistory`を
   ページング付きで返す
2. 一覧は`savedQueryId`の有無で「保存クエリ」「直接入力クエリ」を区別して表示する
   （US-4.6受け入れ基準）
3. 本Unitでは、実行履歴の閲覧範囲を全ユーザ共通とする（PRIVATEな保存クエリの
   実行であっても、実行履歴自体［実行日時・実行者・SQLテキスト・件数等］は
   閲覧者を問わず参照できる。requirements.md・stories.mdに閲覧範囲の制限は
   明記されていないため、同時利用者数約10名規模の社内ツールとして、監査証跡の
   透明性を優先する設計判断とする）

## 7. 大量データ取得監査ログのUnit 5への遡及適用（Question 6）

1. Unit 5の`MasterMaintenanceServiceImpl#listRecords`に、返却件数が閾値
   （デフォルト100件）以上の場合に「大量データ取得」イベントを記録する処理を追加する
   （既存Unitへの変更として本Unitで対応する）

## 8. 監査ログ閲覧（`listEvents`の拡張、US-5.1）

1. Unit 2で実装済みの`AuditLogService#listEvents(Pageable)`に、
   `AuditEventFilterCriteria`（Question 7: `eventType`/`actorUserId`/`occurredAt`の
   期間）を受け取るオーバーロードを追加する
2. REST API・管理者限定画面を本Unitで新規追加する（Unit 2は記録機構のみ実装済み）
3. 一覧は日時（ISO 8601）・ユーザID・イベント種別・詳細（`details`のJSON整形表示、
   接続ID・対象リソース・結果ステータス等を含む）を表示する

## テスト対象プロパティ（PBT-01: property-based-testing拡張）

| 対象 | カテゴリ | プロパティ |
|---|---|---|
| 読み取り専用SQL検証 | Invariant | `SELECT`/`WITH`以外で始まる、またはセミコロン・コメント開始を含む任意のSQL文字列は常に拒否される |
| クエリ実行結果の大量データ判定 | Invariant | 結果件数が閾値以上の場合、常に「大量データ取得」イベントが追加記録される |
| 保存クエリの編集権限 | Invariant | 作成者以外による`saveQuery`（更新）・`retireQuery`の呼び出しは常に拒否される |
| パラメータ検出の網羅性 | Invariant | SQL文字列中の任意の`:paramName`形式の出現は、`detectParameters`の結果に必ず含まれる |
