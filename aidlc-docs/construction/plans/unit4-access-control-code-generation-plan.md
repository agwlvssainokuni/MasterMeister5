# Code Generation Plan — Unit 4: アクセス制御

## Unit Context

- **対応ストーリー**: US-2.4〜US-2.7（4件、stories.md Epic 2の残り）
- **含まれるコンポーネント**: AccessControlComponent、PermissionCacheComponent
- **依存Unit**: Unit 2（`User`エンティティ、`UserJpaRepository#findByEmail`、
  `AuditLogService`）、Unit 3（`TargetConnection`、`ConnectionSchemaService#getSchema`。
  権限設定自体は`DbSchema`/`DbTable`/`DbColumn`のIDを直接参照しない、Functional Design
  Question 1）
- **後続Unitへの提供インターフェース**: `AccessControlService#resolveEffectivePermission`
  （Unit 5のマスタメンテナンス、Unit 6のクエリ実行時検証が利用）
- **本UnitがオーナーとなるDBエンティティ**: `UserGroup`、`GroupMembership`、
  `PermissionEntry`
- **既存Unitへの変更**: Unit 3の`ConnectionSchemaServiceImpl#importSchema`に
  `PermissionCacheService#invalidateByConnection`呼び出しを追加する（NFR Design
  logical-components.md「Unit 3との連携」参照。循環依存にはならない一方向の依存追加）
- **明示的なスコープ外**: Unit 5・6での`resolveEffectivePermission`の実際の呼び出し
  （レコード表示・編集可否判定、クエリ実行時のカラムレベル検証）は各Unitの対象

## REST APIエンドポイント一覧

| メソッド/パス | 認可 | 対応ストーリー |
|---|---|---|
| `GET /api/admin/groups` | ADMIN限定 | US-2.7 |
| `POST /api/admin/groups` | ADMIN限定 | US-2.7 |
| `PATCH /api/admin/groups/{groupId}` | ADMIN限定 | US-2.7 |
| `DELETE /api/admin/groups/{groupId}` | ADMIN限定 | US-2.7 |
| `GET /api/admin/groups/{groupId}/members` | ADMIN限定 | US-2.7 |
| `POST /api/admin/groups/{groupId}/members` | ADMIN限定 | US-2.7 |
| `DELETE /api/admin/groups/{groupId}/members/{userId}` | ADMIN限定 | US-2.7 |
| `GET /api/admin/permissions` （connectionId, subjectType, subjectId, schemaNameクエリパラメータ） | ADMIN限定 | US-2.4 |
| `POST /api/admin/permissions/primary` | ADMIN限定 | US-2.4 |
| `POST /api/admin/permissions/auxiliary` | ADMIN限定 | US-2.4 |
| `GET /api/admin/connections/{connectionId}/permissions/export` | ADMIN限定 | US-2.5 |
| `POST /api/admin/connections/{connectionId}/permissions/import` | ADMIN限定 | US-2.6 |

`GET /api/admin/users`（Unit 2既存）、`GET /api/admin/connections`・スキーマ参照API
（Unit 3既存）をSubject選択・スキーマツリー表示に再利用する。

## 実行ステップ

### Step 1: 依存関係・設定の追加
- [x] 1.1 `backend/build.gradle.kts`にCaffeine（`com.github.ben-manes.caffeine:caffeine`）、
      Jackson YAMLモジュール（`com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`）を
      追加する

### Step 2: Business Logic Generation
- [x] 2.1 `UserGroup`エンティティ（JPA）
- [x] 2.2 `GroupMembership`エンティティ（JPA、`(groupId, userId)`一意制約）
- [x] 2.3 `PermissionEntry`エンティティ（JPA、`SubjectType`enum（USER/GROUP）、
      `ResourceLevel`enum（SCHEMA/TABLE/COLUMN）、`PrimaryLevel`enum（NONE/READ/UPDATE）、
      `(subjectType, subjectId, connectionId, resourceLevel, schemaName, tableName,
      columnName)`一意制約）
- [x] 2.4 `UserGroupJpaRepository`/`GroupMembershipJpaRepository`/
      `PermissionEntryJpaRepository`（Subject条件のIN句検索メソッドを含む）
- [x] 2.5 `AccessControlException`（業務例外、`ConnectionException`と同型パターン。
      グループ名重複・グループ未検出・Subject未解決・識別子不正・重複エントリ等）
- [x] 2.6 `CacheKey`（値オブジェクト）、`PermissionCacheService`/
      `PermissionCacheServiceImpl`（Caffeine、`accesscontrol.cache`パッケージ、
      `getCached`/`put`/`invalidateByUser`/`invalidateByGroup`/`invalidateByConnection`）
- [x] 2.7 `EffectivePermission`（値オブジェクト、primaryLevel/canCreate/canDelete）
- [x] 2.8 `PermissionYamlMapper`（`PermissionEntry`集合⇔YAML DTO変換、Jackson YAML
      モジュール使用）
- [x] 2.9 `AccessControlService`/`AccessControlServiceImpl`（`setPrimaryPermission`/
      `setAuxiliaryPermission`/`createGroup`/`renameGroup`/`deleteGroup`/
      `addUserToGroup`/`removeUserFromGroup`/`resolveEffectivePermission`
      [階層フォールバック＋ユーザ優先・グループ合成アルゴリズム]/`exportPermissions`/
      `importPermissions`[単一トランザクション全置換]）
