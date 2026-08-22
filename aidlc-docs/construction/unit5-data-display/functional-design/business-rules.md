# Unit 5: データ表示 - Business Rules

business-logic-model.mdのフローを支えるルール・制約・バリデーションを一覧化する。

## レコード表示・フィルタ・ソート

- **BR-1**: 一覧表示・SELECT対象カラムは、Unit 4の実効主権限が`READ`以上のカラムに
  限定する。`NONE`のカラムは列定義自体に含めない（US-3.1）
- **BR-2**: UIから指定するフィルタ条件は、読み取り権限のあるカラムのみを対象として
  選択可能とする（US-3.2）
- **BR-3**: 対応する比較演算子はEQ/NE/LT/LE/GT/GE/LIKE/IS_NULL/IS_NOT_NULLとする。
  IN・BETWEEN等の複合演算子は本Unitのスコープ外とする（Question 6）
- **BR-4**: 手入力WHERE句・ORDER BY句（US-3.3）は、権限チェックを経由せず利用できる
  （requirements.mdの明示的な仕様）。ただし、複数SQL文の連結（`;`）やコメント構文
  （`--`、`/*`）を含む入力は拒否する（Question 2、SQLインジェクション対策）
- **BR-5**: ソート順は、明示指定＞`TableCustomization.defaultSortColumn`＞
  取込順、の優先順位で決定する
- **BR-6**: ページングはOFFSET/LIMIT方式、デフォルトページサイズ50件の固定値とする
  （Question 9）

## レコード作成・更新・削除（オールオアナッシング）

- **BR-7**: `applyChanges`は`RecordChangeSet`内の全`RecordChange`を単一トランザクション
  として処理する。1件でも検証エラーがあれば、DBへの変更をゼロ件とする（US-3.4）
- **BR-8**: `UPDATE`は、変更対象の全カラムについて実効主権限が`UPDATE`であることを
  要する。1カラムでも`UPDATE`未満なら当該`RecordChangeSet`全体を拒否する
- **BR-9**: `CREATE`は、対象テーブルの`canCreate`（Unit 4の`resolveEffectivePermission`
  が返す。`CREATE`補助権限＋主キーなし、または全主キー列`UPDATE`以上）が真の場合のみ
  可能とする（US-3.5）
- **BR-10**: `DELETE`は、対象テーブルの`canDelete`（`DELETE`補助権限＋主キーあり＋
  全主キー列`READ`以上）が真の場合のみ可能とする。主キーを持たないテーブルは常に
  削除不可（US-3.6）
- **BR-11**: 主キーを持たないテーブルに対しては、`UPDATE`/`DELETE`操作自体を
  提供しない（対象行を一意に特定できないため、Question 1）。一覧表示・フィルタ・
  ソートは主キーの有無によらず提供する
- **BR-12**: `CREATE`/`UPDATE`の`columnValues`には、対象カラムの`ValidationRule`
  （REGEX/RANGE）を適用する。違反があれば当該`RecordChangeSet`全体を拒否する
  （Question 8）

## 表示・入力カスタマイズ

- **BR-13**: `TableCustomization`/`ColumnCustomization`は、`DbTable`/`DbColumn`の
  IDではなく接続ID＋スキーマ名／テーブル名／カラム名の文字列で対象を特定する
  （Question 4、Unit 3/4と同じ理由）
- **BR-14**: `ColumnCustomization.hidden`/`readOnly`は、Unit 4のアクセス権限モデルを
  上書きしない。READ権限のないカラムは、`hidden=false`であっても常に非表示のままと
  する（requirements.md）
- **BR-15**: 入力ウィジェット種別は`TEXT`/`SELECT`/`CHECKBOX`/`DATE`の4種類とする
  （Question 7）。未指定時はカラムのJDBCデータ型から自動判定する（真偽値型→
  `CHECKBOX`、日付/日時型→`DATE`、それ以外→`TEXT`）
- **BR-16**: カスタマイズ定義YAMLのインポートは、対象接続の既存定義を全削除した
  うえで再構築する全置換方式とする（US-3.7、Unit 3/4と同じ方式）
- **BR-17**: スキーマ再取込で削除されたテーブル/カラムに対応する
  `TableCustomization`/`ColumnCustomization`は、Unit 3の`importSchema`完了時に
  自動削除する（Question 5）。削除件数はUnit 3の`SchemaImportResult`に含め、
  スキーマ取込結果画面に表示する

## 監査ログ記録対象イベント（AuditLogComponent連携）

- **BR-18**: 以下のイベントは必ずAuditLogComponentに記録する: レコード作成/更新/削除
  （`applyChanges`実行、作成/更新/削除件数のサマリを含む）、カスタマイズ定義の
  YAMLエクスポート/インポート
