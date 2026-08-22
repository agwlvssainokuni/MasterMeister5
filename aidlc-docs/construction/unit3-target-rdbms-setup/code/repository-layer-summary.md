# Repository Layer Summary — Unit 3: 対象RDBMSセットアップ

## 生成したリポジトリ

Unit 2と同じ方針（Spring Data JPAをサービス層から直接利用、ポート/アダプタの追加抽象化
なし）に従い、いずれも`JpaRepository`を継承するインタフェースのみを生成した（Step 2で
サービス層と合わせて先行生成済み）。

- `cherry.mastermeister5.connectionschema.repository.TargetConnectionJpaRepository`
  （`findByName`、`findAllByOrderByCreatedAtAsc`）
- `DbSchemaJpaRepository`（`findAllByConnectionId`、`findByConnectionIdAndSchemaName`）
- `DbTableJpaRepository`（`findAllBySchemaId`、`deleteAllBySchemaId`）
- `DbColumnJpaRepository`（`findAllByTableId`、`findAllByTableIdIn`、
  `deleteAllByTableIdIn`）
- `ForeignKeyConstraintJpaRepository`（`deleteAllByFromTableIdIn`）

## DBマイグレーション（Step 14、本Stepの前提として先行生成）

- `V6__create_connection.sql`（`connection`テーブル）
- `V7__create_db_schema.sql`
- `V8__create_db_table.sql`
- `V9__create_db_column.sql`
- `V10__create_foreign_key_constraint.sql`

Unit 1・2からのバージョン連番を継続した（infrastructure-design.md）。`schema`/`table`は
SQL予約語のため、物理テーブル名は`db_schema`/`db_table`とした。

## 生成したテスト

- `TargetConnectionJpaRepositoryTest`（2件。接続名一意制約違反の検証を含む）
- `SchemaHierarchyJpaRepositoryTest`（3件。DbSchema/DbTable/DbColumn/
  ForeignKeyConstraintを一体として検証。`deleteAllBySchemaId`/`deleteAllByTableIdIn`が
  対象外の行に影響しないことを確認）

いずれもUnit 1・2と同様`@DataJpaTest` + `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
を使用した。