- [x] 2.10 Unit 3の`ConnectionSchemaServiceImpl#importSchema`を修正し、
      `PermissionCacheService`への依存を追加、`SchemaImportResult`返却直前に
      `invalidateByConnection(connectionId)`を呼び出す（既存ファイルの修正、新規ファイルは
      作成しない）

### Step 3: Business Logic Unit Testing
- [x] 3.1 `AccessControlServiceImplTest`（グループCRUD・カスケード削除、権限設定upsert、
      実効権限算出[ユーザ優先・グループ合成・階層フォールバック]、YAML全置換インポート・
      重複拒否・Subject未解決拒否）
- [x] 3.2 `PermissionCacheServiceImplTest`（getCached/put、invalidateByUser/Group/
      Connectionの無効化範囲）
- [x] 3.3 `PermissionYamlMapperTest`（往復変換の正当性）
- [x] 3.4 property-based-testing拡張（jqwik）: functional-design/business-logic-model.mdの
      「テスト対象プロパティ」6件を実装する
- [x] 3.5 Unit 3の`ConnectionSchemaServiceImplTest`に、`importSchema`成功時
      `invalidateByConnection`が呼び出されることを検証するテストケースを追加する

### Step 4: Business Logic Summary
- [x] 4.1 `aidlc-docs/construction/unit4-access-control/code/business-logic-summary.md`
      を生成する

### Step 5: API Layer Generation
- [x] 5.1 `GroupController`（一覧・作成・改名・削除、メンバー追加/削除）
- [x] 5.2 `PermissionController`（既存権限参照、主権限/補助権限設定、YAMLエクスポート/
      インポート）
- [x] 5.3 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与）
- [x] 5.4 `GlobalExceptionHandler`更新: `AccessControlException`用のハンドラを追加
- [x] 5.5 `SecurityConfig`確認: 既存の`/api/admin/**` → `hasRole("ADMIN")`ルールが
      新規エンドポイントをそのままカバーしており、追加変更は不要と確認する
- [x] 5.6（実行時追加）Unit 3の`ConnectionController`に
      `GET /api/admin/connections/{connectionId}/schema`を追加し、PermissionScreenの
      スキーマツリー表示用に`ConnectionSchemaService#getSchema`をJSON化して返す
      （`SchemaViewResponse`/`TableViewResponse`/`ColumnViewResponse`を新規生成）

### Step 6: API Layer Unit Testing
- [x] 6.1 `GroupControllerTest`（作成/改名/削除、メンバー追加/削除、actorUserId伝播）
- [x] 6.2 `PermissionControllerTest`（権限設定、YAMLエクスポート、YAMLインポート成功/
      検証エラー）

### Step 7: API Layer Summary
- [x] 7.1 `aidlc-docs/construction/unit4-access-control/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [x] 8.1 `UserGroupJpaRepository`/`GroupMembershipJpaRepository`/
      `PermissionEntryJpaRepository`（Step 2で先行生成済み）

### Step 9: Repository Layer Unit Testing
- [x] 9.1 各リポジトリの`@DataJpaTest`（一意制約、Subject条件のIN句検索、カスケード削除
      前提のクエリ）

### Step 10: Repository Layer Summary
- [x] 10.1
      `aidlc-docs/construction/unit4-access-control/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [x] 11.1 APIクライアント関数（`api/groups.ts`、`api/permissions.ts`）
- [x] 11.2 `GroupManagementScreen`（一覧、作成/改名モーダル、削除確認、メンバー管理）
- [x] 11.3 `PermissionScreen`（接続選択、Subject選択、スキーマツリー表示、主権限/補助権限
       設定、YAMLエクスポート/インポート）
- [x] 11.4 `App.tsx`ルーティング更新（`/groups`、`/permissions`、
       `RequireAuth role="ADMIN"`配下）、`AppLayout`更新（ADMIN限定navItem「グループ管理」
       「アクセス権限」を追加）
- [x] 11.5 i18nメッセージ追加（ja/en、グループ管理・権限設定画面の全文言・エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [x] 12.1 `GroupManagementScreen.test.tsx`（一覧表示、作成、メンバー追加/削除）
- [x] 12.2 `PermissionScreen.test.tsx`（接続/Subject選択、ツリー表示、権限設定送信、
       YAMLインポートエラー表示）

### Step 13: Frontend Components Summary
- [x] 13.1 `aidlc-docs/construction/unit4-access-control/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [x] 14.1 `V11__create_user_group.sql`（Step 3のテスト前提として先行生成済み）
- [x] 14.2 `V12__create_group_membership.sql`（同上）
- [x] 14.3 `V13__create_permission_entry.sql`（同上）

### Step 15: Documentation Generation
- [x] 15.1 `README.md`更新（グループ管理・アクセス権限設定画面の概要、必要であれば操作手順）

### Step 16: Deployment Artifacts Generation
- [x] 16.1 デプロイ関連の環境変数追加は不要（Caffeineはプロセス内キャッシュのため設定項目
      なし）。念のため確認のみ行う

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
