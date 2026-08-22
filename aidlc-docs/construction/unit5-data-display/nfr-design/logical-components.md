# Logical Components — Unit 5: データ表示

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 5で新設する論理コンポーネント
を定義する。詳細な実装（クラス名・パッケージ配置等）はCode Generationで確定する。

`MasterMaintenanceComponent`関連は`cherry.mastermeister5.mastermaintenance`パッケージ
配下に`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを持つ
（Unit 2〜4のパッケージ構成方針を踏襲）。

## コントローラ（REST API）

| 論理コンポーネント | 役割 |
|---|---|
| MasterDataController | レコード一覧取得・一括反映（`/api/data/**`）。認証済みユーザ全員がアクセス可能（ロール制限なし、画面内でUnit 4の実効権限に基づき表示・操作を制御） |
| CustomizationController | カスタマイズ定義取得・YAMLエクスポート/インポート（`/api/admin/connections/{connectionId}/customizations/**`）。ADMINロール限定 |

## MasterMaintenanceComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| MasterMaintenanceService | MasterMaintenanceComponentの実装。`listRecords`（実効権限バッチ判定＋カスタマイズ定義マージ＋WHERE/ORDER BY生成＋ページング）/`applyChanges`（検証フェーズ・実行フェーズの2段階、オールオアナッシング）/`getCustomizationDefinition`/`exportCustomizationDefinition`/`importCustomizationDefinition`（全置換）/`@EventListener`によるスキーマ再取込時の陳腐化整理 |
| TableCustomizationJpaRepository / ColumnCustomizationJpaRepository / ValidationRuleJpaRepository | 各エンティティのJPAリポジトリ（Spring Data JPA直接利用、Unit 2〜4と同方針） |
| MasterMaintenanceException | 業務例外（`ConnectionException`/`AccessControlException`と同型パターン。検証失敗・識別子不正・オールオアナッシング拒否等をファクトリメソッドで表現） |
| CustomizationYamlMapper | `TableCustomization`集合⇔YAML DTO変換（Jackson YAMLモジュール、Unit 4で確立済みの依存を再利用） |
| RecordQueryBuilder（内部ヘルパー） | `FilterCriteria`/`SortCriteria`からSQL WHERE/ORDER BY句を構築する（ブロックリスト検証・バインドパラメータ組立を含む） |

## 既存Unitへの変更

### Unit 3（`connectionschema`パッケージ）

| 変更対象 | 内容 |
|---|---|
| `SchemaImportResult` | `prunedCustomizationCount`フィールドを追加（record） |
| `ConnectionSchemaServiceImpl` | `ApplicationEventPublisher`への依存を追加。`importSchema`内で`SchemaImportedEvent`を発行し、結果を`SchemaImportResult`に反映する |
| （新規）`SchemaImportedEvent` | `connectionId`/`removedTableRefs`/`removedColumnRefs`（不変）＋`prunedCustomizationCount`（リスナーが書き込む可変フィールド） |
| `ConnectionListScreen`（フロントエンド） | スキーマ取込結果モーダルに`prunedCustomizationCount`を追加表示 |

### Unit 4（`accesscontrol`パッケージ）

| 変更対象 | 内容 |
|---|---|
| `AccessControlService` | `resolveEffectivePermissionsForTable(Long userId, Long connectionId, String schemaName, String tableName, List<String> columnNames)`メソッドを追加（戻り値`Map<String, EffectivePermission>`） |
| `AccessControlServiceImpl` | 上記メソッドを実装。既存の`findForResolution`呼び出し・階層フォールバックprivateメソッドを再利用し、クエリを1回に集約する |

## 依存関係

```
MasterDataController → MasterMaintenanceService（listRecords/applyChanges）
CustomizationController → MasterMaintenanceService（カスタマイズ定義系メソッド）

MasterMaintenanceService → TableCustomizationJpaRepository/
                           ColumnCustomizationJpaRepository/ValidationRuleJpaRepository
MasterMaintenanceService → CustomizationYamlMapper（exportCustomizationDefinition/
                           importCustomizationDefinition）
MasterMaintenanceService → AuditLogService（Unit 2確立済み、イベント記録）
MasterMaintenanceService → Unit 3のConnectionSchemaService（getSchema、
                           isSchemaAllowed）、ConnectionPoolRegistry（対象RDBMSへの
                           JDBC接続取得）
MasterMaintenanceService → Unit 4のAccessControlService
                           （resolveEffectivePermissionsForTable、resolveEffectivePermission）

MasterMaintenanceService --（@EventListener）--> connectionschema.service.SchemaImportedEvent
                           （connectionschemaパッケージの型を購読するのみ。
                           mastermaintenance → connectionschemaの一方向）

ConnectionSchemaServiceImpl --（publishEvent）--> SchemaImportedEvent
                           （connectionschema自身が定義するイベント型を発行するのみ。
                           mastermaintenanceパッケージへの依存は発生しない）
```

`connectionschema`パッケージは`mastermaintenance`パッケージの型を一切importしない
ため、循環依存は生じない（NFR Design Question 1）。
