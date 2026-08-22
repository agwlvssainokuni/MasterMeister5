# API Layer Summary — Unit 6: その他機能

## 生成したコントローラ

| コントローラ | エンドポイント | 認可 |
|---|---|---|
| `QueryController` | `GET /api/query/saved-queries`、`POST /api/query/saved-queries`、`DELETE /api/query/saved-queries/{id}`、`POST /api/query/detect-parameters`、`POST /api/query/execute`、`GET /api/query/execution-history` | 認証済み全ユーザ（ロール不問） |
| `AuditLogController`（Unit 2の`audit.controller`パッケージに新設） | `GET /api/admin/audit-events` | ADMIN限定 |

`SecurityConfig`の既存の`anyRequest().authenticated()`フォールバックが
`/api/query/**`をそのままカバーし、`/api/admin/**` → `hasRole("ADMIN")`の既存ルールが
`/api/admin/audit-events/**`もそのままカバーするため、`SecurityConfig`自体の変更は
不要だった（Unit 5の前例と同様、Code Generation Plan Step 5.5で確認）。
`GlobalExceptionHandler`に`QueryException`用のハンドラを追加した。

## 生成したDTO

### `query.controller.dto`
`SaveQueryRequest`/`SavedQueryIdResponse`/`SavedQueryResponse`/
`DetectParametersRequest`/`ParameterDescriptorResponse`/`ExecuteQueryRequest`/
`QueryResultResponse`（`truncated`フラグは`rowCount >= 1000`から算出、
`setMaxRows`は例外を送出せず黙って打ち切るため）/`ExecutionHistoryResponse`/
`ExecutionHistoryPageResponse`（すべてrecord、SECURITY-05の入力検証アノテーション
付与）

### `audit.controller.dto`（Unit 2への追加）
`AuditEventResponse`/`AuditEventPageResponse`

`saveQuery`はcomponent-methods.mdの出力仕様（`SavedQueryId`）通り、保存後のIDのみを
返す（`SavedQueryIdResponse`）。フロントエンドは必要であれば`listSavedQueries`を
再取得する。`retireQuery`は`ResponseEntity<Void>`（204 No Content）を返す
（Unit 2〜5にDELETEエンドポイントの前例がなかったため、標準的なREST慣習として
新規に採用した）。

`listExecutionHistory`・`listEvents`はいずれもクエリパラメータ（`page`/`size`と
各フィルタ項目）を受け取り、`PageRequest.of(page, size)`を構築してサービス層の
`Pageable`引数に渡す（Unit 5の`page`/`pageSize`整数パターンと異なり、内部データベース
に対するJPAページングであるため、Spring Data標準の`Pageable`/`Page`をサービス層の
シグネチャにもそのまま使用した）。

## 生成したテスト

- `QueryControllerTest`（8件。認証済み全ユーザがアクセスできることを前提に、
  actor-id伝播とレスポンスマッピングを検証）
- `AuditLogControllerTest`（2件）
- ADMIN限定の実効性（403）・認証済みユーザの実効性は`@WebMvcTest`スライスでは
  検証不能なため（Unit 2〜5と同じ制約）、Build and Testの結合テストに委ねる
