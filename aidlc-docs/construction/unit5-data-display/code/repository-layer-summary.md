# Repository Layer Summary — Unit 5: データ表示

## 生成したリポジトリ

Unit 2〜4と同じ方針（Spring Data JPAをサービス層から直接利用、ポート/アダプタの
追加抽象化なし）に従い、いずれも`JpaRepository`を継承するインタフェースのみを生成した
（Step 2でサービス層と合わせて先行生成済み）。

- `cherry.mastermeister5.mastermaintenance.repository.TableCustomizationJpaRepository`
  （`findByConnectionIdAndSchemaNameAndTableName`、`findAllByConnectionId`、
  `deleteAllByConnectionId`）
- `ColumnCustomizationJpaRepository`（`findAllByTableCustomizationId`/
  `findAllByTableCustomizationIdIn`、`findByTableCustomizationIdAndColumnName`、
  `deleteAllByTableCustomizationId`/`deleteAllByTableCustomizationIdIn`）
- `ValidationRuleJpaRepository`（`findAllByColumnCustomizationId`/
  `findAllByColumnCustomizationIdIn`、`deleteAllByColumnCustomizationIdIn`）

### 既存Unitへの変更

- Unit 4の`AccessControlService`インタフェースに新規メソッド
  `resolveEffectivePermissionsForTable`を追加（リポジトリ自体の変更はなし、
  既存の`PermissionEntryJpaRepository#findForResolution`をそのまま再利用）

## DBマイグレーション（Step 14、本Stepの前提として先行生成）

- `V14__create_table_customization.sql`（`table_customization`テーブル、
  `(connection_id, schema_name, table_name)`の複合インデックス）
- `V15__create_column_customization.sql`（`column_customization`テーブル）
- `V16__create_validation_rule.sql`（`validation_rule`テーブル）

Unit 1〜4からのバージョン連番を継続した（infrastructure-design.md）。
`TableCustomization`/`ColumnCustomization`の自然キー一意性はDB制約ではなく
アプリケーション側の全置換ロジック（インポート時に既存行を削除してから再構築）で
担保する（Unit 4の`PermissionEntry`と同じ考え方）。

## 生成したテスト

- `MasterMaintenanceRepositoriesTest`（3件。TableCustomization/ColumnCustomization/
  ValidationRuleを一体として検証。`deleteAllByConnectionId`の対象限定、
  カスケード削除に相当する手動削除の動作を確認）

Unit 1〜4と同様`@DataJpaTest` + `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
を使用した。
