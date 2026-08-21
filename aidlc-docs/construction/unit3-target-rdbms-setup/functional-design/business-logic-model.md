# Unit 3: 対象RDBMSセットアップ - Business Logic Model

component-methods.md記載のConnectionSchemaComponentのメソッドを実現する業務フローを定義する。
技術非依存（具体的なAPI形式・DB方式はCode Generationで確定）。

## 1. 接続登録（US-2.1）

1. 管理者が`registerConnection(ConnectionConfig)`を実行する（name, rdbmsType, host, port,
   databaseName, schemaNameHint（任意）, username, password）
2. 接続名（name）の重複を検証する
3. Question 6により、実際に対象RDBMSへの接続を試行する（JDBCドライバでの疎通確認）。
   失敗した場合は登録を拒否し、エラー内容（タイムアウト／認証エラー／不明ホスト等の
   一般的な分類）を返す（機微な内部エラー詳細は返さない、SECURITY-09）
4. 接続確認成功後、`SecurityInfrastructureComponent.encryptConnectionSecret`でパスワードを
   暗号化し、Connectionレコードを状態ACTIVEで保存する
5. AuditLogComponentに接続登録イベントを記録する
6. 接続IDを返す

## 2. 接続無効化・再有効化（US-2.2、Question 1）

1. 管理者が`deactivateConnection(ConnectionId)` / `reactivateConnection(ConnectionId)`
   （Question 1により双方向）を実行する
2. Connectionのstatusを DEACTIVATED / ACTIVE に更新する
3. `deactivateConnection`実行時、当該接続用に生成済みのコネクションプール（Question 8）が
   あれば破棄する
4. AuditLogComponentに無効化/再有効化イベントを記録する

無効化された接続に対する`importSchema`等の新規操作は拒否する（状態ガード。Unit 2の
`UserAccountException.userNotActive`と同様のパターンをConnectionSchemaComponent用の
例外として用意する）。

## 3. スキーマ取込（US-2.3）

1. 管理者が`importSchema(ConnectionId)`を実行する
2. 接続がACTIVEであることを検証する（DEACTIVATEDなら拒否）
3. コネクションプール（Question 8: 接続ごとにHikariCPプールを遅延生成・キャッシュ）から
   コネクションを取得する
4. JDBC標準の`DatabaseMetaData`（Question 5）で、`schemaNameHint`が指定されていればその
   スキーマのみ、未指定であれば接続ユーザが参照可能な全スキーマを対象に、テーブル/ビュー・
   カラム・主キー・外部キー制約（Question 4）を読み取る
5. 対象Schema（複数取込時はSchemaごと）について、既存のTable/Column/
   ForeignKeyConstraintと新たに読み取った内容を突き合わせ、
   - 新規に現れたテーブル・カラムを「追加」
   - 既存にあり新しい読み取り結果に存在しないテーブル・カラムを「削除」
   として差分（`SchemaImportResult`）を算出する
6. 対象SchemaのTable/Column/ForeignKeyConstraintを新しい読み取り結果で全置換する
7. Schemaの`importedAt`を更新する（新規に発見したSchemaは新規作成）
8. AuditLogComponentにスキーマ取込イベントを記録する（対象接続、追加/削除件数のサマリ）
9. `SchemaImportResult`（追加/削除されたテーブル・カラムの一覧、サマリ件数）を返す。
   US-2.3の受け入れ基準にある「削除件数のサマリを再取込結果画面に表示する」に対応する

## 4. スキーマ情報参照・許可リスト検証

1. `getSchema(ConnectionId)`は、指定接続に紐づく全Schema/Table/Column/
   ForeignKeyConstraintを階層構造で返す（アクセス権限画面・マスタメンテナンス画面・
   クエリビルダーが利用する、Unit 4〜6向けのインターフェース）
2. `isSchemaAllowed(ConnectionId, SchemaName)`は、指定接続に指定スキーマ名のSchemaレコードが
   存在するかを返す（Question 3: 取込済み＝許可リスト）

## テスト対象プロパティ（PBT-01: property-based-testing拡張）

| 対象 | カテゴリ | プロパティ |
|---|---|---|
| スキーマ取込の全置換 | Invariant | 取込後のTable/Column集合は、直前の読み取り結果と常に一致する（取込前の残存データが混在しない） |
| 接続の状態遷移 | Invariant | ACTIVE⇄DEACTIVATED以外の遷移は存在しない（Unit 2のUser状態遷移と同型のプロパティ） |
| SchemaImportResultの差分算出 | Invariant | 「追加」と「削除」の集合は常に排他（同一テーブル/カラムが両方に同時に含まれることはない） |
| 接続パスワードの暗号化・復号 | Invariant | 任意のパスワード文字列に対し、`decryptConnectionSecret(encryptConnectionSecret(p)) = p`が常に成立する（Unit 2のパスワードハッシュ検証と同種のround-tripプロパティ） |
