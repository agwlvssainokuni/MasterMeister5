# Unit 4: アクセス制御 - NFR Design Plan

## 対象範囲

- nfr-requirements/（nfr-requirements.md、tech-stack-decisions.md）を具体的な設計パターン・
  論理コンポーネントに落とし込む

## 実行計画

- [x] Step 1: NFR Requirements成果物分析（完了）
- [x] Step 2-4: 質問の作成・提示（本ファイル）
- [x] Step 5: 回答収集・曖昧性分析（全問A、矛盾なし）
- [ ] Step 6: NFR Design成果物生成
  - [ ] `nfr-design-patterns.md`
  - [ ] `logical-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: パッケージ構成・論理コンポーネントの配置

application-design/component-methods.mdでは、AccessControlComponentと
PermissionCacheComponentは別コンポーネントとして定義されている。

A) （推奨）新規トップレベルパッケージ`cherry.mastermeister5.accesscontrol`配下に
`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを持つ
（Unit 2/3のパッケージ構成方針を踏襲）。PermissionCacheComponentは
`cherry.mastermeister5.accesscontrol.cache`サブパッケージに実装し、Caffeine依存を
このパッケージに閉じ込める（現時点で他コンポーネントから再利用される想定はないため）

B) PermissionCacheComponentを横断的関心事として扱い、`cherry.mastermeister5.platform`
配下に独立配置する（将来的にAccessControlComponent以外からの利用を見込む）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: Caffeineキャッシュのキー構造・無効化実装パターン

A) （推奨）`PermissionCacheComponent`実装は単一の`Cache<CacheKey, EffectivePermission>`
（`CacheKey`はuserId+connectionId+resourceLevel+schemaName+tableName+columnNameを持つ
値オブジェクト）を保持する。`invalidateByUser`は該当userIdのキーのみ削除する。
`invalidateByGroup`/`invalidateByConnection`は、対象ユーザ数・接続数が少数
（同時利用者数約10名規模）であることを踏まえ、Caffeineの`asMap()`を走査して条件に
一致するキーを削除する方式とする（逆引きインデックス等の追加構造は持たない）

B) `invalidateByGroup`を高速化するため、グループID→所属ユーザIDの逆引きインデックスを
別途キャッシュ内に保持する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 実効権限解決のクエリバッチングパターン

A) （推奨）`resolveEffectivePermission`は、対象ユーザの所属グループID一覧を1クエリで
取得したうえで、`PermissionEntryRepository`に「対象ユーザ＋全所属グループ」をSubject
条件としたIN句クエリを1回発行し、該当接続・スキーマ配下の全`PermissionEntry`を
まとめて取得してからメモリ上で階層フォールバック解決（COLUMN→TABLE→SCHEMA、ユーザ優先→
グループ合成）を行う（NFR Requirements Question 3のN+1回避方針を具体化）

B) 階層（COLUMN/TABLE/SCHEMA）ごとに個別クエリを発行する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: 識別子入力検証パターンの再利用方針

Unit 3の`ConnectionSchemaServiceImpl#validateIdentifier`は、接続の`host`/`databaseName`/
`schemaNameHint`を許可文字パターン（`^[A-Za-z0-9._-]+$`）で検証する。Unit 4のYAML
インポート（`schemaName`/`tableName`/`columnName`）でも同様の検証が必要になる
（NFR Requirements Question 4）。

A) （推奨）Unit 3と同じ正規表現による検証ロジックを、Unit 4側に単純な`private static`
メソッドとして複製する（現時点では2箇所のみであり、共通化のための抽象化は行わない。
プロジェクトの既存方針＝過度な抽象化を避ける、に従う）

B) 共通バリデーションユーティリティクラス（例:
`cherry.mastermeister5.platform.validation.IdentifierValidator`）として切り出し、
Unit 3側もリファクタリングして共用する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: YAML入出力の安全性設定・DTO設計

A) （推奨）Jackson YAMLモジュール（`YAMLFactory`）は標準構成のまま使用し、ポリモーフィック
型解決機能（`enableDefaultTyping`等）は一切使わない（デフォルトで型安全なデータバインディング
のみが行われる）。YAML入出力用DTOはUnit 3の`RegisterConnectionRequest`等と同様、
record型で定義する（`PermissionExportEntry`/`PermissionImportEntry`等）

B) Jacksonの`PolymorphicTypeValidator`等、より高度な型検証設定を追加で導入する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Resilience / Scalability Patterns（質問なし）

nfr-requirements.mdの通り、resiliency-baseline拡張は不適用であり、単一インスタンス・
同時利用者数約10名規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入しない
（Unit 1〜3と同じ方針）。

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Design成果物を生成する。
