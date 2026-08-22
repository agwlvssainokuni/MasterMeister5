# Business Logic Summary — Unit 4: アクセス制御

## 生成したクラス

### `cherry.mastermeister5.accesscontrol`（AccessControlComponent）
- `entity.UserGroup`（グループ、名前一意）、`entity.GroupMembership`
  （`(groupId, userId)`一意制約）
- `entity.PermissionEntry`（Subject×ResourcePathごとの主権限/補助権限設定。
  `DbSchema`/`DbTable`/`DbColumn`のIDではなく接続ID＋名前で対象リソースを特定、
  Functional Design Question 1）
- `entity.SubjectType`（USER/GROUP）、`entity.ResourceLevel`（SCHEMA/TABLE/COLUMN）、
  `entity.PrimaryLevel`（NONE/READ/UPDATE、順序付きenum）
- `repository.UserGroupJpaRepository`/`GroupMembershipJpaRepository`/
  `PermissionEntryJpaRepository`（`findForResolution`によるSubject×スキーマ単位の
  一括取得を含む）
- `service.AccessControlService`/`AccessControlServiceImpl`（グループ管理、権限設定
  upsert、実効権限算出アルゴリズム[ユーザ優先→グループ合成、階層フォールバック]、
  YAMLエクスポート/インポート[単一トランザクション全置換]。business-rules.md
  BR-1〜BR-21を実装）
- `service.AccessControlException`（業務例外。Unit 2/3と同型パターン）
- `service.PermissionYamlMapper`（Jackson YAMLモジュールによるYAML⇄
  `PermissionYamlDocument`/`PermissionYamlEntry`変換）
- `service.GroupSummary`/`GroupMemberView`/`PermissionEntryView`/
  `SetPrimaryPermissionCommand`/`SetAuxiliaryPermissionCommand`/
  `ImportPermissionsResult`（サービス層の入出力record）

### `cherry.mastermeister5.accesscontrol.cache`（PermissionCacheComponent）
- `CacheKey`/`EffectivePermission`（値オブジェクト）
- `PermissionCacheService`/`PermissionCacheServiceImpl`（Caffeine、TTLなし・最大
  10,000エントリ。`invalidateByUser`/`invalidateByGroup`/`invalidateByConnection`）

### 既存Unitへの変更
- Unit 3の`ConnectionSchemaServiceImpl`に`PermissionCacheService`依存を追加し、
  `importSchema`成功時に`invalidateByConnection`を呼び出すよう修正（NFR Design
  logical-components.md「Unit 3との連携」）。`connectionschema → accesscontrol`の
  一方向依存であり、循環依存にはならない
- Unit 3の`DbTableJpaRepository`に`findBySchemaIdAndTableName`を追加（Unit 4の
  レコード作成/削除可否判定における主キー列検索用）
- `AuditEventType`に`GROUP_CREATED`/`GROUP_RENAMED`/`GROUP_DELETED`/
  `GROUP_MEMBER_ADDED`/`GROUP_MEMBER_REMOVED`/`PERMISSION_CHANGED`/
  `PERMISSIONS_EXPORTED`/`PERMISSIONS_IMPORTED`を追加
- i18nメッセージ追加（`errors.group_*`/`errors.membership_*`/
  `errors.access_control_*`/`errors.permission_*`、日英2言語＋デフォルト）

## 実効権限算出アルゴリズム（business-logic-model.md Section 3）

`resolveEffectivePermission`は、対象ユーザの所属グループ＋接続・スキーマ単位で
`PermissionEntry`を1クエリで一括取得し（N+1回避、NFR Design Question 3）、
メモリ上で以下を算出する:
- 主権限: ユーザ自身の階層フォールバック（COLUMN→TABLE→SCHEMA）を最優先。ユーザに
  設定がなければ、所属グループそれぞれの階層フォールバック結果のうち最も許可的な値
  （MAX）を採用
- 補助権限: 同じ原則（TABLE→SCHEMA、グループはOR）
- レコード作成/削除可否: 対象テーブルの主キー列（Unit 3の`DbColumn`）を検索し、
  各PK列の実効主権限をUPDATE/READと比較して判定（BR-12/BR-13）

## PBT適用評価（property-based-testing拡張 PBT-01）

functional-design/business-logic-model.mdで識別した6件のうち4件をjqwikの`@Property`
で実装した（ユーザ設定のグループ優先、グループ合成のOR/MAX性、階層フォールバック、
実効主権限の3値閉包）。YAMLインポートの全置換・重複拒否の2件は、モックベースの
`@Property`化が複雑になりすぎると判断し、Unit 3と同様に例示ベーステストで実質的に
担保した。

## 生成したテスト

- `AccessControlServiceImplTest`（例示ベース14件、Property 3件）
- `PermissionCacheServiceImplTest`（例示ベース5件）
- `PermissionYamlMapperTest`（往復変換・不正YAML拒否、例示ベース2件）
- Unit 3の`ConnectionSchemaServiceImplTest`に`invalidateByConnection`呼び出し検証を
  1件追加
