# Unit 5: データ表示 - NFR Design Plan

## 対象範囲

- nfr-requirements/（nfr-requirements.md、tech-stack-decisions.md）を具体的な設計パターン・
  論理コンポーネントに落とし込む

## 実行計画

- [x] Step 1: NFR Requirements成果物分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [x] Step 6: NFR Design成果物生成
  - [x] `nfr-design-patterns.md`
  - [x] `logical-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: Unit 3からUnit 5への陳腐化整理呼び出し方式（重要: 循環依存回避）

Unit 5の`MasterMaintenanceComponent`はUnit 3の`ConnectionSchemaService#getSchema`等に
依存する（`mastermaintenance → connectionschema`、正常な方向）。一方、Unit 3の
`importSchema`完了時に陳腐化したカスタマイズ定義を削除する
（`pruneStaleCustomizations`）ためには、`ConnectionSchemaServiceImpl`から
Unit 5を呼び出す経路が必要になる。Unit 4の`PermissionCacheService`呼び出し追加
（logical-components.md「Unit 3との連携」）と異なり、`accesscontrol`パッケージは
`connectionschema`に一切依存していなかったため一方向のままだったが、本Unitで同じ
直接呼び出しパターンを採用すると`connectionschema ⇄ mastermaintenance`の
**循環依存**が生じてしまう。

A) （推奨）Spring `ApplicationEventPublisher`を用いる。`ConnectionSchemaServiceImpl`は
`connectionschema.service`パッケージに定義する`SchemaImportedEvent`
（`connectionId`、`removedTableRefs`、`removedColumnRefs`、書き込み可能な
`prunedCustomizationCount`結果フィールドを保持）を発行するのみとする。Unit 5の
`MasterMaintenanceServiceImpl`が`@EventListener`でこれを購読し陳腐化整理を実行、
結果を`SchemaImportedEvent`に書き戻す。Spring既定の同期リスナー実行（同一
トランザクション内、`@Async`は使わない）により、`ConnectionSchemaServiceImpl`は
`publishEvent`呼び出し直後にイベントの結果フィールドを読み取り
`SchemaImportResult.prunedCustomizationCount`に反映できる。これにより
`connectionschema`パッケージは`mastermaintenance`パッケージに一切依存しない

B) Unit 4と同じ直接依存パターン（`ConnectionSchemaServiceImpl`が
`MasterMaintenanceService`を直接呼び出す）を採用し、`connectionschema ⇄
mastermaintenance`の循環依存を許容する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 実効権限バッチ判定の内部実装パターン

A) （推奨）`AccessControlService#resolveEffectivePermissionsForTable`は、対象ユーザの
所属グループ一覧取得＋`PermissionEntryRepository#findForResolution`によるスキーマ単位の
一括取得（既存の`resolveEffectivePermission`と同じ1回のクエリ）を行った後、渡された
カラム名それぞれについて既存の階層フォールバックアルゴリズム（`buildChain`/
`findMatch`等の既存privateメソッド）をメモリ上で繰り返し適用し、
`Map<String, EffectivePermission>`を返す

B) カラムごとに内部で`resolveEffectivePermission`を呼び出す（結果としてクエリが
1回に集約されない）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 手入力WHERE/ORDER BY句のブロックリスト実装

A) （推奨）正規表現`;`（セミコロン）、`--`（行コメント）、`/\*`（ブロックコメント
開始）のいずれかを含む入力を拒否する。この検証はUnit 3の`validateIdentifier`と同様の
専用privateメソッドとして`mastermaintenance.service`パッケージに実装する
（共通化のための抽象化は行わない、プロジェクトの既存方針を踏襲）

B) より高度な字句解析（トークナイザ）による検証を行う

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: ValidationRule（簡易バリデーション）の実行層

A) （推奨）サーバ側（`MasterMaintenanceServiceImpl#applyChanges`）で最終検証として
必ず実行する。クライアント側（フロントエンド）でも同じルールを使った補助的な
即時フィードバックを提供するが、あくまでUX向上目的であり、サーバ側検証を
省略する根拠にはしない（多層防御、Unit 2のパスワード検証と同じ考え方）

B) サーバ側検証のみ提供し、クライアント側では行わない

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: SQL生成・パラメータバインディングの実装パターン

A) （推奨）`listRecords`/`applyChanges`とも、`NamedParameterJdbcTemplate`と
`MapSqlParameterSource`を用いてカラム値をバインドパラメータとして渡す。
テーブル名・カラム名等の識別子はUnit 3の許可文字パターンで検証済みの文字列を
そのままSQL文字列に連結する（識別子はバインドパラメータにできないため）

B) 独自のSQLビルダーライブラリを導入する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Resilience / Scalability Patterns（質問なし）

nfr-requirements.mdの通り、resiliency-baseline拡張は不適用であり、単一インスタンス・
同時利用者数約10名規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入
しない（Unit 1〜4と同じ方針）。

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Design成果物を生成する。
