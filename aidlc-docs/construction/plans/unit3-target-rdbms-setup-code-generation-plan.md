# Code Generation Plan — Unit 3: 対象RDBMSセットアップ

## Unit Context

- **対応ストーリー**: US-2.1〜US-2.3（3件、stories.md Epic 2の一部）
- **含まれるコンポーネント**: ConnectionSchemaComponent。SecurityInfrastructureComponentの
  `encryptConnectionSecret`/`decryptConnectionSecret`実装を追加する
- **依存Unit**: Unit 1（基盤）、Unit 2（AuditLogService、SecurityConfig、
  platform.securityパッケージ）
- **後続Unitへの提供インターフェース**: `ConnectionSchemaService.getSchema`/
  `isSchemaAllowed`（Unit 4のアクセス権限画面、Unit 6のクエリビルダー/実行時検証が利用）、
  `SchemaImportResult`（Unit 5の`pruneStaleCustomizations`が消費）
- **本UnitがオーナーとなるDBエンティティ**: `TargetConnection`、`DbSchema`、`DbTable`、
  `DbColumn`、`ForeignKeyConstraint`
- **明示的なスコープ外**: アクセス権限モデル（US-2.4〜2.7）はUnit 4の対象。マスタ
  メンテナンス機能（`pruneStaleCustomizations`の実際の呼び出し）はUnit 5の対象。
  クエリ実行時の`isSchemaAllowed`呼び出しはUnit 6の対象

## 本計画で追加確定する技術選定（Design段階未決定分）

- **エンティティのJavaクラス名**: `java.sql.Connection`との名前衝突を避けるため、接続
  エンティティのJavaクラス名は`TargetConnection`とする（DBの物理テーブル名は
  `connection`のまま）。同様に`Schema`/`Table`は`jakarta.persistence.Table`アノテーション
  等との混同を避けるため、Javaクラス名を`DbSchema`/`DbTable`/`DbColumn`とする
  （物理テーブル名は`db_schema`/`db_table`/`db_column`、infrastructure-design.md参照）
- **リポジトリ層のパターン**: Unit 2と同じくSpring Data JPAリポジトリを直接利用する
  （ポート/アダプタの追加抽象化は行わない）
- **`NamedParameterJdbcTemplate`の生成方法**: 対象RDBMSごとに動的にDataSourceを構築する
  ため、Springの自動設定によるBean登録ではなく、`ConnectionPoolRegistry`が接続ごとに
  `new NamedParameterJdbcTemplate(dataSource)`を生成する

## REST APIエンドポイント一覧

| メソッド/パス | 認可 | 対応ストーリー |
|---|---|---|
| `GET /api/admin/connections` | ADMIN限定 | US-2.1〜2.3 |
| `POST /api/admin/connections` | ADMIN限定 | US-2.1 |
| `POST /api/admin/connections/{connectionId}/deactivate` | ADMIN限定 | US-2.2 |
| `POST /api/admin/connections/{connectionId}/reactivate` | ADMIN限定 | US-2.2 |
| `POST /api/admin/connections/{connectionId}/schema-import` | ADMIN限定 | US-2.3 |

## 実行ステップ

### Step 1: 依存関係・設定の追加
- [ ] 1.1 `backend/build.gradle.kts`にJDBCドライバ（`com.mysql:mysql-connector-j`、
      `org.postgresql:postgresql`、`org.mariadb.jdbc:mariadb-java-client`）を追加
- [ ] 1.2 `backend/src/main/resources/application.yml`に接続暗号鍵設定
      （`mastermeister5.security.connection-secret-key`）のプレースホルダを追加

### Step 2: Business Logic Generation
- [ ] 2.1 `TargetConnection`エンティティ（JPA、状態: ACTIVE/DEACTIVATED）
- [ ] 2.2 `RdbmsType`enum（MYSQL/MARIADB/POSTGRESQL/H2）、`ConnectionStatus`enum
- [ ] 2.3 `DbSchema`/`DbTable`/`DbColumn`/`ForeignKeyConstraint`エンティティ
- [ ] 2.4 `ConnectionSecretCipher`（AES-256-GCM、`platform.security`パッケージ、
      `SecurityInfrastructureComponent#encryptConnectionSecret`/`#decryptConnectionSecret`
      実装）
- [ ] 2.5 `ConnectionPoolRegistry`（接続ごとのHikariCPプール遅延生成・キャッシュ・破棄）
- [ ] 2.6 `SchemaMetadataReader`（JDBC `DatabaseMetaData`によるテーブル/カラム/PK/FK読取り）
- [ ] 2.7 `ConnectionException`（業務例外、`UserAccountException`と同型パターン）
- [ ] 2.8 `ConnectionSchemaService`/`ConnectionSchemaServiceImpl`（`registerConnection`
      [登録時接続確認を含む]/`deactivateConnection`/`reactivateConnection`/`importSchema`
      [スキーマ単位トランザクション、全置換、差分算出]/`getSchema`/`isSchemaAllowed`）

