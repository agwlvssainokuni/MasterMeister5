# API Layer Summary — Unit 4: アクセス制御

## 生成したコントローラ

| コントローラ | エンドポイント | 認可 |
|---|---|---|
| `GroupController` | `GET/POST /api/admin/groups`、`PATCH/DELETE /api/admin/groups/{id}`、`GET/POST /api/admin/groups/{id}/members`、`DELETE /api/admin/groups/{id}/members/{userId}` | ADMIN限定 |
| `PermissionController` | `GET /api/admin/permissions`、`POST /api/admin/permissions/primary`、`POST /api/admin/permissions/auxiliary`、`GET /api/admin/connections/{id}/permissions/export`、`POST /api/admin/connections/{id}/permissions/import` | ADMIN限定 |

Unit 2で確定済みの`SecurityConfig`の`/api/admin/**` → `hasRole("ADMIN")`ルールが
そのままカバーするため、`SecurityConfig`自体の変更は不要だった。`GlobalExceptionHandler`
に`AccessControlException`用のハンドラを追加した。

### 既存コントローラへの追加（Unit 3の`ConnectionController`）

PermissionScreenのスキーマツリー表示のため、`GET /api/admin/connections/{id}/schema`
（`ConnectionSchemaService#getSchema`をそのままJSON化して返す）を追加した。
`SchemaViewResponse`/`TableViewResponse`/`ColumnViewResponse`（record）を新規生成した。

## 生成したDTO

`CreateGroupRequest`/`RenameGroupRequest`/`AddGroupMemberRequest`/
`GroupSummaryResponse`/`GroupMemberResponse`/`SetPrimaryPermissionRequest`/
`SetAuxiliaryPermissionRequest`/`PermissionEntryResponse`/
`ImportPermissionsResultResponse`（すべてrecord、SECURITY-05の入力検証アノテーション
付与。識別子の許可文字検証はサービス層で実施）

YAMLエクスポートは`ResponseEntity<String>`に`Content-Disposition: attachment`
ヘッダを付与して返す。YAMLインポートは`@RequestBody String`で生のYAMLテキストを
受け取る（Jackson YAMLモジュールでのパースはサービス層で行う）。

## 生成したテスト

- `GroupControllerTest`（7件）
- `PermissionControllerTest`（6件。YAMLエクスポートのヘッダ検証、インポート成功/
  重複エラーの400応答を含む）
- ADMIN限定の実効性（403）は`@WebMvcTest`スライスでは検証不能なため（Unit 2/3と同じ
  制約）、Build and Testの結合テストに委ねる
