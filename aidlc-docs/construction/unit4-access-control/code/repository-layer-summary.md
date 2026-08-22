# Repository Layer Summary — Unit 4: アクセス制御

## 生成したリポジトリ

Unit 2/3と同じ方針（Spring Data JPAをサービス層から直接利用、ポート/アダプタの追加
抽象化なし）に従い、いずれも`JpaRepository`を継承するインタフェースのみを生成した
（Step 2でサービス層と合わせて先行生成済み）。

- `cherry.mastermeister5.accesscontrol.repository.UserGroupJpaRepository`
  （`findByName`、`findAllByOrderByNameAsc`）
- `GroupMembershipJpaRepository`（`findAllByGroupId`/`findAllByUserId`/
  `findByGroupIdAndUserId`/`countByGroupId`/`deleteAllByGroupId`/
  `deleteByGroupIdAndUserId`）
- `PermissionEntryJpaRepository`（自然キーでのupsert検索、`findForResolution`
  [Subject×スキーマ単位の一括取得、business-logic-model.md Section 3のクエリ
  バッチング]、`findAllByConnectionIdAndSubjectTypeAndSubjectId`、
  `findAllByConnectionId`、`deleteAllByConnectionId`、
  `deleteAllBySubjectTypeAndSubjectId`）

### 既存Unitへの変更

Unit 3の`DbTableJpaRepository`に`findBySchemaIdAndTableName`を追加した
（レコード作成/削除可否判定のための主キー列検索用）。

## DBマイグレーション（Step 14、本Stepの前提として先行生成）

- `V11__create_user_group.sql`
- `V12__create_group_membership.sql`
- `V13__create_permission_entry.sql`（`connection_id, schema_name, subject_type,
  subject_id`の複合インデックスを追加、`findForResolution`のクエリ性能を意識）

Unit 1〜3からのバージョン連番を継続した（infrastructure-design.md）。
`permission_entry`の一意制約はDB制約ではなくサービス層の検索upsertで担保する
（`table_name`/`column_name`がnullになりうるため、ANSI標準の一意制約ではnull列を
区別できない、domain-entities.md参照）。

## 生成したテスト

- `AccessControlRepositoriesTest`（6件。UserGroup/GroupMembership/PermissionEntryを
  一体として検証。グループ名一意制約違反、`findForResolution`のスキーマ絞り込み、
  `deleteAllByConnectionId`/`deleteAllBySubjectTypeAndSubjectId`の対象限定を確認）

Unit 1〜3と同様`@DataJpaTest` + `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
を使用した。