### Step 3: Business Logic Unit Testing
- [ ] 3.1 `ConnectionSchemaServiceImplTest`（登録重複判定、状態遷移、スキーマ取込の全置換・
      差分算出、許可リスト検証）
- [ ] 3.2 `ConnectionSecretCipherTest`（暗号化・復号の正常系）
- [ ] 3.3 `SchemaMetadataReaderTest`（H2の実DBに対する読み取り検証、`@JdbcTest`相当）
- [ ] 3.4 property-based-testing拡張（jqwik）: functional-design/business-logic-model.mdの
      「テスト対象プロパティ」4件を実装する
      - スキーマ取込全置換のInvariant
      - 接続状態遷移のInvariant
      - SchemaImportResult差分の排他性のInvariant
      - 接続パスワード暗号化・復号のInvariant（round-trip）

### Step 4: Business Logic Summary
- [ ] 4.1 `aidlc-docs/construction/unit3-target-rdbms-setup/code/business-logic-summary.md`
      を生成する

### Step 5: API Layer Generation
- [ ] 5.1 `ConnectionController`（一覧・登録・無効化・再有効化・スキーマ取込）
- [ ] 5.2 リクエスト/レスポンスDTO（record、SECURITY-05の入力検証アノテーション付与。
      ホスト名等の許可文字検証を含む）
- [ ] 5.3 `SecurityConfig`更新（既存ファイルを修正）: `/api/admin/connections/**`に
      `hasRole("ADMIN")`を追加（既存の`/api/admin/**`ルールで既にカバーされているか確認し、
      未カバーであれば追加）
- [ ] 5.4 `GlobalExceptionHandler`更新: `ConnectionException`用のハンドラを追加

### Step 6: API Layer Unit Testing
- [ ] 6.1 `ConnectionControllerTest`（登録成功/重複エラー/接続確認失敗、スキーマ取込結果の
      レスポンス、無効化/再有効化）

### Step 7: API Layer Summary
- [ ] 7.1 `aidlc-docs/construction/unit3-target-rdbms-setup/code/api-layer-summary.md`を
      生成する

### Step 8: Repository Layer Generation
- [ ] 8.1 `TargetConnectionJpaRepository`（接続名一意検索を含む）
- [ ] 8.2 `DbSchemaJpaRepository`/`DbTableJpaRepository`/`DbColumnJpaRepository`/
      `ForeignKeyConstraintJpaRepository`

### Step 9: Repository Layer Unit Testing
- [ ] 9.1 各リポジトリの`@DataJpaTest`（一意制約、検索クエリ）

### Step 10: Repository Layer Summary
- [ ] 10.1
      `aidlc-docs/construction/unit3-target-rdbms-setup/code/repository-layer-summary.md`
       を生成する

### Step 11: Frontend Components Generation
- [ ] 11.1 APIクライアント関数（`api/connections.ts`）
- [ ] 11.2 `ConnectionListScreen`（一覧、登録モーダル、スキーマ取込結果モーダル、
       無効化/再有効化/スキーマ取込操作）
- [ ] 11.3 `App.tsx`ルーティング更新（`/connections`、`RequireAuth role="ADMIN"`配下）、
       `AppLayout`更新（ADMIN限定navItem「接続管理」を追加）
- [ ] 11.4 i18nメッセージ追加（ja/en、接続管理画面の全文言・エラーメッセージ）

### Step 12: Frontend Components Unit Testing
- [ ] 12.1 `ConnectionListScreen.test.tsx`（一覧表示、登録モーダル送信、スキーマ取込結果
       表示、無効化/再有効化操作）

### Step 13: Frontend Components Summary
- [ ] 13.1 `aidlc-docs/construction/unit3-target-rdbms-setup/code/frontend-summary.md`を
       生成する

### Step 14: Database Migration Scripts
- [ ] 14.1 `V6__create_connection.sql`
- [ ] 14.2 `V7__create_db_schema.sql`
- [ ] 14.3 `V8__create_db_table.sql`
- [ ] 14.4 `V9__create_db_column.sql`
- [ ] 14.5 `V10__create_foreign_key_constraint.sql`

### Step 15: Documentation Generation
- [ ] 15.1 `README.md`更新（devenvでの対象RDBMS接続情報、`localhost`+マッピング済み
       ポートでの接続登録手順）

### Step 16: Deployment Artifacts Generation
- [ ] 16.1 `.env.example`更新（`MM5_CONNECTION_SECRET_KEY`）

## 著作権・ライセンス表記

生成する全てのソースファイル冒頭に、著作権者`agwlvssainokuni`・Apache License 2.0の
ヘッダーコメントを付与する（memory: feedback-copyright-license-header）。
