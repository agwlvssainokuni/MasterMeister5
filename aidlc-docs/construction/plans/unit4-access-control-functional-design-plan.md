# Unit 4: アクセス制御 - Functional Design Plan

## 対象範囲

- **Unit定義**: `aidlc-docs/inception/application-design/unit-of-work.md` Unit 4
- **対応ストーリー**: US-2.4, US-2.5, US-2.6, US-2.7（stories.md Epic 2の残り）
- **含まれるコンポーネント**: AccessControlComponent、PermissionCacheComponent
- **依存Unit**: Unit 2（`User`エンティティ、`app_user`テーブル）、Unit 3（`TargetConnection`/
  `DbSchema`/`DbTable`/`DbColumn`エンティティ、`ConnectionSchemaService`）
- **参照**: requirements.md 4.2（アクセス権限モデル部分）、application-design/
  component-methods.md（AccessControlComponent、PermissionCacheComponent）

## 実行計画

- [x] Step 1: ユニットコンテキスト分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [ ] Step 6: Functional Design成果物生成
  - [ ] `business-logic-model.md`
  - [ ] `business-rules.md`
  - [ ] `domain-entities.md`
  - [ ] `frontend-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: ResourcePathの表現方式（スキーマ再取込によるID変動への対応）

Unit 3のスキーマ取込は「全置換」方式であり、再取込のたびに`DbSchema`/`DbTable`/`DbColumn`の
行が削除・再挿入され、IDが変わりうる（`business-rules.md`参照）。権限設定はスキーマ／
テーブル／カラムの各階層に対して行われるため、参照方式を確定する必要がある。

A) （推奨）権限設定は`TargetConnection`のID＋スキーマ名／テーブル名／カラム名の**文字列**で
対象リソースを特定する（`DbSchema`等のIDを直接参照しない）。再取込後も同名のリソースが
存在すれば権限設定は自動的に有効なまま維持される

B) `DbSchema`/`DbTable`/`DbColumn`のIDで対象リソースを特定する。再取込のたびに、Unit 3の
`SchemaImportResult`（削除された/新規に採番されたリソース情報）を使って権限設定側のID参照を
付け替える処理を本Unitで追加実装する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 権限設定データモデルの粒度

「主権限（スキーマ／テーブル／カラム階層）」と「補助権限（スキーマ／テーブル階層）」を
どのようなテーブル構成で保持するか。

A) （推奨）1テーブル（例: `permission_entry`）で、Subject（ユーザ/グループ）・
ResourcePath（接続ID＋階層種別＋スキーマ名／テーブル名／カラム名）・主権限レベル
（NONE/READ/UPDATE、null可＝未設定）・補助権限フラグ（CREATE/DELETE、階層がカラムの
行では常にnull）を1行にまとめて保持する

B) 主権限用テーブルと補助権限用テーブルを分離する（階層ごとにさらにテーブルを分ける
案も含めてOtherで自由記述可）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: Subject（ユーザ/グループ）の表現方法

権限設定・グループ所属の対象となる「ユーザまたはグループ」をどう表現するか。

A) （推奨）`subjectType`（USER/GROUP）判別カラムと`subjectId`（`User.id`または
`UserGroup.id`）の組で表現する（1テーブルに両者を混在させる、外部キー制約はDB側では
張らない）

B) ユーザ向け権限テーブルとグループ向け権限テーブルを完全に分離する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: 実効権限算出時、階層に明示設定がない場合のフォールバック規則

requirements.mdは「カラム＞テーブル＞スキーマの優先順位で実効的な主権限が決まる」と
定める。ある階層（例: カラム）に明示設定がない場合の挙動を確定する。

A) （推奨）明示設定がない階層は「未設定」として扱い、より上位（カラムが未設定ならテーブル、
テーブルも未設定ならスキーマ）の設定にフォールバックする。どの階層にも設定がなければ
`NONE`とする

B) 各階層は独立とし、明示設定がない階層は常に`NONE`とする（上位へのフォールバックは
行わない。カラム権限は必ずカラムごとに個別設定が必要）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: スキーマ再取込で削除されたテーブル/カラムに紐づく既存権限設定の扱い

Unit 3の`SchemaImportResult`は`removedTableRefs`/`removedColumnRefs`
（`"schema.table"`/`"schema.table.column"`形式の文字列参照）を返す（Unit 5の
`pruneStaleCustomizations`が消費する設計）。本Unitの権限設定エントリも同様に扱うか確定する。

A) （推奨）本Unitでは自動削除処理を実装しない。Question 1でA（名前ベース参照）を選んだ
場合、削除されたリソースへの権限設定エントリは自動的に「参照先不在」として実効権限
計算上は無害（該当リソースが存在しないため権限判定自体が発生しない）だが、DB上には
陳腐化した行として残り続ける。将来的なクリーンアップの要否は本設計の対象外とする

B) `importSchema`実行時に、削除されたテーブル/カラムに紐づく権限設定エントリを
`removedTableRefs`/`removedColumnRefs`を使って自動削除する処理を本Unitで追加実装する
（Unit 3の`ConnectionSchemaServiceImpl`または本Unit側にフック処理を追加）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: YAMLエクスポート/インポートでのSubject識別方法

「他環境への複製」（US-2.5）を目的とするため、内部DBの自動採番ID（`User.id`/
`UserGroup.id`）をYAMLに含めると環境間で不整合が生じうる。

A) （推奨）YAML内でユーザは`email`、グループは`groupName`で識別する。インポート時に
これらのキーで`User`/`UserGroup`を検索し、見つからない場合はそのエントリを検証エラー
として扱う（インポート全体を拒否する）

B) YAML内でも内部ID（`User.id`/`UserGroup.id`）をそのまま使用する（環境間複製時は
IDが一致している前提を置く）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7: インポート時の「重複エントリ」の判定基準

US-2.6は「YAML内に重複エントリが存在する場合はインポート全体を拒否する」と定める。
「重複」の判定基準を確定する。

A) （推奨）同一Subject（`subjectType`+識別子）×同一ResourcePath
（接続＋スキーマ名／テーブル名／カラム名の組）の組み合わせが2回以上出現する場合を
重複とする

B) Subjectのみ、またはResourcePathのみが同一であれば重複とする（より厳格な判定）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8: 権限設定画面（フロントエンド）の構造

管理者が主権限・補助権限を設定するUIの構造を確定する。

A) （推奨）対象接続を選択後、取込済みスキーマ/テーブル/カラムをツリー表示し、
Subject（ユーザまたはグループ）を1件選択した状態で、ツリー上の各ノードに主権限
（NONE/READ/UPDATE）・補助権限（スキーマ/テーブルのみCREATE/DELETE）を直接設定する
「ツリー＋選択中Subjectへの一括設定」形式のUIとする

B) Subject×ResourcePathの個別エントリを1件ずつ追加・編集・削除するフォーム形式の
一覧UI（表形式）とする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Functional Design成果物を生成する。
