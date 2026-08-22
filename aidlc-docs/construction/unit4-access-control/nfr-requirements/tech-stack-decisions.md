# Tech Stack Decisions — Unit 4: アクセス制御

requirements.md 3章およびUnit 1〜3のNFR Requirementsで既に確定済みの技術スタックに加え、
Unit 4のNFR Requirementsで新たに確定した項目を記録する。

## 既存確定事項（参考）

- 依存関係管理: Gradle `dependencyLocking` + GitHub Dependabot（Unit 1確立済み）
- テストフレームワーク: JUnit5 + Mockito + jqwik（バックエンド）、Vitest + React Testing
  Library（フロントエンド）
- 権限設定の対象リソース特定: 接続ID＋スキーマ名／テーブル名／カラム名の文字列参照
  （Unit 3の`DbSchema`/`DbTable`/`DbColumn`IDを直接参照しない、Functional Design Question 1）

## Unit 4で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| YAML処理ライブラリ | Jackson YAMLモジュール（`com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`） | Question 1: 既存のJackson ObjectMapperベースのREST DTOマッピングパターンをYAML入出力にもそのまま適用できる |
| 実効権限キャッシュ | Caffeine（`com.github.ben-manes.caffeine:caffeine`）、TTLなし・最大10,000エントリ | Question 2: requirements.mdで指定されたキャッシュ実装。無効化イベント駆動のため無期限が適切 |
| 実効権限解決のクエリ設計 | 対象Subject（ユーザ＋所属全グループ）分の`PermissionEntry`を1クエリで一括取得 | Question 3: N+1クエリ回避。明示的な性能目標値は設定しない |
| YAMLデシリアライズ方式 | 型安全なDTOマッピングのみ使用（任意型のインスタンス化を許可しない） | Question 4: SECURITY-09準拠、YAML deserialization攻撃対策 |
| 監査ログ記録粒度 | 操作単位（権限設定変更・グループ操作・YAML入出力それぞれ1操作＝1イベント） | Question 5: Unit 2/3の粒度方針を踏襲 |

## Unit 5・6への申し送り

- `AccessControlComponent#resolveEffectivePermission`（Caffeineキャッシュ経由）は、
  Unit 5（マスタメンテナンス、レコード表示・編集・作成・削除可否判定）とUnit 6
  （クエリビルダー・実行時のカラムレベル権限検証）が呼び出す想定
- Unit 3の`importSchema`実行時、`PermissionCacheComponent#invalidateByConnection`を
  呼び出す連携が必要（Functional Design business-logic-model.md Section 4）。Unit 3側の
  実装への呼び出し追加はUnit 4のCode Generationで対応する
