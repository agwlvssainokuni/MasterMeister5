# Business Logic Summary — Unit 5: データ表示

## 生成したクラス

### `cherry.mastermeister5.mastermaintenance`（MasterMaintenanceComponent）
- `entity.TableCustomization`/`ColumnCustomization`/`ValidationRule`（接続ID＋
  スキーマ名／テーブル名／カラム名の文字列で対象を特定。`DbTable`/`DbColumn`のIDは
  参照しない、Functional Design Question 4）
- `entity.InputWidget`（TEXT/SELECT/CHECKBOX/DATE）、`entity.ValidationRuleType`
  （REGEX/RANGE）、`entity.SortDirection`（ASC/DESC）
- `repository.TableCustomizationJpaRepository`/`ColumnCustomizationJpaRepository`/
  `ValidationRuleJpaRepository`
- `service.MasterMaintenanceService`/`MasterMaintenanceServiceImpl`
  （`listRecords`[実効権限バッチ判定＋カスタマイズ定義マージ＋WHERE/ORDER BY生成＋
  ページング]、`applyChanges`[検証フェーズ・実行フェーズの2段階、対象RDBMSへの
  単一JDBCトランザクションによるオールオアナッシング]、`getCustomizationDefinition`/
  `exportCustomizationDefinition`/`importCustomizationDefinition`[YAML全置換]、
  `@EventListener`による`SchemaImportedEvent`購読[陳腐化整理]。business-rules.md
  BR-1〜BR-18を実装）
- `service.MasterMaintenanceException`（業務例外、既存Unitと同型パターン）
- `service.CustomizationYamlMapper`（Jackson YAMLモジュール、Unit 4の
  `PermissionYamlMapper`と同型パターン）
- `service.FilterOperator`/`FilterCondition`/`FilterCriteria`/`SortCriteria`/
  `ColumnDef`/`RecordPage`/`ChangeOperation`/`RecordChange`/`RecordChangeSet`/
  `ApplyResult`/`ImportCustomizationResult`/`TableSummary`/`ListRecordsCommand`/
  `SelectOption`/`CustomizationYaml*`（サービス層の入出力record）

### 既存Unitへの変更
- Unit 3の`connectionschema.service`パッケージに`SchemaImportedEvent`
  （`ApplicationEvent`のサブクラス）を新規追加した。`ConnectionSchemaServiceImpl`は
  `ApplicationEventPublisher`への依存を追加し、`importSchema`成功時にこのイベントを
  発行する。`connectionschema`パッケージは`mastermaintenance`パッケージを一切
  importしないため、`mastermaintenance → connectionschema`の既存依存と合わせても
  循環依存にはならない（NFR Design Question 1、イベント駆動による疎結合化）
- Unit 3の`SchemaImportResult`（record）に`prunedCustomizationCount`フィールドを
  追加した
- Unit 4の`AccessControlService`/`AccessControlServiceImpl`に
  `resolveEffectivePermissionsForTable`を追加した（既存の`findForResolution`
  呼び出し・階層フォールバックprivateメソッドを再利用し、テーブル全カラムの実効権限を
  1クエリで一括判定する。NFR Design Question 2）
- `AuditEventType`に`RECORD_CHANGES_APPLIED`/`CUSTOMIZATIONS_EXPORTED`/
  `CUSTOMIZATIONS_IMPORTED`を追加
- i18nメッセージ追加（`errors.master_data_*`/`errors.customization_yaml_*`、
  日英2言語＋デフォルト）

## 対象RDBMSへの書き込みトランザクション（applyChanges）

`applyChanges`は、内部アプリのJPAトランザクション（`@Transactional`）とは別に、
対象RDBMSへの生JDBC接続を取得し`autoCommit(false)`に切り替えたうえで、
`SingleConnectionDataSource`（`suppressClose=true`）でラップした
`NamedParameterJdbcTemplate`を使ってINSERT/UPDATE/DELETEを実行する。全件成功時のみ
`commit()`、1件でも例外が発生すれば`rollback()`する。`SingleConnectionDataSource`は
try-with-resourcesで閉じない（`close()`がラップ元のコネクションを実際に閉じてしまい、
その後の`setAutoCommit(originalAutoCommit)`が失敗するテスト失敗を実装中に検出し修正した。
外側のtry-with-resourcesが生コネクションを1回だけ確実にクローズする）。

## PBT適用評価（property-based-testing拡張 PBT-01）

functional-design/business-logic-model.mdで識別した6件のうち2件をjqwikの`@Property`で
実装した（手入力WHERE句のセミコロン検出は常に拒否される、実効権限が`NONE`のカラムは
カスタマイズ定義によらず常に非表示）。オールオアナッシング性・作成/削除可否判定の
整合性・カスタマイズ定義YAML全置換の3件は、モックベースの`@Property`化が複雑になり
すぎると判断し、Unit 3/4と同様に例示ベーステストで実質的に担保した。

## 生成したテスト

- `MasterMaintenanceServiceImplTest`（例示ベース10件、Property 2件。実際の
  H2データベースを「対象RDBMS」として使用し、SQL生成・実行の正しさをモックではなく
  実クエリで検証した）
- `CustomizationYamlMapperTest`（往復変換・不正YAML拒否、例示ベース2件）
- Unit 3の`ConnectionSchemaServiceImplTest`に`SchemaImportedEvent`発行検証を1件追加
- Unit 4の`AccessControlServiceImplTest`に`resolveEffectivePermissionsForTable`の
  バッチ判定検証を1件追加
