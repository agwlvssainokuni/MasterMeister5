# Code Generation Plan — Unit 6: その他機能

## Unit Context

- **対応ストーリー**: US-4.1〜US-4.6（クエリビルダー・保存・実行・履歴、6件、
  stories.md Epic 4全体）、US-5.1（監査ログ閲覧、stories.md Epic 5）、計7件
- **含まれるコンポーネント**: QueryComponent、AuditLogComponent（閲覧APIの追加実装）
- **依存Unit**: Unit 2（`AuditLogService`）、Unit 3（`ConnectionSchemaService#isSchemaAllowed`、
  `ConnectionPoolRegistry`）
- **後続Unitへの提供インターフェース**: なし（Unit 6は最後のUnit）
- **本UnitがオーナーとなるDBエンティティ**: `SavedQuery`、`QueryExecutionHistory`
- **既存Unitへの変更**:
  - Unit 2の`AuditLogService`/`AuditLogServiceImpl`に
    `listEvents(AuditEventFilterCriteria, Pageable)`オーバーロードを追加する
  - Unit 2の`audit`パッケージに`controller`サブパッケージを新設し、
    `AuditLogController`を追加する
  - Unit 5の`MasterMaintenanceServiceImpl#listRecords`に、返却件数が
    `MM5_BULK_ACCESS_THRESHOLD`（デフォルト100）以上の場合の「大量データ取得」
    監査イベント記録を追加する（NFR Requirements Question 3・6の申し送り事項）
- **明示的なスコープ外**: なし（Unit 6が最終Unit）

## REST APIエンドポイント一覧

| メソッド/パス | 認可 | 対応ストーリー |
|---|---|---|
| `GET /api/query/saved-queries` | 認証済み全ユーザ（PUBLIC＋自身のPRIVATE） | US-4.3・US-4.4 |
| `POST /api/query/saved-queries` | 認証済み全ユーザ（新規作成・自身の既存更新） | US-4.3 |
| `DELETE /api/query/saved-queries/{id}` | 認証済み全ユーザ（作成者限定、論理非表示） | US-4.4 |
| `POST /api/query/detect-parameters` | 認証済み全ユーザ | US-4.5 |
| `POST /api/query/execute` | 認証済み全ユーザ | US-4.1〜US-4.5 |
| `GET /api/query/execution-history`（フィルタ・ページクエリパラメータ） | 認証済み全ユーザ | US-4.6 |
| `GET /api/admin/audit-events`（フィルタ・ページクエリパラメータ） | ADMIN限定 | US-5.1 |

`SecurityConfig`は、既存の`anyRequest().authenticated()`フォールバックが
`/api/query/**`をそのまま認証済み全ユーザに許可し、`/api/admin/**`ルールが
`/api/admin/audit-events/**`をADMIN限定にするため、Unit 5の前例（Step 5.5参照）と
同様に変更不要と見込まれる。Step 5で実装時に確認する。

## 実行ステップ

### Step 1: 依存関係・設定の追加
- [x] 1.1 新規ライブラリ追加は不要であることを確認する（`NamedParameterJdbcTemplate`・
      `NamedParameterUtils`はSpring標準）

### Step 2: Business Logic Generation
- [x] 2.1 `SavedQuery`エンティティ（JPA、`visibility`enum[PUBLIC/PRIVATE]、
      `status`enum[ACTIVE/RETIRED]）
- [x] 2.2 `QueryExecutionHistory`エンティティ（JPA、`params`はJSON変換、
      Unit 2の`JsonMapConverter`を再利用）
- [x] 2.3 `SavedQueryJpaRepository`/`QueryExecutionHistoryJpaRepository`
- [x] 2.4 `QueryException`（業務例外、既存Unitと同型パターン）
- [x] 2.5 `ParameterDescriptor`/`QueryResult`/`ExecutionHistoryFilterCriteria`
      （値オブジェクト）
