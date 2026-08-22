# Unit 6: その他機能 - NFR Requirements Plan

## 対象範囲

- functional-design/（business-logic-model.md、business-rules.md、domain-entities.md、
  frontend-components.md）を踏まえたNFR（性能・セキュリティ・技術選定）の確定
- 前提: 単一インスタンス構成、同時利用者数約10名（requirements.md 5章）

## 実行計画

- [x] Step 1: Functional Design成果物分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [x] Step 6: NFR Requirements成果物生成
  - [x] `nfr-requirements.md`
  - [x] `tech-stack-decisions.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: クエリ実行結果の上限件数・タイムアウト

任意のSELECT文を実行するため、Unit 5のページング付き一覧とは異なり、意図せず
巨大な結果セットや長時間実行が発生しうる。

A) （推奨）結果セットは最大1,000件で打ち切る（それ以上は「結果が多いため一部のみ
表示」を示すメッセージとともに先頭1,000件を返す）。実行タイムアウトは30秒とする
（`Statement#setQueryTimeout`、Unit 3の接続確認タイムアウト[5秒]より長いが、
分析用クエリの実行時間を考慮した値とする）

B) 上限・タイムアウトを設けない（同時利用者数約10名規模のため許容する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 読み取り専用の多層防御

Functional Design Question 3で確定したブロックリスト方式に加え、JDBCレベルでの
追加防御を検討する。

A) （推奨）クエリ実行に使用するJDBCコネクションに対し`Connection#setReadOnly(true)`
を設定する（多くのRDBMSドライバがこれを実際のDML/DDL拒否のヒントとして利用する。
ブロックリストと合わせた多層防御とする）

B) ブロックリストのみに依拠し、JDBCレベルの追加設定は行わない

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 大量データ取得閾値の設定方法

A) （推奨）Unit 1・2で確立済みの`.env`/環境変数方式を踏襲し、
`MM5_BULK_ACCESS_THRESHOLD`（デフォルト100）で設定可能にする。Unit 5の`listRecords`・
Unit 6の`executeQuery`の両方がこの共通設定値を参照する

B) Unit 5・Unit 6でそれぞれ独立した設定値を持つ

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: クエリ実行履歴・監査ログのページングのデフォルト件数

A) （推奨）Unit 5の`listRecords`と同じ方針（OFFSET/LIMIT方式、デフォルト50件）を
踏襲する

B) 監査ログ・クエリ履歴は独自のデフォルト件数を設定する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 監査ログ・実行履歴フィルタ列へのインデックス付与

A) （推奨）`AuditEvent`の`eventType`・`actorUserId`・`occurredAt`、
`QueryExecutionHistory`の`executedByUserId`・`connectionId`・`executedAt`に
複合/単純インデックスを追加する（Flywayマイグレーションで定義）

B) インデックスは追加しない（同時利用者数約10名規模のため許容する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Resilience / Scalability Patterns（質問なし）

nfr-requirements.mdの通り、resiliency-baseline拡張は不適用であり、単一インスタンス・
同時利用者数約10名規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入
しない（Unit 1〜5と同じ方針）。

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Requirements成果物を生成する。
