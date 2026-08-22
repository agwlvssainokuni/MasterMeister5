# Tech Stack Decisions — Unit 5: データ表示

requirements.md 3章およびUnit 1〜4のNFR Requirementsで既に確定済みの技術スタックに加え、
Unit 5のNFR Requirementsで新たに確定した項目を記録する。

## 既存確定事項（参考）

- 対象RDBMSアクセス: `NamedParameterJdbcTemplate`（requirements.md 3章、Unit 3の
  `ConnectionPoolRegistry`が提供するプールを利用）
- YAML処理: Jackson YAMLモジュール（Unit 4で追加済み、本Unitでも再利用する）
- 依存関係管理: Gradle `dependencyLocking` + GitHub Dependabot（Unit 1確立済み）
- カスタマイズ定義・権限設定の対象リソース特定: 接続ID＋名前ベース参照
  （`DbSchema`/`DbTable`/`DbColumn`のIDを直接参照しない、Unit 3/4/5共通の方針）

## Unit 5で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| 実効権限判定のバッチ化 | `AccessControlService#resolveEffectivePermissionsForTable`を新規追加（Unit 4の既存メソッドへの追加） | Question 1: `findForResolution`のDB取得を1回に抑え、カラム数分のN+1的往復を回避する |
| フィルタ値のSQLインジェクション対策 | `NamedParameterJdbcTemplate`のバインドパラメータ使用＋識別子の許可文字パターン検証 | Question 2: 値とSQL識別子を明確に分離する |
| 手入力WHERE/ORDER BY句の検証方式 | セミコロン・SQLコメント開始のブロックリスト方式（正規表現） | Question 3: 専用SQLパーサ導入のコストを回避しつつ最低限のスタッキング対策を行う |
| `applyChanges`の実行方式 | 検証完了後に単一トランザクションで実行（検証と実行を分離） | Question 4: オールオアナッシングを確実に担保する |
| レコード一覧のキャッシュ | キャッシュしない | Question 5: マスタデータの鮮度を優先する |
| 監査ログ記録粒度 | 操作単位（`applyChanges`1回＝1イベント、YAML入出力も1操作＝1イベント） | Question 6: Unit 3/4の粒度方針を踏襲 |

## Unit 4への変更申し送り

`AccessControlService`インタフェースに`resolveEffectivePermissionsForTable`
（`Long userId, Long connectionId, String schemaName, String tableName,
List<String> columnNames`引数、`Map<String, EffectivePermission>`相当の戻り値）を
追加する。既存の`resolveEffectivePermission`（単一カラム用）はそのまま維持し、
新規メソッドは内部で同じ階層フォールバックロジックを複数カラムに対して一括適用する
形で実装する（後方互換、既存呼び出し元への影響なし）。

## Unit 3への変更申し送り

`SchemaImportResult`（record）に`prunedCustomizationCount`フィールドを追加する
（Functional Design Question 5）。`ConnectionSchemaServiceImpl#importSchema`から
本Unitの`MasterMaintenanceService#pruneStaleCustomizations`相当の処理を呼び出す
具体的な連携方式（直接依存 or イベント経由）はNFR Designで確定する。
