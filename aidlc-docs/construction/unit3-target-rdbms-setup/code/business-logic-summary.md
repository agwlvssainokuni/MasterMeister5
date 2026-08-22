# Business Logic Summary — Unit 3: 対象RDBMSセットアップ

## 生成したクラス

### `cherry.mastermeister5.connectionschema`（ConnectionSchemaComponent）
- `entity.TargetConnection`（`java.sql.Connection`との名前衝突を避けた命名。JPA、状態:
  ACTIVE/DEACTIVATED）
- `entity.RdbmsType`（MYSQL/MARIADB/POSTGRESQL/H2）、`entity.ConnectionStatus`（enum）
- `entity.DbSchema`/`entity.DbTable`/`entity.DbColumn`/`entity.ForeignKeyConstraint`
  （`Schema`/`Table`という予約語・アノテーション名との衝突を避けた命名）
- `repository.TargetConnectionJpaRepository`/`DbSchemaJpaRepository`/
  `DbTableJpaRepository`/`DbColumnJpaRepository`/`ForeignKeyConstraintJpaRepository`
- `service.ConnectionSchemaService` / `service.ConnectionSchemaServiceImpl`（登録時
  接続確認、無効化/再有効化、スキーマ単位トランザクションでの全置換・差分算出、
  許可リスト検証。business-rules.md BR-1〜BR-18を実装）
- `service.ConnectionException`（業務例外。Unit 2の`UserAccountException`と同型パターン）
- `service.ConnectionPoolRegistry`（接続ごとのHikariCPプール遅延生成・キャッシュ・破棄、
  登録時テスト接続用の一時DataSource生成）
- `service.SchemaMetadataReader`（JDBC標準`DatabaseMetaData`によるテーブル/ビュー・
  カラム・主キー・外部キー読み取り。スキーマ列挙が空の場合はcatalog=schemaとして
  MySQL/MariaDB互換に対応）
- `service.DiscoveredSchema`/`DiscoveredTable`/`DiscoveredColumn`/`DiscoveredForeignKey`
  （読み取り結果の中間表現）
- `service.RegisterConnectionCommand`/`ConnectionSummary`/`SchemaImportResult`/
  `SchemaView`/`TableView`/`ColumnView`（サービス層の入出力record）

### `cherry.mastermeister5.platform.security`（SecurityInfrastructureComponent拡張）
- `ConnectionSecretCipher`（AES-256-GCM。`encryptConnectionSecret`/
  `decryptConnectionSecret`実装。Unit 1・2で確立済みのパッケージを継続利用）
- `ConnectionSecretProperties`

### 共通
- `AuditEventType`に`CONNECTION_REGISTERED`/`CONNECTION_DEACTIVATED`/
  `CONNECTION_REACTIVATED`/`SCHEMA_IMPORTED`を追加
- i18nメッセージ追加（`errors.connection_*`、日英2言語＋デフォルト）

## PBT適用評価（property-based-testing拡張 PBT-01）

functional-design/business-logic-model.mdで識別した4件のうち3件をjqwikの`@Property`で
実装した。「スキーマ取込全置換のInvariant」「SchemaImportResult差分の排他性のInvariant」は、
モックベースの`@Property`化がテストの可読性・保守性を大きく損なうと判断し、代表的な
シナリオ（初回取込、テーブル削除検出）を例示ベーステストで担保することとした。

## 生成したテスト

- `ConnectionSchemaServiceImplTest`（例示ベース9件、Property 2件）
- `ConnectionSecretCipherTest`（例示ベース2件、Property 1件）
- `SchemaMetadataReaderTest`（実H2データベースに対する例示ベース3件）
