# Unit 6: その他機能 - Business Rules

business-logic-model.mdのフローを支えるルール・制約・バリデーションを一覧化する。

## クエリビルダー

- **BR-1**: `buildSql`/`parseSqlToBuilderState`はフロントエンドに実装し、バックエンドは
  関与しない（Question 1）

## 保存クエリ

- **BR-2**: `SavedQuery`は物理削除を提供しない。`retireQuery`による論理非表示
  （`status=RETIRED`）のみ可能とする（US-4.4）
- **BR-3**: `saveQuery`（更新）・`retireQuery`は、対象`SavedQuery`の`creatorUserId`が
  呼び出し元と一致する場合のみ許可する。一致しない場合は拒否する（US-4.3）
- **BR-4**: `visibility=PRIVATE`の保存クエリは、`creatorUserId`と異なるユーザからの
  `executeQuery`呼び出しを拒否する。一覧表示（クエリ選択UI）でも作成者以外には
  表示しない
- **BR-5**: `RETIRED`状態の保存クエリは一覧表示から除外されるが、
  `QueryExecutionHistory.savedQueryId`からの参照は保持され続ける（物理削除がないため
  参照整合性の問題自体が発生しない）

## クエリ実行

- **BR-6**: 実行対象のSQLは、コメント除去後の先頭が`SELECT`または`WITH`であることを
  要する。それ以外は拒否する
- **BR-7**: 実行対象のSQLにセミコロン（`;`）またはSQLコメント開始（`--`、`/*`）が
  含まれる場合は拒否する（複数文の連結防止、Unit 5のQuestion 3と同じ方針）
- **BR-8**: 実行対象スキーマは、Unit 3の`isSchemaAllowed`が真を返すスキーマ
  （取込済みスキーマ）に限定する
- **BR-9**: スキーマの適用はJDBC標準の`Connection#setSchema`で行い、SQL文字列の
  書き換えは行わない（Question 2）
- **BR-10**: パラメータのバインディングはSpring `NamedParameterJdbcTemplate`の
  `:paramName`形式をそのまま利用する（Question 4）

## クエリ実行履歴

- **BR-11**: `QueryExecutionHistory`は実行のたびに1件作成する（保存クエリ経由・
  直接入力を問わない）。`sqlText`は実行時点のスナップショットとし、保存クエリの
  事後更新の影響を受けない
- **BR-12**: 実行履歴の閲覧は認証済み全ユーザに許可する（BR-4のPRIVATE制約は
  「実行」にのみ適用し、「履歴の閲覧」には適用しない。Functional Design Section 6の
  設計判断）

## 監査ログ

- **BR-13**: 「クエリ実行」イベントは、`executeQuery`の呼び出しのたびに必ず記録する
  （結果件数・実行時間を含む）
- **BR-14**: 「大量データ取得」イベントは、結果件数が閾値（デフォルト100件、環境変数等で
  変更可能）以上の場合に、「クエリ実行」イベントとは別に追加で記録する
- **BR-15**: 「大量データ取得」の閾値判定・記録は、Unit 6の`executeQuery`だけでなく
  Unit 5の`MasterMaintenanceServiceImpl#listRecords`にも適用する（Question 6、
  既存Unitへの変更）
- **BR-16**: 監査ログ（`AuditEvent`）の閲覧は管理者のみ許可する（US-5.1、既存の
  `/api/admin/**` → `hasRole("ADMIN")`ルールでカバーされる）
- **BR-17**: 監査ログのフィルタ条件は実カラム（`eventType`/`actorUserId`/
  `occurredAt`の期間）に限定する。`details`のJSON内容はSQLレベルの絞込対象にしない
  （Question 7）
- **BR-18**: 監査ログはアプリケーションコードから更新・削除できない
  （SECURITY-14、Unit 2で確立済みの`AuditLogService`の制約をそのまま維持する。
  本Unitは閲覧専用のAPIのみ追加する）

## 監査ログ記録対象イベント（本Unitで新規追加）

- **BR-19**: 以下のイベントを新規に記録する: クエリ保存（新規/更新の区別を含む）、
  クエリ論理非表示、クエリ実行（結果件数・実行時間を含む）、大量データ取得
  （Unit 5・Unit 6の両方が対象）
