# Unit 4: アクセス制御 - Business Rules

business-logic-model.mdのフローを支えるルール・制約・バリデーションを一覧化する。

## グループ管理

- **BR-1**: グループ名（`UserGroup.name`）は一意（全体で重複不可）
- **BR-2**: グループ削除時、当該グループの`GroupMembership`（所属関係）および
  当該グループがSubjectである`PermissionEntry`（権限設定）をすべてカスケード削除する
  （US-2.7）
- **BR-3**: 1人のユーザは複数グループに同時に所属できる（`GroupMembership`の
  `(groupId, userId)`重複は不可）

## 権限設定エントリ

- **BR-4**: `PermissionEntry`は`DbSchema`/`DbTable`/`DbColumn`のIDではなく、
  接続ID＋スキーマ名／テーブル名／カラム名の**文字列**でリソースを特定する（Question 1）。
  スキーマ再取込による全置換（Unit 3）でIDが変わっても、同名のリソースが存在する限り
  権限設定は失効しない
- **BR-5**: `resourceLevel=SCHEMA`の行は`tableName`/`columnName`が共にnull、
  `TABLE`の行は`tableName`のみ必須（`columnName`はnull）、`COLUMN`の行は両方必須
- **BR-6**: 補助権限（`auxCreate`/`auxDelete`）は`resourceLevel=SCHEMA`/`TABLE`の行にのみ
  設定できる。`COLUMN`階層の行では常にnull（requirements.md「補助権限はスキーマ／テーブル
  単位」）
- **BR-7**: 同一Subject（`subjectType`+`subjectId`）×同一ResourcePath
  （`connectionId`+`resourceLevel`+`schemaName`+`tableName`+`columnName`）の組み合わせで、
  `PermissionEntry`は高々1行のみ存在する（一意制約、Question 7の重複判定基準と一致）
- **BR-8**: スキーマ再取込で存在しなくなったテーブル/カラムに紐づく`PermissionEntry`は
  自動削除しない（Question 5）。当該行は「参照先不在」として実効権限計算上は無害だが、
  DB上には陳腐化した行として残る

## 実効権限の合成

- **BR-9**: 実効主権限は、階層フォールバック（COLUMN→TABLE→SCHEMA）をユーザ自身の
  設定内で先に適用した結果（`userOwn`）を最優先とする。`userOwn`が「未設定」の場合のみ、
  同じ階層フォールバックを各所属グループ内で適用し、最も許可的な値（UPDATE > READ >
  NONE）で合成した`groupComposed`を採用する（Question 4）
- **BR-10**: どの階層にもユーザ自身・所属グループいずれの明示設定もない場合、実効主権限は
  `NONE`とする
- **BR-11**: 補助権限も主権限と同じ「ユーザ直接設定が優先、なければグループ合成
  （OR）」の原則で算出する（フォールバックはTABLE→SCHEMAのみ、COLUMN階層は補助権限の
  対象外）
- **BR-12**: レコード作成可否は、対象テーブルの`CREATE`補助権限の実効値が真、かつ
  （主キーを持たないテーブル、または全主キー列の実効主権限が`UPDATE`以上）の場合に真
  （requirements.md）
- **BR-13**: レコード削除可否は、対象テーブルの`DELETE`補助権限の実効値が真、かつ
  主キーを持ち、かつ全主キー列の実効主権限が`READ`以上の場合に真。主キーを持たない
  テーブルは常に削除不可（requirements.md）

## 実効権限キャッシュ

- **BR-14**: 実効権限（`EffectivePermission`）は`UserId`+`ResourcePath`をキーに
  キャッシュする（component-methods.md）
- **BR-15**: 権限設定の変更・グループ構成の変更・スキーマ再取込のいずれかが発生した
  場合、関係するキャッシュエントリを無効化する（requirements.md、US-2.4）

## YAMLエクスポート・インポート

- **BR-16**: YAML内でSubjectはUSERなら`email`、GROUPなら`groupName`で識別する
  （内部の自動採番IDは使わない、Question 6）
- **BR-17**: インポート時、YAML内の`email`/`groupName`が既存の`User`/`UserGroup`に
  解決できないエントリが1件でもあれば、インポート全体を拒否する
- **BR-18**: インポート時、Subject×ResourcePathが重複するエントリが1件でもあれば、
  インポート全体を拒否する（部分適用は行わない、US-2.6）
- **BR-19**: インポートは単一トランザクション内で、対象接続の既存`PermissionEntry`を
  全削除したうえでYAML内容から再構築する（全置換方式、US-2.6）
- **BR-20**: グループ自体（`UserGroup`）はYAMLインポートで新規作成しない。インポート対象の
  グループはあらかじめUI（US-2.7）で作成済みであることが前提

## 監査ログ記録対象イベント（AuditLogComponent連携）

- **BR-21**: 以下のイベントは必ずAuditLogComponentに記録する: 権限設定変更
  （主権限/補助権限）、グループ作成/改名/削除、グループ所属追加/削除、権限設定の
  YAMLエクスポート/インポート
