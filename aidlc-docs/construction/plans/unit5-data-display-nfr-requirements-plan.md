# Unit 5: データ表示 - NFR Requirements Plan

## 対象範囲

- functional-design/（business-logic-model.md、business-rules.md、domain-entities.md、
  frontend-components.md）を踏まえたNFR（性能・セキュリティ・技術選定）の確定
- 前提: 単一インスタンス構成、同時利用者数約10名（requirements.md 5章）

## 実行計画

- [x] Step 1: Functional Design成果物分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [ ] Step 6: NFR Requirements成果物生成
  - [ ] `nfr-requirements.md`
  - [ ] `tech-stack-decisions.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: 実効権限判定のバッチ化（テーブル一覧取得時のN+1回避）

`listRecords`はテーブルの全カラムについてUnit 4の実効権限を判定する必要がある。
カラムごとに`resolveEffectivePermission`を呼び出すと、キャッシュ未ヒット時に
カラム数分のDB往復が発生しうる。

A) （推奨）Unit 4の`AccessControlService`に、対象テーブルの全カラムをまとめて
判定する新規メソッド（例: `resolveEffectivePermissionsForTable`）を追加する。
内部で`findForResolution`によるDB取得を1回に抑え、各カラムの実効権限はメモリ上で
算出する（既存の`resolveEffectivePermission`の階層フォールバックロジックを
カラム単位に適用する形で再利用する）

B) 既存の`resolveEffectivePermission`をカラムごとに呼び出す（初回アクセス時に
カラム数分のDB往復が発生することを許容する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: フィルタ値のSQLインジェクション対策

A) （推奨）フィルタ条件の値は`NamedParameterJdbcTemplate`のバインドパラメータとして
渡す（値の直接埋め込みは行わない）。識別子（スキーマ名・テーブル名・カラム名）は
Unit 3の`validateIdentifier`と同じ許可文字パターンで検証してからSQL文字列に
埋め込む

B) 値も含めてすべて文字列連結でSQLを組み立てる（非推奨、SQLインジェクションの
リスクが高い）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 手入力WHERE/ORDER BY句の具体的な検証方式（US-3.3、Functional Design
Question 2の具体化）

A) （推奨）セミコロン（`;`）およびSQLコメント開始（`--`、`/*`）を含む入力を正規表現で
検出し拒否するブロックリスト方式とする。専用のSQLパーサ導入は行わない（同時利用者数
約10名規模の社内ツールであり、過剰な実装コストを避ける）

B) 専用のSQLパーサライブラリを導入し、WHERE/ORDER BY句を構文解析したうえで
安全性を検証する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: `applyChanges`の検証順序・実行方式

A) （推奨）`RecordChangeSet`内の全`RecordChange`について権限チェック・
ValidationRule検証を先に完了させたうえで、1つのDBトランザクション内で実際の
INSERT/UPDATE/DELETEを実行する（検証エラーがあればDBに一切変更を加えない）

B) 各`RecordChange`を順に検証しながら同時に実行し、エラー時にトランザクションを
ロールバックする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: レコード一覧のキャッシュ方針

A) （推奨）`RecordPage`（レコード一覧・件数）はキャッシュしない。マスタデータは
随時更新されうるため、常に最新のクエリ結果を返す。キャッシュ対象はUnit 4の
実効権限のみとする

B) 短時間（数秒程度）のレコード一覧キャッシュを導入する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: 監査ログ記録の粒度

A) （推奨）`applyChanges`は1回の呼び出し＝1監査ログイベントとして記録する
（作成/更新/削除件数のサマリを含む、Unit 3/4の粒度方針を踏襲）。カスタマイズ定義の
YAMLエクスポート/インポートも同様に1操作＝1イベントとする

B) 個々のレコード変更ごとに監査ログを記録する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Resilience / Scalability Patterns（質問なし）

nfr-requirements.mdの通り、resiliency-baseline拡張は不適用であり、単一インスタンス・
同時利用者数約10名規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入
しない（Unit 1〜4と同じ方針）。

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Requirements成果物を生成する。
