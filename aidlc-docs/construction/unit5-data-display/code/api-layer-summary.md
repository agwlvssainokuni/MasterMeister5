# API Layer Summary — Unit 5: データ表示

## 生成したコントローラ

| コントローラ | エンドポイント | 認可 |
|---|---|---|
| `MasterDataController` | `POST /api/data/connections/{id}/tables`、`POST /api/data/connections/{id}/tables/{schemaName}/{tableName}/records`、`POST /api/data/connections/{id}/tables/{schemaName}/{tableName}/apply` | 認証済み全ユーザ（ロール不問） |
| `CustomizationController` | `GET /api/admin/connections/{id}/customizations/{schemaName}/{tableName}`、`GET /api/admin/connections/{id}/customizations/export`、`POST /api/admin/connections/{id}/customizations/import` | ADMIN限定 |

`SecurityConfig`の既存の`anyRequest().authenticated()`フォールバックが
`/api/data/**`をそのままカバーするため、`SecurityConfig`自体の変更は不要だった
（Code Generation Plan Step 5.5で確認）。`/api/admin/**` → `hasRole("ADMIN")`の
既存ルールが`/api/admin/connections/**/customizations/**`もそのままカバーする。
`GlobalExceptionHandler`に`MasterMaintenanceException`用のハンドラを追加した。

## 計画からの変更点

Code Generation Planでは`listRecords`をGETエンドポイント（フィルタ・ソートを
クエリパラメータで受け取る）としてスケッチしていたが、フィルタ条件（`FilterCondition`
のリスト）を素直なGETクエリパラメータにマッピングするのは不自然なため、JSON本体を
受け取るPOST「検索」エンドポイントに変更した（`ListRecordsRequest`）。この変更は
`api-layer`の実装時点で確定した設計判断であり、`ListRecordsRequest`のJavadocに
理由を明記した。

## 生成したDTO

`TableSummaryResponse`/`ListRecordsRequest`/`FilterConditionRequest`/
`ColumnDefResponse`/`RecordPageResponse`/`RecordChangeRequest`/
`ApplyChangesRequest`/`ApplyResultResponse`/`CustomizationDefinitionResponse`/
`ImportCustomizationResultResponse`（すべてrecord、SECURITY-05の入力検証
アノテーション付与）。カスタマイズ定義の列単位の詳細（`CustomizationYamlColumn`等）は
サービス層の型をそのままレスポンスに再利用した（Unit 4の`PermissionEntryResponse`と
異なり、追加のマッピングを要しない単純な値の集まりであるため）。

YAMLエクスポートは`ResponseEntity<String>`に`Content-Disposition: attachment`
ヘッダを付与して返す。YAMLインポートは`@RequestBody String`で生のYAMLテキストを
受け取る（Unit 4の`PermissionController`と同じパターン）。

## 生成したテスト

- `MasterDataControllerTest`（4件。ADMIN限定ではなく認証済み全ユーザがアクセス
  できることを前提に、actor-id伝播とレスポンスマッピングを検証）
- `CustomizationControllerTest`（4件）
- ADMIN限定の実効性（403）・認証済みユーザの実効性は`@WebMvcTest`スライスでは
  検証不能なため（Unit 2〜4と同じ制約）、Build and Testの結合テストに委ねる
