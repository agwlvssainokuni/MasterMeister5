# Logical Components — Unit 3: 対象RDBMSセットアップ

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 3で新設する論理コンポーネント
を定義する。詳細な実装（クラス名・パッケージ配置等）はCode Generationで確定する。

Question 4により、ConnectionSchemaComponent関連は`cherry.mastermeister5.connectionschema`
パッケージ配下に`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを
持つ（Unit 2の`useraccount`パッケージ構成を踏襲）。暗号化コンポーネントはQuestion 3により
Unit 1・2から続く`cherry.mastermeister5.platform.security`パッケージに配置する。

## コントローラ（REST API）

| 論理コンポーネント | 役割 |
|---|---|
| ConnectionController | 接続一覧/登録/無効化/再有効化/スキーマ取込（`/api/admin/connections/**`）。ADMINロール限定 |

## ConnectionSchemaComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| ConnectionSchemaService | ConnectionSchemaComponentの実装。`registerConnection`（登録時の接続確認を含む）/`deactivateConnection`/`reactivateConnection`/`importSchema`（スキーマ単位トランザクション、全置換、差分算出）/`getSchema`/`isSchemaAllowed` |
| ConnectionJpaRepository / SchemaJpaRepository / TableJpaRepository / ColumnJpaRepository / ForeignKeyConstraintJpaRepository | 各エンティティのJPAリポジトリ（Spring Data JPA直接利用、Unit 2と同方針） |
| ConnectionPoolRegistry | 接続ごとのHikariCPプールを遅延生成・キャッシュする（`Map<ConnectionId, HikariDataSource>`）。無効化時にプールを破棄する |
| SchemaMetadataReader | JDBC標準`DatabaseMetaData`を用いてテーブル/ビュー・カラム・主キー・外部キー制約を読み取る |
| ConnectionException | 業務例外（Unit 2の`UserAccountException`と同型のパターン。接続名重複・疎通確認失敗・DEACTIVATED状態への操作等をファクトリメソッドで表現） |

## SecurityInfrastructureComponent実装（`platform.security`パッケージ、既存を拡張）

| 論理コンポーネント | 役割 |
|---|---|
| ConnectionSecretCipher | AES-256-GCMによる`encryptConnectionSecret`/`decryptConnectionSecret`実装。IVを暗号文に連結して単一バイト列として扱う |
| ConnectionSecretProperties | 暗号鍵（環境変数由来）の設定プロパティ |

## 依存関係

```
ConnectionController → ConnectionSchemaService（全メソッド）

ConnectionSchemaService → ConnectionJpaRepository/SchemaJpaRepository/TableJpaRepository/
                          ColumnJpaRepository/ForeignKeyConstraintJpaRepository
ConnectionSchemaService → ConnectionSecretCipher（登録時の暗号化、接続時の復号）
ConnectionSchemaService → ConnectionPoolRegistry（スキーマ取込時のコネクション取得、
                          無効化時のプール破棄）
ConnectionSchemaService → SchemaMetadataReader（スキーマ取込時のメタデータ読み取り）
ConnectionSchemaService → AuditLogService（Unit 2確立済み、イベント記録）

ConnectionPoolRegistry → ConnectionSecretCipher（プール生成時の復号）
```

循環依存はない。すべて一方向の依存である。
