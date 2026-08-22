# Unit 5: データ表示 - Functional Design Plan

## 対象範囲

- **Unit定義**: `aidlc-docs/inception/application-design/unit-of-work.md` Unit 5
- **対応ストーリー**: US-3.1〜US-3.7（7件、stories.md Epic 3全体）
- **含まれるコンポーネント**: MasterMaintenanceComponent
- **依存Unit**: Unit 3（`ConnectionSchemaService#getSchema`/`isSchemaAllowed`、
  `SchemaImportResult`）、Unit 4（`AccessControlService#resolveEffectivePermission`、
  カラム単位の権限判定）
- **参照**: requirements.md 4.3（マスタメンテナンス機能）、component-methods.md
  MasterMaintenanceComponent

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

### Question 1: レコード識別方式（更新・削除対象の特定）

主キーを持たないテーブル（ビュー等）に対する更新・削除操作の扱いを確定する。

A) （推奨）主キーを持つテーブルのみ、レコード単位の更新・削除を許可する（主キー値の組
で対象行を一意に特定する）。主キーを持たないテーブルは一覧表示・フィルタ・ソートのみ
提供し、更新・削除操作自体を提供しない（一意特定ができないため）。作成は
US-3.5/BR相当の条件（`CREATE`補助権限＋主キーなし、または全PK列UPDATE以上）に従い、
主キーの有無を問わず提供する

B) 主キーを持たないテーブルも、全カラムの現在値をWHERE条件として更新・削除を試みる
（複数行に影響するリスクを許容する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: WHERE句・ORDER BY句手入力の安全性（US-3.3）

requirements.mdは「WHERE句・ORDER BY句の手入力は権限に関係なく利用可能」と明記する。
SQLインジェクション対策の方式を確定する。

A) （推奨）手入力されたWHERE句・ORDER BY句は、生成するSQLのWHERE/ORDER BY部分として
NamedParameterJdbcTemplateに渡す（値リテラルの直接埋め込みは行わず、識別子部分の
みをそのまま連結する）。複数SQL文の連結（`;`によるスタッキング）やコメント構文
（`--`、`/*`）を検出した場合は拒否する簡易検証を行う。手入力条件で権限のない
カラムを参照できてしまう点はrequirements.mdの明示的な仕様として許容する
（表示自体は権限のある列に限定されるため、値の直接露出はない）

B) 手入力条件で参照するカラムも含めてすべて権限チェックの対象にする
（requirements.mdの明示的な記述と矛盾するため非推奨）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 一括反映（作成・更新・削除）APIのペイロード構造（US-3.4〜3.6）

requirements.mdは「作成・更新・削除のすべての操作を単一トランザクションとして扱う
APIエンドポイントに統合する」と定める。

A) （推奨）`RecordChangeSet`は`{operation: CREATE|UPDATE|DELETE, primaryKeyValues,
columnValues}`のリストとして表現し、単一APIエンドポイント（`applyChanges`）で
操作種別混在のまま単一トランザクション処理する（オールオアナッシング、US-3.4の
受け入れ基準どおり）

B) 作成・更新・削除を別々のAPIエンドポイントに分割する
（「単一エンドポイントに統合する」というrequirements.mdの記述と矛盾するため非推奨）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: カスタマイズ定義の対象リソース特定方式

A) （推奨）Unit 3/4と同じ方針で、`DbTable`/`DbColumn`のIDではなく接続ID＋スキーマ名／
テーブル名／カラム名の文字列で対象を特定する（スキーマ再取込による全置換でIDが
変わっても定義が失効しない、Unit 4 Functional Design Question 1と同じ理由）

B) `DbTable`/`DbColumn`のIDを直接参照する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: スキーマ再取込時のカスタマイズ定義の陳腐化整理・結果表示（US-3.7）

requirements.mdは「再取込のたびに陳腐化したカスタマイズ定義エントリを自動削除し、
再取込結果画面に削除件数のサマリを表示する」と定める。

A) （推奨）Unit 3の`SchemaImportResult`（record）に`prunedCustomizationCount`
フィールドを追加し、`ConnectionSchemaServiceImpl#importSchema`から本Unitの
`MasterMaintenanceService#pruneStaleCustomizations`を呼び出した結果を格納する
（Unit 4の`PermissionCacheService#invalidateByConnection`呼び出し追加と同型の
「既存Unitへの依存追加」パターン）。Unit 3の`ConnectionListScreen`のスキーマ取込
結果モーダルにこの件数を追加表示する

B) 別画面・別APIとして、カスタマイズ定義の陳腐化状況を確認する専用の仕組みを新設する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: フィルタ条件UIの対応演算子（US-3.2）

A) （推奨）等価(=)・不等価(!=)・大小(<,<=,>,>=)・LIKE（部分一致）・
IS NULL/IS NOT NULLの基本演算子を、カラムのデータ型に応じたUIコンポーネント
（テキスト入力・数値入力・日付ピッカー等）で提供する。IN・BETWEEN等の複合演算子は
初期リリースの対象外とする

B) IN・BETWEENを含むSQLの主要な演算子をすべて初期リリースから提供する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7: 入力ウィジェット種別の列挙（表示・入力カスタマイズ）

A) （推奨）`TEXT`（既定、型自動判定）/`SELECT`（固定値リスト、選択肢はカスタマイズ
定義内に静的定義）/`CHECKBOX`（真偽値）/`DATE`（日付ピッカー）の4種類とする

B) より多くのウィジェット種別（複数選択、範囲スライダー等）を初期リリースから提供する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8: 簡易バリデーションルールの表現形式（表示・入力カスタマイズ）

A) （推奨）カスタマイズ定義内でカラムごとに`{type: REGEX, pattern}`または
`{type: RANGE, min, max}`を0〜複数個指定できる形式とする。DB制約
（NOT NULL・型）に追加する形でクライアント側・サーバ側の両方に適用する

B) 相互参照・条件付き必須等を含む、より高度なバリデーションDSLを提供する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 9: ページングの方式・デフォルト件数

A) （推奨）OFFSET/LIMIT方式のオフセットページングとし、デフォルトページサイズは
50件の固定値とする（接続ごとの変更機構は持たない）。カーソルベースページング等の
特別な最適化は行わない（requirements.md 5章「具体的な性能目標値は定めない」と
同じ方針）

B) カーソルベースページングを採用する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Functional Design成果物を生成する。
