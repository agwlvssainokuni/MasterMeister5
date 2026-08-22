# Unit 4: アクセス制御 - NFR Requirements Plan

## 対象範囲

- functional-design/（business-logic-model.md、business-rules.md、domain-entities.md、
  frontend-components.md）を踏まえたNFR（性能・可用性・セキュリティ・技術選定）の確定
- 前提: 単一インスタンス構成（Unit 1〜3のInfrastructure Designで確定済み。ロードバランサ・
  APIゲートウェイなし）、同時利用者数約10名（requirements.md 5章）

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

### Question 1: YAML処理ライブラリの選定

本Unitが初めてYAML入出力（US-2.5、US-2.6）を実装するUnitであり、YAMLライブラリは
プロジェクト内で未選定である。

A) （推奨）Jackson YAMLモジュール（`com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`）
を使用する。Spring BootのREST DTOマッピングで既に使っているJackson ObjectMapperベースの
パターン（record DTO＋アノテーション）をそのままYAML入出力にも適用できる

B) SnakeYAML（`org.yaml.snakeyaml`、Spring Boot依存に既に同梱）を直接使用し、
独自のマッピングロジックを実装する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: Caffeineキャッシュ（実効権限キャッシュ）の構成

`PermissionCacheComponent`はCaffeineを使用する（requirements.md）。TTL・最大サイズを
確定する。

A) （推奨）TTLは設定しない（無期限、business-logic-model.mdで定義した無効化イベント
[権限設定変更/グループ構成変更/スキーマ再取込]でのみクリアする）。最大エントリ数は
10,000件とし、あふれた場合はCaffeine標準のサイズベース退避（近似LRU）に委ねる

B) TTLを設定する（例: 30分）ことで、無効化処理の実装漏れがあってもキャッシュが
自動的に古くなりすぎないようにする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 実効権限解決のパフォーマンス方針

requirements.md 5章は「具体的な目標値は定めない」とする一般方針だが、実効権限解決は
Unit 5・6のデータ表示・クエリ実行のたびに呼ばれる可能性が高い処理である。

A) （推奨）明示的な性能目標値（ms等）は設定しない（requirements.md 5章の全体方針に従う）。
ただしキャッシュ未ヒット時の算出処理（ユーザの所属グループ取得＋該当PermissionEntry検索）
はN+1クエリを避け、対象Subject（ユーザ＋所属全グループ）分のPermissionEntryを1回の
クエリでまとめて取得してからメモリ上でフォールバック解決する設計とする

B) 実効権限解決はキャッシュ未ヒット時でも99パーセンタイルで100ms以内を明示目標とする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: YAMLインポート時のセキュリティ対策

security-baseline拡張（SECURITY-05入力検証）に基づき、YAMLインポート機能固有の対策を
確定する。

A) （推奨）YAMLパースは型安全なデータバインディング（Question 1のライブラリの標準的な
ObjectMapper/Constructor経由でのDTOマッピング）のみを使用し、任意Javaクラスの
インスタンス化を許可する設定（SnakeYAMLの`Constructor`をカスタマイズしての任意型許可等）は
用いない（YAML deserialization攻撃対策）。加えて、YAML内の`schemaName`/`tableName`/
`columnName`はUnit 3の`ConnectionSchemaServiceImpl#validateIdentifier`と同じ許可文字
パターンで検証し、不正な値を含むインポートはUnit 3同様エラーとして拒否する

B) 追加のサニタイズ・型制約は設けず、パースできた内容をそのまま信頼する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 監査ログ記録の粒度（権限設定変更・グループ操作・YAML入出力）

Unit 2/3は「個々の変更ではなく操作単位」で監査ログを記録する方針を踏襲している。

A) （推奨）権限設定変更（`setPrimaryPermission`/`setAuxiliaryPermission`呼び出し）・
グループ作成/改名/削除・所属追加/削除・YAMLエクスポート/インポートは、それぞれ
1操作＝1監査ログイベントとして記録する。YAMLインポートは反映件数のサマリを含める
（Unit 3のスキーマ取込イベントと同様の粒度）

B) 個々の`PermissionEntry`変更ごとに監査ログを記録する（YAMLインポートで100件更新されれば
100件のログが記録される）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Requirements成果物を生成する。
