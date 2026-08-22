# Unit Test Execution

対象: MasterMeister5（Unit 1〜6、全機能完了時点）

## Run Unit Tests

### 1. バックエンド単体テストの実行

```bash
./gradlew :backend:test
```

property-based-testing拡張（jqwik）を全面適用しているため、`@Test`（JUnit5）と
`@Property`（jqwik）が同一クラスに混在するテストクラスが多数存在する。jqwikは
JUnit Jupiterの`@BeforeEach`/`@AfterEach`/`@Mock`を実行しないため、該当クラスは
フィールド初期化子・インスタンス初期化ブロックでセットアップしている
（Unit 2〜6共通の既知の制約）。

### 2. フロントエンド単体テストの実行

```bash
cd frontend
npx vitest run
```

Gradleビルドには統合されていない（`npmBuildFrontend`タスクは`npm run build`の
みを実行する）ため、上記コマンドを個別に実行する必要がある。

### 3. テスト結果の確認

**2026-08-22実施、全6Unit完了時点の実測結果**:

| 対象 | テストファイル数 | テスト件数 | 結果 |
|---|---|---|---|
| バックエンド全体 | 40 | 210 | 全件成功 |
| うち `useraccount`（Unit 2） | - | 51 | 成功 |
| うち `audit`（Unit 2） | - | 7 | 成功 |
| うち `connectionschema`（Unit 3） | - | 26 | 成功 |
| うち `accesscontrol`（Unit 4） | - | 45 | 成功 |
| うち `mastermaintenance`（Unit 5） | - | 26 | 成功 |
| うち `query`（Unit 6） | - | 27 | 成功 |
| うち `platform`（Unit 1・横断基盤） | - | 28 | 成功 |
| フロントエンド全体 | 19 | 46 | 全件成功 |

- **テストレポートの場所**: `backend/build/reports/tests/test/index.html`
  （バックエンド）、`frontend`でVitestをUIモード（`npx vitest --ui`）や
  `--reporter=verbose`で実行することでも詳細確認可能
- **カバレッジ計測**: 本プロジェクトではJaCoCo等のカバレッジ計測ツールは
  導入していない（未計測。要件・NFRで数値目標が定められていないため、
  導入は今回のスコープ外とした）

### 4. テスト失敗時の対応

1. `backend/build/reports/tests/test/`（バックエンド）または`vitest run`の
   コンソール出力（フロントエンド）でテスト結果を確認する
2. 失敗したテストケースを特定する
3. 実装（プロダクションコードまたはテストコード）を修正する。本プロジェクトでは
   モックで隠蔽せず実際のH2データベースを「対象RDBMS」として使うテストが多数
   存在する（`MasterMaintenanceServiceImplTest`/`QueryServiceImplTest`等）ため、
   SQL生成・実行系の不具合は多くの場合テストで直接検出できる
4. 全件成功するまで再実行する
