# Repository Layer Summary — Unit 1: デザインシステム基盤

## 生成したクラス
- `AppThemeEntity`（JPA `@Entity`、テーブル`app_theme`、単一行=id固定値1）
- `AppThemeJpaRepository`（Spring Data JPA、パッケージプライベート）
- `AppThemeRepositoryImpl`（`AppThemeRepository`ポートのJPA実装アダプタ。`load()`は
  レコードが存在しない場合`AppTheme.defaultTheme()`にフォールバック、`save()`は既存行の
  更新または新規作成）

## DBマイグレーション
- `V1__create_app_theme.sql`（Flyway、`app_theme`テーブル作成）

## 生成したテスト
- `AppThemeRepositoryImplTest`（`@DataJpaTest`、既定値フォールバック・保存/読込ラウンド
  トリップ・2回保存時の更新（挿入ではない）を検証）

## PBT適用評価
DB書き込み/読み込みのラウンドトリップはPBT-02の対象外と明記されている
（"for the data transformation layer, not the I/O itself"）。`AppTheme`↔`AppThemeEntity`間の
変換自体も単純なフィールドコピーでありデータ変換ロジックと呼べるほどの複雑さがないため、
本Unitでは例示ベーステストのみとした。
