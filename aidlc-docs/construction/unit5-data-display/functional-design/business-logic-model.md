# Unit 5: データ表示 - Business Logic Model

component-methods.md記載のMasterMaintenanceComponentのメソッドを実現する業務フローを
定義する。技術非依存（具体的なAPI形式・SQL生成方式はCode Generationで確定）。

## 1. レコード一覧取得（`listRecords`、US-3.1〜US-3.3）

1. 呼び出し元が`listRecords(ConnectionId, TableRef, FilterCriteria, SortCriteria,
   Page)`を実行する
2. `isSchemaAllowed`（Unit 3）で対象スキーマが取込済みであることを検証する
3. 対象テーブルの全カラムについて、Unit 4の`resolveEffectivePermission`
   （resourceLevel=COLUMN）を呼び出し、実効主権限が`NONE`のカラムを列定義・
   SELECT対象から除外する（US-3.1「読み取り権限のないカラムは一覧に表示されない」）
4. Unit 5の`TableCustomization`/`ColumnCustomization`を取得し、表示対象カラム
   （Step 3で絞り込み済み）の表示ラベル・並び順・ウィジェット種別をマージする
   （非表示のカラムに権限があっても`hidden=true`なら除外、Question 4関連）
5. `FilterCriteria.rawWhereClause`が指定されていればそれを使用し（Question 2の
   安全性検証を適用）、そうでなければ`conditions`からWHERE句を組み立てる
   （UI指定のフィルタは読み取り権限のあるカラムのみ選択可能）
6. `SortCriteria.rawOrderByClause`が指定されていればそれを使用し、そうでなければ
   `SortCriteria`の指定、それも未指定なら`TableCustomization.defaultSortColumn`を
   使用する
7. Question 9のページング方式（OFFSET/LIMIT、デフォルト50件）でクエリを実行し、
   `RecordPage`（列定義・行データ・総件数）を返す

## 2. 一括反映（`applyChanges`、US-3.4〜US-3.6）

1. 呼び出し元が`applyChanges(ConnectionId, TableRef, RecordChangeSet)`を実行する
2. 単一トランザクション内で、`RecordChangeSet`内の各`RecordChange`を検証する:
   - `UPDATE`: `columnValues`に含まれる全カラムについて実効主権限が`UPDATE`で
     あることを確認する。1カラムでも`UPDATE`未満なら検証エラー
   - `CREATE`: `resolveEffectivePermission`（resourceLevel=TABLE）の`canCreate`が
     真であることを確認する（Unit 4のBR-12ロジック。US-3.5の受け入れ基準）
   - `DELETE`: 同様に`canDelete`が真であることを確認する（US-3.6）
   - Question 8のValidationRule（REGEX/RANGE）をCREATE/UPDATEの`columnValues`に
     適用する
3. 検証を1件でも通過しない場合、トランザクション全体を中断しDBへの変更をゼロ件と
   する（オールオアナッシング、US-3.4受け入れ基準）
4. 全件の検証を通過した場合のみ、`CREATE`→`INSERT`、`UPDATE`→主キー値をWHERE条件
   とした`UPDATE`、`DELETE`→主キー値をWHERE条件とした`DELETE`を生成し、対象RDBMSへ
   実行する
5. AuditLogComponentに反映イベント（作成/更新/削除件数のサマリ）を記録する
6. `ApplyResult`（作成/更新/削除件数）を返す

## 3. カスタマイズ定義の取得（`getCustomizationDefinition`）

1. `getCustomizationDefinition(ConnectionId, TableRef)`は、指定テーブルの
   `TableCustomization`/`ColumnCustomization`/`ValidationRule`を階層構造で返す
   （管理者ダッシュボードのカスタマイズ定義編集・エクスポート用）

## 4. カスタマイズ定義のYAMLエクスポート・インポート（US-3.7）

1. `exportCustomizationDefinition(ConnectionId)`は、指定接続に紐づく全
   `TableCustomization`（配下の`ColumnCustomization`/`ValidationRule`を含む）を
   YAML化して返す
2. `importCustomizationDefinition(ConnectionId, YamlDocument)`は、YAML内の
   `schemaName`/`tableName`/`columnName`を検証（Unit 3/4と同じ許可文字パターン）した
   うえで、指定接続に紐づく既存のカスタマイズ定義を全削除し、YAMLの内容から
   再構築する（全置換、Unit 3/4と同じ方式）。ウィジェット種別・バリデーション
   ルール種別が列挙値の範囲外の場合はインポート全体を拒否する
3. `ImportResult`（取込テーブル件数）を返す

## 5. スキーマ再取込時の陳腐化整理（`pruneStaleCustomizations`、US-3.7、Question 5）

1. Unit 3の`importSchema`が完了すると、削除されたテーブル・カラムの参照
   （`SchemaImportResult`の`removedTableRefs`/`removedColumnRefs`、
   `"schema.table"`/`"schema.table.column"`形式）を本メソッドに引き渡す
   （具体的な連携方式はNFR Designで確定する）
2. `removedTableRefs`に一致する`TableCustomization`（配下の`ColumnCustomization`/
   `ValidationRule`を含む）を削除する
3. `removedColumnRefs`に一致する`ColumnCustomization`（配下の`ValidationRule`を
   含む）を削除する（テーブル自体は存在する場合）
4. `PruneResult`（削除件数）を返し、Unit 3の`SchemaImportResult.
   prunedCustomizationCount`に反映される。スキーマ取込結果画面
   （Unit 3の`ConnectionListScreen`）に表示される

## テスト対象プロパティ（PBT-01: property-based-testing拡張）

| 対象 | カテゴリ | プロパティ |
|---|---|---|
| WHERE/ORDER BY生成のSQLインジェクション耐性 | Invariant | 任意の識別子・フィルタ値の組み合わせに対し、生成されるSQLは常に単一のSELECT文であり、複数文の連結（`;`によるスタッキング）を含まない |
| 一括反映のオールオアナッシング性 | Invariant | `RecordChangeSet`内の任意の1件が検証エラーとなる場合、DBへの変更は常にゼロ件である |
| レコード作成可否の判定 | Invariant | `canCreate`が真になるのは、`CREATE`補助権限が真、かつ（主キーなし、または全主キー列が`UPDATE`以上）の場合に限られる（Unit 4のBR-12との整合） |
| レコード削除可否の判定 | Invariant | `canDelete`が真になるのは、`DELETE`補助権限が真、かつ主キーを持ち、かつ全主キー列が`READ`以上の場合に限られる（Unit 4のBR-13との整合） |
| カスタマイズ定義の権限非上書き | Invariant | READ権限のないカラムは、`ColumnCustomization.hidden`の値によらず常に列定義から除外される |
| カスタマイズ定義YAML全置換 | Invariant | インポート成功後の`TableCustomization`集合は、常にYAML内容と一致する |
