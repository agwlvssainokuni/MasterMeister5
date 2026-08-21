# Tech Stack Decisions — Unit 3: 対象RDBMSセットアップ

requirements.md 3章およびUnit 1・2のNFR Requirementsで既に確定済みの技術スタックに加え、
Unit 3のNFR Requirementsで新たに確定した項目を記録する。

## 既存確定事項（参考）
- 対象RDBMSアクセス: NamedParameterJdbcTemplate（requirements.md 3章）
- 依存関係管理: Gradle `dependencyLocking` + GitHub Dependabot（Unit 1確立済み）
- 機微情報の鍵管理: `.env`/環境変数方式（Unit 1・2踏襲）

## Unit 3で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| 接続パスワード暗号化 | AES-256-GCM、鍵は環境変数由来 | Question 1: 認証付き暗号で改ざん検知も兼ねる。Unit 2のJWT署名鍵管理方式を踏襲 |
| 接続確認タイムアウト | 5秒 | Question 2: 管理者操作を長時間ブロックしない範囲での一時的遅延許容 |
| コネクションプール（HikariCP） | 最大プールサイズ5、最小アイドル1（接続ごと） | Question 3: 複数接続並行運用を前提に、接続ごとに大きなプールを持たせない |
| スキーマ取込の処理方式 | 同期処理のまま（非同期化しない） | Question 4: 同時利用者数約10名規模では不要な複雑さと判断 |
| JDBCドライバ | `com.mysql:mysql-connector-j`、`org.postgresql:postgresql`、`org.mariadb.jdbc:mariadb-java-client`を追加。H2は既存依存 | Question 5（Functional Design）/Question 5（NFR Requirements）: 既存の依存関係管理の仕組みをそのまま適用 |
| 接続エラーメッセージ | 分類コード（ホスト到達不可／認証エラー／タイムアウト／その他）のみ返す | Question 6: SECURITY-09準拠、内部詳細を露出しない |

## Unit 4以降への申し送り
- `ConnectionSchemaComponent#getSchema`/`#isSchemaAllowed`は、Unit 4（アクセス制御）が
  権限設定画面のスキーマ/テーブル/カラム階層表示に、Unit 6（クエリ機能）がクエリビルダー・
  実行時のスキーマ許可リスト検証に、それぞれ再利用する
- Unit 5（マスタメンテナンス）は、スキーマ再取込時に`SchemaImportResult`を受け取り
  `pruneStaleCustomizations`を呼び出す形でUnit 3と連携する
