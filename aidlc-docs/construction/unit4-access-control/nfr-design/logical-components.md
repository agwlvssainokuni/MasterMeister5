# Logical Components — Unit 4: アクセス制御

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 4で新設する論理コンポーネント
を定義する。詳細な実装（クラス名・パッケージ配置等）はCode Generationで確定する。

Question 1により、AccessControlComponent関連は`cherry.mastermeister5.accesscontrol`
パッケージ配下に`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを
持つ（Unit 2/3のパッケージ構成方針を踏襲）。PermissionCacheComponentは
`cherry.mastermeister5.accesscontrol.cache`サブパッケージに実装する。

## コントローラ（REST API）

| 論理コンポーネント | 役割 |
|---|---|
| GroupController | グループ一覧/作成/改名/削除、メンバー追加/削除（`/api/admin/groups/**`）。ADMINロール限定 |
| PermissionController | 主権限/補助権限設定、実効権限参照（画面表示用）、YAMLエクスポート/インポート（`/api/admin/permissions/**`、`/api/admin/connections/{connectionId}/permissions/**`）。ADMINロール限定 |

## AccessControlComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| AccessControlService | AccessControlComponentの実装。`setPrimaryPermission`/`setAuxiliaryPermission`/`createGroup`/`renameGroup`/`deleteGroup`/`addUserToGroup`/`removeUserFromGroup`/`resolveEffectivePermission`/`exportPermissions`/`importPermissions` |
| UserGroupJpaRepository / GroupMembershipJpaRepository / PermissionEntryJpaRepository | 各エンティティのJPAリポジトリ（Spring Data JPA直接利用、Unit 2/3と同方針） |
| AccessControlException | 業務例外（Unit 2の`UserAccountException`、Unit 3の`ConnectionException`と同型のパターン。グループ名重複・Subject未解決・重複エントリ・識別子不正等をファクトリメソッドで表現） |
| PermissionYamlMapper | `PermissionEntry`集合とYAML（Jackson YAMLモジュール、`PermissionExportEntry`/`PermissionImportEntry` record DTO）間の変換（Question 5） |

## PermissionCacheComponent実装（`accesscontrol.cache`サブパッケージ）

| 論理コンポーネント | 役割 |
|---|---|
| PermissionCacheService | PermissionCacheComponentの実装。Caffeineの`Cache<CacheKey, EffectivePermission>`をラップし、`getCached`/`put`/`invalidateByUser`/`invalidateByGroup`/`invalidateByConnection`を提供する（Question 2） |
| CacheKey | userId+connectionId+resourceLevel+schemaName+tableName+columnNameを持つキャッシュキー値オブジェクト |

## 依存関係

```
GroupController → AccessControlService（グループ管理系メソッド）
PermissionController → AccessControlService（権限設定・YAML入出力系メソッド）

AccessControlService → UserGroupJpaRepository/GroupMembershipJpaRepository/
                       PermissionEntryJpaRepository
AccessControlService → PermissionCacheService（resolveEffectivePermissionでのgetCached/put、
                       権限設定・グループ操作変更時のinvalidateByUser/invalidateByGroup）
AccessControlService → PermissionYamlMapper（exportPermissions/importPermissions）
AccessControlService → AuditLogService（Unit 2確立済み、イベント記録）
AccessControlService → Unit 2の User エンティティ・リポジトリ（`findByEmail`、YAML
                       インポート時のSubject解決。Unit 3がUnit 2のSecurityInfrastructure
                       Componentへ直接依存するのと同じパターン）
```

循環依存はない。すべて一方向の依存である。

### Unit 3（ConnectionSchemaComponent）との連携（既存Unitへの変更、Question 2/NFR Requirements
tech-stack-decisions.md「Unit 5・6への申し送り」参照）

requirements.mdは「スキーマ再取込のいずれかが変更された場合はキャッシュを無効化する」と
定める。この契機は`ConnectionSchemaComponent#importSchema`（Unit 3）にあるため、Unit 4の
Code Generationで`ConnectionSchemaServiceImpl`（`connectionschema`パッケージ）に
`PermissionCacheService`への依存を追加し、`importSchema`成功後に
`invalidateByConnection(connectionId)`を呼び出す変更を行う。

```
ConnectionSchemaServiceImpl（Unit 3） → PermissionCacheService（Unit 4、新規追加分の依存）
```

`accesscontrol`パッケージは`connectionschema`パッケージに依存しないため、この追加は
循環依存にならない（`connectionschema → accesscontrol`の一方向のみ）。イベント駆動
（Spring `ApplicationEventPublisher`）による疎結合化も選択肢だったが、プロジェクト内で
確立された「直接依存・直接呼び出し」パターン（AuditLogServiceの利用方法と同型）に統一する
ため、直接のサービス間呼び出しとする。
