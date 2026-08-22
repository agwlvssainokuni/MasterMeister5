# Code Generation Plan — Unit 5: データ表示

## Unit Context

- **対応ストーリー**: US-3.1〜US-3.7（7件、stories.md Epic 3全体）
- **含まれるコンポーネント**: MasterMaintenanceComponent
- **依存Unit**: Unit 2（`AuditLogService`）、Unit 3（`ConnectionSchemaService#getSchema`/
  `isSchemaAllowed`、`ConnectionPoolRegistry`、`SchemaImportResult`）、Unit 4
  （`AccessControlService#resolveEffectivePermission`/
  `resolveEffectivePermissionsForTable`）
- **後続Unitへの提供インターフェース**: なし（Unit 6はクエリ・監査ログ閲覧機能であり、
  本Unitのマスタメンテナンス機能には依存しない）
- **本UnitがオーナーとなるDBエンティティ**: `TableCustomization`、`ColumnCustomization`、
  `ValidationRule`
- **既存Unitへの変更**:
  - Unit 3の`ConnectionSchemaServiceImpl`に`ApplicationEventPublisher`依存を追加し、
    `importSchema`内で`SchemaImportedEvent`を発行する。`SchemaImportResult`に
    `prunedCustomizationCount`フィールドを追加する
  - Unit 3の`ConnectionListScreen`のスキーマ取込結果モーダルに
    `prunedCustomizationCount`を追加表示する
  - Unit 4の`AccessControlService`/`AccessControlServiceImpl`に
    `resolveEffectivePermissionsForTable`を追加する
- **明示的なスコープ外**: Unit 6（クエリビルダー・監査ログ閲覧）は対象外

## REST APIエンドポイント一覧

| メソッド/パス | 認可 | 対応ストーリー |
|---|---|---|
| `GET /api/data/connections/{connectionId}/tables` | 認証済み全ユーザ | US-3.1（READ権限のあるテーブル一覧） |
| `GET /api/data/connections/{connectionId}/tables/{schemaName}/{tableName}/records` （フィルタ・ソート・ページクエリパラメータ） | 認証済み全ユーザ | US-3.1〜US-3.3 |
| `POST /api/data/connections/{connectionId}/tables/{schemaName}/{tableName}/apply` | 認証済み全ユーザ | US-3.4〜US-3.6 |
| `GET /api/admin/connections/{connectionId}/customizations` | ADMIN限定 | US-3.7 |
| `GET /api/admin/connections/{connectionId}/customizations/export` | ADMIN限定 | US-3.7 |
| `POST /api/admin/connections/{connectionId}/customizations/import` | ADMIN限定 | US-3.7 |

`SecurityConfig`に、`/api/data/**`を認証済みユーザ全員に許可する新規ルール
（`hasAnyRole("ADMIN","GENERAL")`または`authenticated()`）を追加する必要がある
（既存の`/api/admin/**`ルールとは別、Step 5で確認・追加）。

## 実行ステップ

### Step 1: 依存関係・設定の追加
- [x] 1.1 新規ライブラリ追加は不要であることを確認する（Jackson YAMLモジュールは
      Unit 4で追加済み、NamedParameterJdbcTemplateはSpring標準）

### Step 2: Business Logic Generation
- [x] 2.1 `TableCustomization`/`ColumnCustomization`/`ValidationRule`エンティティ
      （JPA、`InputWidget`enum[TEXT/SELECT/CHECKBOX/DATE]、`ValidationRuleType`enum
      [REGEX/RANGE]）
- [x] 2.2 `TableCustomizationJpaRepository`/`ColumnCustomizationJpaRepository`/
      `ValidationRuleJpaRepository`
- [x] 2.3 `MasterMaintenanceException`（業務例外、既存Unitと同型パターン）
- [x] 2.4 `RecordPage`/`RecordRow`/`ColumnDef`/`FilterCriteria`/`FilterCondition`/
      `FilterOperator`enum/`SortCriteria`/`RecordChangeSet`/`RecordChange`/
      `ChangeOperation`enum/`ApplyResult`/`ImportResult`/`PruneResult`（値オブジェクト）
- [x] 2.5 `CustomizationYamlEntry`/`CustomizationYamlDocument`/`CustomizationYamlMapper`
      （Jackson YAMLモジュール、Unit 4の`PermissionYamlMapper`と同型パターン）
- [x] 2.6 `MasterMaintenanceService`/`MasterMaintenanceServiceImpl`（`listRecords`
      [実効権限バッチ判定＋カスタマイズ定義マージ＋WHERE/ORDER BY生成＋ページング]/
      `applyChanges`[検証フェーズ・実行フェーズの2段階、オールオアナッシング]/
      `getCustomizationDefinition`/`exportCustomizationDefinition`/
      `importCustomizationDefinition`[全置換]/`@EventListener`による陳腐化整理）
- [x] 2.7 Unit 3の`connectionschema.service`パッケージに`SchemaImportedEvent`を追加する
      （`connectionId`/`removedTableRefs`/`removedColumnRefs`は不変、
      `prunedCustomizationCount`は書き込み可能）
- [x] 2.8 Unit 3の`SchemaImportResult`に`prunedCustomizationCount`フィールドを追加する
      （既存ファイルの修正）
- [x] 2.9 Unit 3の`ConnectionSchemaServiceImpl`を修正し、`ApplicationEventPublisher`
      依存を追加、`importSchema`内で`SchemaImportedEvent`を発行して結果を
      `SchemaImportResult`に反映する（既存ファイルの修正、新規ファイルは作成しない）
