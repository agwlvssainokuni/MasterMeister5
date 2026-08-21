# Unit 3: 対象RDBMSセットアップ - Functional Design Plan

## 対象範囲

- **Unit定義**: `aidlc-docs/inception/application-design/unit-of-work.md` Unit 3
- **対応ストーリー**: US-2.1, US-2.2, US-2.3（stories.md Epic 2の一部。US-2.4〜2.7は
  Unit 4の対象）
- **含まれるコンポーネント**: ConnectionSchemaComponent
- **依存Unit**: Unit 1（基盤）、Unit 2（SecurityInfrastructureComponentの
  `encryptConnectionSecret`/`decryptConnectionSecret`は宣言済みだが未実装。Unit 3で実装する）
- **参照**: requirements.md 4.2（接続管理・スキーマ取込部分）、3章（対象RDBMS:
  MySQL/MariaDB/PostgreSQL/H2、NamedParameterJdbcTemplate、コネクションプール使用）

## 実行計画

- [ ] Step 1: ユニットコンテキスト分析（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: Functional Design成果物生成
  - [ ] `business-logic-model.md`
  - [ ] `business-rules.md`
  - [ ] `domain-entities.md`
  - [ ] `frontend-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: 接続無効化からの再有効化要否（requirements.md「再有効化の要否はFunctional
Designで確定する」）

A) （推奨）Unit 2のユーザ無効化/再有効化と同様、再有効化を可能にする（双方向）。誤って
無効化した場合の復旧手段を確保する

B) 再有効化は提供しない（無効化は一方向、物理削除がない代わりの唯一の「引退」操作とする）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 2: 接続登録時のRDBMS種別・対象データベース/スキーマの指定粒度

「対象スキーマの許可リスト管理」（component-methods.md）はスキーマが接続の下に複数
存在しうることを前提にしている。PostgreSQLは1データベースに複数スキーマを持てるが、
MySQL/MariaDBは「データベース＝スキーマ」で実質1:1、H2も同様。

A) （推奨）接続登録時は「RDBMS種別・ホスト・ポート・データベース名・認証情報」のみを
指定する。スキーマ一覧はスキーマ取込操作の実行時に、接続ユーザが参照可能な範囲で
自動的に発見する（PostgreSQLは複数件、MySQL/MariaDB/H2は通常1件になる）

B) 接続登録時に対象スキーマ名も管理者が明示的に1つ指定する（1接続=1スキーマに固定し、
PostgreSQLの複数スキーマ対応は将来拡張とする）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 3: 「対象スキーマの許可リスト」の実体

クエリ実行時（Unit 6）に検証で使う「許可リスト」は何を指すか。

A) （推奨）スキーマ取込によって内部DBに保持されているスキーマ一覧＝許可リストとする。
取込済みでないスキーマは存在自体を認識しないため、自動的に対象外になる（別途の
オン/オフ管理UIは持たない）

B) 取込済みスキーマとは別に、管理者が個別に「許可」「除外」を選択できる管理画面を
Unit 3で用意する

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 4: スキーマ取込で読み取る制約情報の範囲

requirements.md「テーブル/ビュー構造（物理名、コメント、型、制約）」の「制約」の範囲。

A) （推奨）主キー（PK）・外部キー（FK）・NOT NULL制約を取り込む。UNIQUE制約・
CHECK制約・デフォルト値は対象外とする（アクセス権限モデル・マスタメンテナンス機能
（作成/削除可否判定）が必要とするのはPK/FK/NOT NULLで足りるため）

B) UNIQUE制約・デフォルト値も含め、JDBCで取得可能な制約情報をすべて取り込む

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 5: 複数RDBMS対応のためのスキーマメタデータ取得方式

A) （推奨）JDBC標準の`DatabaseMetaData` API（`getTables`/`getColumns`/`getPrimaryKeys`/
`getImportedKeys`等）を共通実装で使用する。RDBMS間の細かな差異は許容し、RDBMS別の
専用実装は持たない

B) RDBMSごとに`INFORMATION_SCHEMA`への直接クエリを実装する（より正確な情報を得られるが、
RDBMS種別ごとの実装が必要になる）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 6: 接続登録時の接続確認（テスト接続）

A) （推奨）接続登録時に実際に対象RDBMSへ接続を試行し、失敗時はエラーとして登録を
拒否する（誤った接続情報の登録を防ぐ）

B) 登録時に接続確認は行わない（接続情報をそのまま保存し、実際の疎通確認はスキーマ
取込操作時に初めて行われる）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 7: JDBCドライバの追加

A) （推奨）`mysql-connector-j`（MySQL/MariaDB互換）、`org.postgresql:postgresql`を
`backend/build.gradle.kts`に追加する。H2は既存依存で対応済み。MariaDB専用ドライバ
（`mariadb-java-client`）は使わず、MySQLドライバとの互換性に委ねる

B) MariaDB専用に`org.mariadb.jdbc:mariadb-java-client`も別途追加する

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 8: コネクションプールの構成方針

A) （推奨）登録済み接続ごとに、HikariCPのプールを遅延生成（初回アクセス時）しキャッシュ
する（`Map<ConnectionId, HikariDataSource>`）。無効化された接続のプールは破棄する

B) 接続ごとの専用プールは持たず、必要な都度DataSourceを生成してクローズする
（プーリングの利点を得られないが実装が単純）

C) Other (please describe after [Answer]: tag below)

[Answer]:

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Functional Design成果物を生成する。
