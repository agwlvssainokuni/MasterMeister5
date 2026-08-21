# Unit 3: 対象RDBMSセットアップ - Business Rules

business-logic-model.mdのフローを支えるルール・制約・バリデーションを一覧化する。

## 接続登録・管理

- **BR-1**: 接続名（`name`）は一意（Connectionテーブル全体で重複不可、状態を問わない）
- **BR-2**: 接続登録時、実際に対象RDBMSへの疎通確認を行う。失敗時は登録を拒否する
  （Question 6）
- **BR-3**: 接続パスワードは可逆暗号化して保存する。平文は一切保持しない（requirements.md）
- **BR-4**: 対象RDBMS接続は物理削除を提供しない。無効化（DEACTIVATED）のみ可能
- **BR-5**: 無効化された接続は再有効化できる（双方向、Question 1）
- **BR-6**: 無効化された接続に対する新規操作（スキーマ取込等）は拒否する。既存の
  権限設定・カスタマイズ定義・保存クエリ・クエリ実行履歴・監査ログはすべて保持され続ける
- **BR-7**: `schemaNameHint`（対象スキーマ名の任意指定、Question 2追加分）を指定した場合、
  スキーマ取込はそのスキーマのみを対象とする。未指定の場合は接続ユーザが参照可能な全
  スキーマを自動発見する

## スキーマ取込

- **BR-8**: スキーマ取込は管理者による明示的な操作（スキーマ更新操作）でのみ実行される。
  自動的な定期再取込は行わない
- **BR-9**: 取込対象の制約情報は主キー（PK）・外部キー（FK）・NOT NULL制約に限る
  （UNIQUE制約・CHECK制約・デフォルト値は対象外、Question 4）
- **BR-10**: スキーマ取込のたびに、対象SchemaのTable/Column/ForeignKeyConstraintを
  全置換する（差分マージではない）
- **BR-11**: 再取込により存在しなくなったテーブル・カラムは`SchemaImportResult`の
  削除リストに含め、呼び出し元（管理画面、将来的にはUnit 5の
  `pruneStaleCustomizations`）に通知する
- **BR-12**: スキーマ取込対象のメタデータ取得はJDBC標準の`DatabaseMetaData` APIを用いる
  （RDBMS別の`INFORMATION_SCHEMA`直接クエリは実装しない、Question 5）

## 許可リスト

- **BR-13**: 内部DBにSchemaレコードとして保持されているスキーマのみが「許可されたスキーマ」
  である（Question 3）。取込を行っていないスキーマは存在自体を認識せず、
  `isSchemaAllowed`は常にfalseを返す

## コネクションプール

- **BR-14**: 接続ごとに専用のコネクションプール（HikariCP）を遅延生成し、以後のアクセスで
  再利用する（Question 8）
- **BR-15**: 接続を無効化した場合、対応するコネクションプールを破棄する（再有効化時に
  再度遅延生成される）

## JDBCドライバ

- **BR-16**: MySQL/PostgreSQL/MariaDB向けにそれぞれ専用のJDBCドライバを追加する
  （`mysql-connector-j`、`org.postgresql:postgresql`、`mariadb-java-client`、
  Question 7）。H2は既存依存で対応済み

## 監査ログ記録対象イベント（AuditLogComponent連携）

- **BR-17**: 以下のイベントは必ずAuditLogComponentに記録する: 接続登録、接続無効化、
  接続再有効化、スキーマ取込（追加/削除件数のサマリを含む）
- **BR-18**: 監査ログに接続パスワード平文を記録しない（SECURITY-03準拠）