- [x] 2.10 Unit 4の`AccessControlService`インタフェースに
      `resolveEffectivePermissionsForTable`を追加し、`AccessControlServiceImpl`に
      実装を追加する（既存の`buildChain`/`findMatch`等のprivateメソッドを再利用、
      既存ファイルの修正）

### Step 3: Business Logic Unit Testing
- [x] 3.1 `MasterMaintenanceServiceImplTest`（listRecordsの権限絞り込み・
      カスタマイズ定義マージ、applyChangesのオールオアナッシング・権限検証・
      ValidationRule検証、YAML全置換インポート、WHERE/ORDER BYブロックリスト検証）
- [x] 3.2 `CustomizationYamlMapperTest`（往復変換の正当性）
- [x] 3.3 property-based-testing拡張（jqwik）: functional-design/
      business-logic-model.mdの「テスト対象プロパティ」6件を実装する
- [x] 3.4 Unit 3の`ConnectionSchemaServiceImplTest`に、`importSchema`成功時
      `SchemaImportedEvent`が発行されることを検証するテストケースを追加する
- [x] 3.5 Unit 4の`AccessControlServiceImplTest`に、
      `resolveEffectivePermissionsForTable`のテストケースを追加する

### Step 4: Business Logic Summary
- [x] 4.1 `aidlc-docs/construction/unit5-data-display/code/business-logic-summary.md`
      を生成する

### Step 5: API Layer Generation
- [x] 5.1 `MasterDataController`（テーブル一覧・レコード一覧・一括反映）
- [x] 5.2 `CustomizationController`（カスタマイズ定義取得・YAMLエクスポート/インポート）
- [x] 5.3 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与）
- [x] 5.4 `GlobalExceptionHandler`更新: `MasterMaintenanceException`用のハンドラを追加
- [x] 5.5（実行時に判明）`SecurityConfig`確認: 既存の`anyRequest().authenticated()`
      フォールバックが`/api/data/**`をそのまま認証済み全ユーザに許可しており、
      新規ルールの追加は不要と判明した（`SecurityConfig`自体の変更なし）
- [x] 5.6（実行時追加）`listRecords`はフィルタ条件のリストをGETクエリパラメータに
      素直にマッピングできないため、計画のGETではなくPOST「検索」エンドポイントとして
      実装した（`ListRecordsRequest`）

### Step 6: API Layer Unit Testing
- [x] 6.1 `MasterDataControllerTest`（テーブル一覧、レコード一覧、一括反映の
      actorUserId伝播・オールオアナッシング拒否時の応答）
- [x] 6.2 `CustomizationControllerTest`（カスタマイズ定義取得、YAMLエクスポート、
      YAMLインポート成功/検証エラー）

### Step 7: API Layer Summary
- [x] 7.1 `aidlc-docs/construction/unit5-data-display/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [x] 8.1 `TableCustomizationJpaRepository`/`ColumnCustomizationJpaRepository`/
      `ValidationRuleJpaRepository`（Step 2で先行生成済み）

### Step 9: Repository Layer Unit Testing
- [x] 9.1 各リポジトリの`@DataJpaTest`（カスケード削除、名前ベース検索）

### Step 10: Repository Layer Summary
- [x] 10.1
      `aidlc-docs/construction/unit5-data-display/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [x] 11.1 APIクライアント関数（`api/masterData.ts`、`api/customizations.ts`）
- [x] 11.2 `MasterDataScreen`（接続/テーブル選択、フィルタ・ソートUI、
       make-you-chic-uiの`Table`コンポーネントによるページング・ソート・インライン
       編集、反映/新規作成/削除操作）
- [x] 11.3 `CustomizationScreen`（接続選択、テーブル別カスタマイズ定義一覧、
       YAMLエクスポート/インポート）
- [x] 11.4 `App.tsx`ルーティング更新（`/data`は認証済み全ユーザ、`/data/customization`は
       `RequireAuth role="ADMIN"`配下）、`AppLayout`更新（全ロール共通navItem
       「データ表示」、ADMIN限定navItem「表示・入力カスタマイズ」を追加）
- [x] 11.5 Unit 3の`ConnectionListScreen`修正: スキーマ取込結果モーダルに
       `prunedCustomizationCount`を追加表示する
- [x] 11.6 i18nメッセージ追加（ja/en、データ表示・カスタマイズ画面の全文言・
       エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [x] 12.1 `MasterDataScreen.test.tsx`（テーブル一覧・レコード一覧表示、フィルタ、
       インライン編集からの反映送信、作成/削除の反映）
- [x] 12.2 `CustomizationScreen.test.tsx`（一覧表示、YAMLエクスポート、
       YAMLインポートエラー表示）

### Step 13: Frontend Components Summary
- [x] 13.1 `aidlc-docs/construction/unit5-data-display/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [x] 14.1 `V14__create_table_customization.sql`（Step 3のテスト前提として
      先行生成済み）
- [x] 14.2 `V15__create_column_customization.sql`（同上）
- [x] 14.3 `V16__create_validation_rule.sql`（同上）

### Step 15: Documentation Generation
- [x] 15.1 `README.md`更新（データ表示・カスタマイズ画面の概要）

### Step 16: Deployment Artifacts Generation
- [x] 16.1 デプロイ関連の環境変数追加は不要。念のため確認のみ行う

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
