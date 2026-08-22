# Unit 6: その他機能 - NFR Design Plan

## 対象範囲

- nfr-requirements/（nfr-requirements.md、tech-stack-decisions.md）を具体的な設計パターン・
  論理コンポーネントに落とし込む

## 実行計画

- [ ] Step 1: NFR Requirements成果物分析（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: NFR Design成果物生成
  - [ ] `nfr-design-patterns.md`
  - [ ] `logical-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: パッケージ構成・論理コンポーネントの配置

A) （推奨）`QueryComponent`関連は新規トップレベルパッケージ
`cherry.mastermeister5.query`配下に`entity`/`repository`/`service`/`controller`の
レイヤーサブパッケージを持つ（Unit 2〜5のパッケージ構成方針を踏襲）。
`AuditLogComponent`の閲覧API追加は、Unit 2で確立済みの`cherry.mastermeister5.audit`
パッケージに`controller`サブパッケージを新設して実装する（既存コンポーネントへの
機能追加のため、新規トップレベルパッケージは作らない）

B) 監査ログ閲覧APIも`query`パッケージに含める

C) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 2: クエリ実行の上限・タイムアウト・読み取り専用設定の実装パターン

A) （推奨）`JdbcTemplate`の標準機能（`setMaxRows(1000)`、`setQueryTimeout(30)`）を
使用し、独自のJDBC制御コードは書かない。この`JdbcTemplate`を
`NamedParameterJdbcTemplate`でラップして`:paramName`バインディングを行う。
読み取り専用は、実行前に生JDBCコネクションへ`setReadOnly(true)`を設定したうえで
上記`JdbcTemplate`に渡すことで多層防御とする

B) `java.sql.Statement`を直接操作し、`setMaxRows`/`setQueryTimeout`を手動で設定する

C) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 3: `AuditLogService`インタフェースの拡張方法

A) （推奨）既存の`listEvents(Pageable)`はそのまま残し、新規オーバーロード
`listEvents(AuditEventFilterCriteria, Pageable)`を追加する（Unit 4の
`AccessControlService#resolveEffectivePermissionsForTable`追加と同じ「既存Unitへの
非破壊的なメソッド追加」パターン）

B) 既存の`listEvents(Pageable)`を`AuditEventFilterCriteria`必須の形に置き換える
（既存呼び出し元がある場合は破壊的変更になるため非推奨）

C) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 4: フィルタ列インデックスのマイグレーション方式

A) （推奨）`QueryExecutionHistory`は新規テーブルのため、`CREATE TABLE`と同じ
マイグレーションファイル内で`CREATE INDEX`を定義する。`AuditEvent`はUnit 2で
既に作成済みの既存テーブルであるため、別途`ALTER`相当の新規マイグレーション
（`CREATE INDEX`文のみのファイル）を追加する

B) `AuditEvent`のインデックス追加は見送り、`QueryExecutionHistory`のみインデックスを
付与する

C) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Resilience / Scalability Patterns（質問なし）

nfr-requirements.mdの通り、resiliency-baseline拡張は不適用であり、単一インスタンス・
同時利用者数約10名規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入
しない（Unit 1〜5と同じ方針）。

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Design成果物を生成する。
