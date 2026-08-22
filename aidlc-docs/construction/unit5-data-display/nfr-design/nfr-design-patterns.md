# NFR Design Patterns — Unit 5: データ表示

nfr-requirements.mdの各方針・tech-stack-decisions.mdの技術選定を、具体的な設計パターンに
落とし込む。

## Resilience / Scalability Patterns

N/A。resiliency-baseline不適用、単一インスタンス・同時利用者数約10名規模のため、
本Unitでは特別な耐障害性・スケーリングパターンを導入しない（Unit 1〜4と同じ方針）。

## Performance Patterns

### 実効権限バッチ判定（Question 2）

- `AccessControlService#resolveEffectivePermissionsForTable(userId, connectionId,
  schemaName, tableName, columnNames)`は、既存の`resolveEffectivePermission`と同じ
  `findForResolution`（1クエリでSubject×スキーマ単位の`PermissionEntry`を取得）を
  1回だけ呼び出し、渡された各カラム名について既存の階層フォールバックロジック
  （`buildChain`/`findMatch`）をメモリ上で繰り返し適用する
- カラムごとの結果は`CacheKey`単位でキャッシュに格納する（既存の`resolveEffectivePermission`
  と同じキャッシュを共有し、以後の単一カラム参照でもヒットする）

### SQL生成・パラメータバインディング（Question 5）

- `NamedParameterJdbcTemplate` + `MapSqlParameterSource`でカラム値をバインドパラメータ
  として渡す
- テーブル名・カラム名・スキーマ名はUnit 3の許可文字パターンで検証済みの文字列を
  そのままSQL文字列に連結する（識別子はバインドパラメータ化できないため）
- `listRecords`のCOUNT(*)クエリはWHERE句を共有し、SELECT本体と同じ条件で件数を取得する

## Security Patterns

### 手入力WHERE/ORDER BY句のブロックリスト（Question 3）

- 正規表現`;`、`--`、`/\*`のいずれかを含む入力を拒否する`private`メソッドとして
  `mastermaintenance.service`パッケージに実装する（Unit 3の`validateIdentifier`と
  同じ「共通化のための抽象化を避ける」方針）

### SQLインジェクション対策の全体像

- フィルタ条件の値（UI指定・手入力を問わず）はバインドパラメータとする
- 識別子（スキーマ名・テーブル名・カラム名）はUnit 3の`^[A-Za-z0-9._-]+$`パターンで
  検証する
- 手入力WHERE/ORDER BY句は上記ブロックリストを通過したもののみSQL文字列に連結する

### ValidationRuleの多層防御（Question 4）

- サーバ側（`MasterMaintenanceServiceImpl#applyChanges`）で必ず最終検証する
- クライアント側は同じルールを使った即時フィードバックのみ（UX向上目的、サーバ側
  検証の代替にはしない）

## Reliability Patterns

- `applyChanges`は「検証フェーズ（権限チェック・ValidationRule）→実行フェーズ
  （INSERT/UPDATE/DELETE）」の2段階に分離し、検証フェーズで1件でも失敗すれば
  実行フェーズに進まない（Question 4のNFR Requirements回答を具体化）

## Unit 3・Unit 4との連携パターン（Question 1、重要）

### 循環依存の回避（イベント駆動）

Unit 4では`accesscontrol`パッケージが`connectionschema`に依存していなかったため、
`ConnectionSchemaServiceImpl`から`PermissionCacheService`を直接呼び出す一方向の
依存追加で済んだ。Unit 5の`mastermaintenance`パッケージは`connectionschema`に
（`getSchema`等で）既に依存しているため、同じ直接呼び出しパターンを踏襲すると
`connectionschema ⇄ mastermaintenance`の循環依存が生じる。これを避けるため、
本Unitのみ以下のイベント駆動パターンを採用する:

1. `connectionschema.service`パッケージに`SchemaImportedEvent`
   （`connectionId`、`removedTableRefs`、`removedColumnRefs`は`final`、
   `prunedCustomizationCount`は`private`セッター経由で書き込み可能）を新規定義する
2. `ConnectionSchemaServiceImpl#importSchema`は、`SchemaImportResult`を構築する
   直前に`ApplicationEventPublisher#publishEvent(new SchemaImportedEvent(...))`を
   呼び出す
3. Unit 5の`MasterMaintenanceServiceImpl`に`@EventListener`メソッドを実装し、
   `SchemaImportedEvent`を受け取って陳腐化整理（`pruneStaleCustomizations`相当の
   処理）を行い、結果件数を`event.setPrunedCustomizationCount(...)`で書き戻す
4. Spring既定の同期イベント配信（`@TransactionalEventListener`は使わず、通常の
   `@EventListener`。`importSchema`のトランザクション内で同期的に実行される）に
   より、`publishEvent`呼び出しが返った時点で`event.getPrunedCustomizationCount()`
   が確定している
5. `connectionschema`パッケージは`mastermaintenance`パッケージの型を一切importせず、
   `mastermaintenance`パッケージのみが`connectionschema.service.SchemaImportedEvent`を
   importする（一方向）