- [x] 2.6 `QueryService`/`QueryServiceImpl`（`saveQuery`[新規作成/既存更新・作成者検証]/
      `retireQuery`[論理非表示・作成者検証]/`detectParameters`[`NamedParameterUtils`]/
      `executeQuery`[読み取り専用検証・`isSchemaAllowed`検証・`setSchema`・
      `setMaxRows(1000)`/`setQueryTimeout(30)`/`setReadOnly(true)`・実行・履歴記録・
      監査ログ記録（クエリ実行＋大量データ取得の閾値判定）]/`listExecutionHistory`）
- [x]（実行時に判明）`detectParameters`は計画時点では`NamedParameterUtils`
      経由でのパラメータ名取得を想定していたが、`ParsedSql#getParameterNames()`が
      パッケージプライベートでアプリケーションコードから呼び出せないことがコンパイル
      エラーで判明した。`NamedParameterJdbcTemplate`自身が認識する`:paramName`構文と
      同じ正規表現でパラメータ名を検出する実装に変更した（実行時のバインディング自体は
      引き続き`NamedParameterJdbcTemplate`＋`MapSqlParameterSource`に全て委譲）
- [x] 2.7（実行時に判明）`ReadOnlySqlValidator`は独立クラスとせず、Unit 5の
      `MasterMaintenanceServiceImpl.validateRawClause`と同じ「共通化のための抽象化を
      避ける」方針に倣い、`QueryServiceImpl`内のprivateメソッド（`validateReadOnlySql`）
      として実装した
- [x] 2.8 Unit 2の`AuditEventFilterCriteria`（値オブジェクト、新規）を追加する
- [x] 2.9 Unit 2の`AuditLogService`インタフェースに
      `listEvents(AuditEventFilterCriteria, Pageable)`を追加し、
      `AuditLogServiceImpl`に実装を追加する（既存の`listEvents(Pageable)`は変更しない、
      既存ファイルの修正）
- [x] 2.10 Unit 5の`MasterMaintenanceServiceImpl#listRecords`を修正し、返却件数が
      `MM5_BULK_ACCESS_THRESHOLD`（デフォルト100、環境変数）以上の場合に
      「大量データ取得」監査イベントを追加記録する（既存ファイルの修正。「返却件数」は
      ページ内の行数ではなく、フィルタ条件に合致する全件数[`totalCount`]で判定する
      設計判断とした。理由をコードコメントに明記）

### Step 3: Business Logic Unit Testing
- [x] 3.1 `QueryServiceImplTest`（saveQueryの新規作成/更新/作成者検証、retireQueryの
      作成者検証、detectParametersのパラメータ検出、executeQueryの読み取り専用検証・
      スキーマ許可検証・実行・履歴記録・監査ログ記録、大量データ取得閾値判定、
      listExecutionHistoryのフィルタ・ページング）
- [x] 3.2 property-based-testing拡張（jqwik）: functional-design/
      business-logic-model.mdの「テスト対象プロパティ」4件（PBT-01）を実装する
- [x]（実行時に判明）大量データ取得閾値のProperty実装で、jqwikは1つの
      `@Property`メソッド内の全試行を同一テストインスタンスで実行する（JUnit
      Jupiterのメソッド単位インスタンス生成とは異なる）ことが
      `TooManyActualInvocations`エラーで判明した。共有モックの呼び出し履歴を
      各試行の先頭で`Mockito.clearInvocations`する対応で修正した
- [x] 3.3 Unit 2の`AuditLogServiceImplTest`に、`listEvents(AuditEventFilterCriteria, Pageable)`
      のテストケースを追加する
- [x] 3.4 Unit 5の`MasterMaintenanceServiceImplTest`に、大量データ取得監査イベントが
      閾値以上の件数で記録されることを検証するテストケースを追加する

### Step 4: Business Logic Summary
- [x] 4.1 `aidlc-docs/construction/unit6-other-features/code/business-logic-summary.md`
      を生成する

### Step 5: API Layer Generation
- [x] 5.1 `QueryController`（保存クエリCRUD、パラメータ検出、クエリ実行、実行履歴閲覧）
- [x] 5.2 Unit 2の`audit.controller`パッケージに`AuditLogController`（監査ログ閲覧）を
      新規追加する
