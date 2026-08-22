# Build and Test Summary

対象: MasterMeister5（Unit 1〜6、全機能完了時点）。実施日: 2026-08-22。

## Build Status

- **Build Tool**: Gradle 9.6.1（Java 25、node-gradleプラグイン経由でNode v26.7.0
  / npm 11.19.0を使用してフロントエンドをビルド）
- **Build Command**: `./gradlew clean build`
- **Build Status**: **Success**
- **Build Time**: 約3分45秒
- **Build Artifacts**:
  - `backend/build/libs/backend-*.war`（フロントエンド静的アセット同梱の
    本番相当WAR）
  - `backend/src/main/resources/static/`（`vite build`出力。ビルド後に
    `data-testid`や日本語UI文言[「クエリビルダー」「監査ログ」等]が実際に
    バンドルへ含まれていることを`grep`で確認済み）
- 許容される警告: `RateLimitConfig.java`関連の非推奨API使用ノート
  （bucket4jライブラリ起因、機能に影響なし、既存の既知事項）

## Test Execution Summary

### Unit Tests

| 対象 | Total | Passed | Failed | Status |
|---|---|---|---|---|
| バックエンド（`./gradlew :backend:test`） | 210 | 210 | 0 | Pass |
| フロントエンド（`cd frontend && npx vitest run`） | 46 | 46 | 0 | Pass |

- **Coverage**: 未計測（JaCoCo等のカバレッジツール未導入。詳細は
  unit-test-instructions.md参照）
- テストファイル数: バックエンド40ファイル、フロントエンド19ファイル
- Unit別のバックエンドテスト件数内訳（`useraccount`/`audit`=Unit 2、
  `connectionschema`=Unit 3、`accesscontrol`=Unit 4、`mastermaintenance`=Unit 5、
  `query`=Unit 6、`platform`=Unit 1・横断基盤）は
  unit-test-instructions.mdの表を参照

### Integration Tests

専用の結合テストソースセットは存在しないが、以下の形でUnit間結合を実質的に
検証している（詳細はintegration-test-instructions.md参照）:
- **単体テストの枠内での結合検証**: 実DBを使ったSQL生成・実行検証
  （Unit 5・6）、`@DataJpaTest`によるFlyway×JPAマッピング検証（全Unit）、
  イベント駆動連携の検証（Unit 3→Unit 5の`SchemaImportedEvent`）
- **手動結合スモークテスト**: ログイン→ユーザ招待→接続登録→スキーマ取込→
  権限設定→データ表示→クエリ実行→監査ログ閲覧までの10シナリオを定義。
  自動化はされていない（未実施、手順のみ整備）
- **Test Scenarios**: 10（integration-test-instructions.md）
- **Status**: Pass（単体テストレベル）／未実施（手動スモークテストは手順整備のみ）

### Performance Tests

- 要件上（同時利用者数約10名規模、resiliency-baseline拡張オプトアウト済み）、
  自動化された負荷試験はスコープ外と判断（Unit 1〜6のNFR Designで一貫した結論）
- クエリ実行結果の上限件数（1,000件）・タイムアウト（30秒）等、個別の性能関連
  制約は実装・単体テストで検証済み（詳細はperformance-test-instructions.md参照）
- **Status**: N/A（自動負荷試験）／Pass（個別の性能関連実装の単体テスト）

### Additional Tests

- **Security Tests**: 実施（security-test-instructions.md）。SECURITY-01〜09・
  11〜15は各Unitの実装・テストで作り込み済み。**SECURITY-10（依存関係脆弱性
  スキャン）は未達成（`.github/dependabot.yml`等が未導入というギャップを検出）**
- **Contract Tests**: N/A（本プロジェクトはモノリシックな単一デプロイ単位であり、
  Unit間はJavaのメソッド呼び出し・Spring Eventで直結しているため、独立した
  API契約テストの対象となる外部サービス境界が存在しない）
- **E2E Tests**: 未実施（自動化されたE2Eテストツール[Playwright等]は導入して
  いない。integration-test-instructions.mdの手動結合スモークテスト手順が
  実質的な代替）

## Overall Status

- **Build**: Success
- **All Automated Tests**: Pass（バックエンド210件・フロントエンド46件、
  合計256件、失敗0件）
- **既知のギャップ**:
  1. SECURITY-10（依存関係脆弱性スキャナ未導入）
  2. 手動結合スモークテスト・E2Eテストは手順のみ整備で未実施
  3. テストカバレッジ未計測
- **Ready for Operations**: Yes（上記ギャップは本番運用を直ちに妨げるものでは
  ないが、ユーザーの判断で対応要否を検討することを推奨する）

## Next Steps

CLAUDE.mdのワークフロー上、次はOperations phase（プレースホルダー、将来拡張用）
に進む。上記の既知のギャップへの対応は、ユーザーの判断で別途スコープとして
着手することを推奨する。
