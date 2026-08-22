# Unit 6: その他機能 - Functional Design Plan

## 対象範囲

- **Unit定義**: `aidlc-docs/inception/application-design/unit-of-work.md` Unit 6
- **対応ストーリー**: US-4.1〜US-4.6（クエリ関連機能、6件）、US-5.1（監査ログ閲覧、1件）
- **含まれるコンポーネント**: QueryComponent、AuditLogComponent（閲覧APIの追加実装）
- **依存Unit**: Unit 2（`AuditLogService`の記録機構、`AuditEvent`エンティティ）、Unit 3
  （`ConnectionSchemaService#isSchemaAllowed`、`ConnectionPoolRegistry`）
- **参照**: requirements.md 4.4（クエリ関連機能）・4.5（監査ログ）、component-methods.md
  QueryComponent・AuditLogComponent

## 実行計画

- [x] Step 1: ユニットコンテキスト分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [x] Step 6: Functional Design成果物生成
  - [x] `business-logic-model.md`
  - [x] `business-rules.md`
  - [x] `domain-entities.md`
  - [x] `frontend-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: `buildSql`/`parseSqlToBuilderState`の実装層

component-methods.mdはこの2メソッドをQueryComponent（バックエンド想定）に列挙するが、
実装をフロントエンド・バックエンドのどちらに置くかは未確定である。`parseSqlToBuilderState`
（任意のSQL文字列を構造化されたタブ状態へ逆変換する）は、汎用SQLパーサの実装を要する
本格的なエンジニアリングコストを伴う。

A) （推奨）両メソッドともフロントエンド（TypeScript）に実装する。`buildSql`
（タブ構成の状態→SQL文字列）は構造化データからの単純な文字列組み立てで済み、
`parseSqlToBuilderState`（SQL文字列→タブ構成の状態）はクエリビルダー自身が生成した
形式のSQL、および一般的な単純SELECT文を対象としたベストエフォートの逆変換とする
（複雑な結合・サブクエリ等は完全な逆変換を保証しない）。バックエンドは`SqlText`
（文字列）のみを扱い、SQLパーサへの依存を持たない

B) バックエンドに本格的なSQLパーサライブラリを導入し、両メソッドをJavaで実装する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: クエリ実行時の対象スキーマ適用方式

クエリビルダーで生成するSQLは「スキーマ非修飾」（対象スキーマは実行時に指定）である。

A) （推奨）JDBC標準の`Connection#setSchema(String)`を実行前に呼び出し、対象RDBMS側で
非修飾のテーブル名をそのスキーマ内で解決させる（SQL文字列の書き換えは行わない）

B) SQL文字列を解析し、テーブル名にスキーマ修飾子を機械的に付与してから実行する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 読み取り専用SQL検証方式

requirements.mdは「読み取り専用のSQLのみ実行可能」と定める。

A) （推奨）Unit 5の手入力WHERE/ORDER BY句と同じブロックリスト方式を踏襲する:
SQL文の先頭（コメント除去後）が`SELECT`または`WITH`で始まることを要求し、
セミコロン（`;`）・SQLコメント開始（`--`、`/*`）を含む入力を拒否する（複数文の
連結を防止する）。本格的なSQL構文解析による完全な読み取り専用性の保証は行わない

B) 専用のSQLパーサでSQL全体を構文解析し、DML/DDLキーワードの完全な排除を保証する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: パラメータ検出・バインディング方式

requirements.mdは「`:param`形式のパラメータ化クエリ」への対応を要求する。

A) （推奨）Spring Frameworkの`NamedParameterJdbcTemplate`は`:paramName`形式の
プレースホルダをネイティブにサポートするため、これをそのまま利用する。
`detectParameters`は`NamedParameterUtils`（Spring提供のユーティリティ）でSQL文字列を
解析し、パラメータ名一覧を抽出する（独自の正規表現実装は行わない）

B) 独自の正規表現でパラメータを検出し、実行時に`?`プレースホルダへ変換したうえで
位置ベースの`PreparedStatement`を使用する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 保存クエリの更新方法

US-4.3は「編集は作成者のみ可能」と定めるが、component-methods.mdには`saveQuery`
（新規保存）と`retireQuery`（論理非表示）のみが列挙されており、更新系の専用メソッドが
明記されていない。

A) （推奨）`saveQuery`は新規作成・既存更新の両方を担う（`SavedQueryId`を省略すれば
新規作成、指定すれば既存クエリの更新とする）。更新時は作成者本人であることを検証し、
それ以外のユーザからの更新は拒否する

B) 新規作成用`saveQuery`と更新用`updateQuery`を別メソッドとして分離する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: 大量データ取得監査ログのしきい値・適用範囲

requirements.md 4.5は「大量データ取得（閾値設定可能、デフォルト100件以上）」を
必須記録イベントとして定める。この記録はUnit 5の`listRecords`（マスタメンテナンス
一覧表示）とUnit 6の`executeQuery`（クエリ実行）の両方が対象となりうる。

A) （推奨）閾値はデフォルト100件（環境変数等で変更可能な設定値）とし、Unit 5の
`MasterMaintenanceServiceImpl#listRecords`とUnit 6の`executeQuery`の両方に、
返却件数が閾値以上の場合に大量データ取得イベントを記録する処理を追加する
（Unit 5は本Unitで追加変更が必要な既存Unitとして扱う）

B) 大量データ取得の記録はUnit 6の`executeQuery`のみを対象とし、Unit 5の
`listRecords`への遡及適用は行わない（要件の一部を本Unitのスコープ外とする）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7: 監査ログ閲覧のフィルタ範囲

`AuditEvent`エンティティは`eventType`/`actorUserId`/`targetUserId`/`occurredAt`を
実カラムとして持ち、接続ID・対象リソース・結果ステータス等は`details`のJSON
テキストカラムに格納される（Unit 2のtech-stack-decisions.md、スキーマ変更なしで
Unit間の任意イベント種別追加を可能にするための設計）。

A) （推奨）閲覧画面のフィルタ条件は実カラム（`eventType`、`actorUserId`、
`occurredAt`の期間範囲）に限定する。`details`内の接続ID等はイベント詳細の表示
（一覧の各行を展開、またはJSON整形表示）でのみ確認できるものとし、SQLレベルでの
絞込対象にはしない

B) `details`のJSONカラムに対してもSQLレベルでの部分一致検索を提供する
（RDBMS間のJSON関数差異を吸収する追加実装が必要になる）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Functional Design成果物を生成する。