- [x] 5.3 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与）
- [x] 5.4 `GlobalExceptionHandler`更新: `QueryException`用のハンドラを追加
- [x] 5.5 `SecurityConfig`確認: 既存ルールで`/api/query/**`・`/api/admin/audit-events/**`
      がカバーされているか確認する（Unit 5の前例と同様、変更不要と見込むが実装時に検証）
- [x]（実行時追加）`retireQuery`はUnit 2〜5に前例のないDELETEエンドポイントであるため、
      `ResponseEntity<Void>`（204 No Content）を返す標準的なREST慣習を新規に採用した

### Step 6: API Layer Unit Testing
- [x] 6.1 `QueryControllerTest`（保存クエリCRUD、パラメータ検出、クエリ実行、
      実行履歴閲覧のactorUserId伝播・権限拒否時の応答）
- [x] 6.2 `AuditLogControllerTest`（監査ログ閲覧、ADMIN限定の確認）

### Step 7: API Layer Summary
- [x] 7.1 `aidlc-docs/construction/unit6-other-features/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [x] 8.1 `SavedQueryJpaRepository`/`QueryExecutionHistoryJpaRepository`（Step 2で
      先行生成済み）

### Step 9: Repository Layer Unit Testing
- [x] 9.1 各リポジトリの`@DataJpaTest`（作成者別検索、visibility別検索、
      フィルタ列検索）

### Step 10: Repository Layer Summary
- [x] 10.1
      `aidlc-docs/construction/unit6-other-features/code/repository-layer-summary.md`
      を生成する

### Step 11: Frontend Components Generation
- [x] 11.1 APIクライアント関数（`api/query.ts`、`api/auditLog.ts`）
- [x] 11.2 `QueryScreen`（クエリビルダータブUI[SELECT/FROM/JOIN/WHERE/GROUP BY/
       HAVING/ORDER BY/LIMIT OFFSET]、`buildSql`/`parseSqlToBuilderState`
       [TypeScript実装]、保存/論理非表示、パラメータ入力、実行、結果表示）
- [x] 11.3 `QueryHistoryScreen`（実行履歴一覧、フィルタ、make-you-chic-uiの`Table`
       コンポーネントによるページング）
- [x] 11.4 `AuditLogScreen`（監査ログ一覧、フィルタ、`Table`コンポーネントによる
       ページング、詳細JSON整形表示）
- [x] 11.5 `App.tsx`ルーティング更新（`/query`・`/query/history`は認証済み全ユーザ、
       `/admin/audit-events`は`RequireAuth role="ADMIN"`配下）、`AppLayout`更新
       （全ロール共通navItem「クエリ」「実行履歴」、ADMIN限定navItem「監査ログ」を
       追加）
- [x] 11.6 i18nメッセージ追加（ja/en、クエリビルダー・実行履歴・監査ログ画面の
       全文言・エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [x] 12.1 `QueryScreen.test.tsx`（クエリビルダー操作、保存、実行、結果表示）
- [x] 12.2 `QueryHistoryScreen.test.tsx`（一覧表示、フィルタ）
- [x] 12.3 `AuditLogScreen.test.tsx`（一覧表示、フィルタ、ADMIN限定表示）

### Step 13: Frontend Components Summary
- [x] 13.1 `aidlc-docs/construction/unit6-other-features/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [x] 14.1 `V17__create_saved_query.sql`（Step 3のテスト前提として先行生成する）
- [x] 14.2 `V18__create_query_execution_history.sql`（フィルタ列インデックスを
      インラインで定義、同上）
- [x] 14.3 `V19__add_audit_event_indexes.sql`（既存`audit_event`テーブルへの
      `CREATE INDEX`のみ、同上）

### Step 15: Documentation Generation
- [x] 15.1 `README.md`更新（クエリビルダー・実行履歴・監査ログ閲覧機能の概要）

### Step 16: Deployment Artifacts Generation
- [x] 16.1 `.env.example`（存在する場合）に`MM5_BULK_ACCESS_THRESHOLD`が
      Unit 5導入時に未追加であれば追加する。念のため確認のみ行う

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
