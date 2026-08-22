# API Layer Summary — Unit 3: 対象RDBMSセットアップ

## 生成したコントローラ

| コントローラ | エンドポイント | 認可 |
|---|---|---|
| `ConnectionController` | `GET/POST /api/admin/connections`、`POST /api/admin/connections/{id}/deactivate`、`POST /api/admin/connections/{id}/reactivate`、`POST /api/admin/connections/{id}/schema-import` | ADMIN限定 |

Unit 2で確定済みの`SecurityConfig`の`/api/admin/**` → `hasRole("ADMIN")`ルールが
そのままカバーするため、`SecurityConfig`自体の変更は不要だった。`GlobalExceptionHandler`に
`ConnectionException`用のハンドラを追加した（Step 2で先行対応済み）。

## 生成したDTO

`RegisterConnectionRequest`/`ConnectionSummaryResponse`/`SchemaImportResultResponse`
（すべてrecord、SECURITY-05の入力検証アノテーション付与。ホスト名等の許可文字検証は
サービス層で実施）

## 生成したテスト

- `ConnectionControllerTest`（4件）
- ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能なため（Unit 2と同じ制約）、
  Build and Testの結合テストに委ねる
