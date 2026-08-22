# Build Instructions

対象: MasterMeister5（Unit 1〜6、全機能完了時点）

## Prerequisites

- **Build Tool**: Gradle 9.6.1（`./gradlew`同梱、別途インストール不要）
- **Java**: 25（`backend/build.gradle.kts`の`JavaLanguageVersion.of(25)`で固定。
  本ビルドではEclipse Temurin 25.0.3で検証済み）
- **Node.js / npm**: フロントエンドビルドはGradleの`node-gradle`プラグイン経由で
  実行されるため別途インストール不要（本ビルドではNode v26.7.0 / npm 11.19.0で
  検証済み）
- **Git submodule**: `libs/make-you-chic-ui`（UIコンポーネントライブラリ）。
  README.mdのセットアップ手順1〜2（`git submodule update --init --recursive`、
  `npm install && npm run build -w make-you-chic-ui`）を先に実行しておくこと
  （Gradleビルド自体も`npmInstallMakeYouChicUi`/`npmBuildMakeYouChicUi`タスクで
  同等の処理を自動実行するため、通常は明示的な事前実行は不要）
- **環境変数**: `MM5_CONNECTION_SECRET_KEY`・`MM5_JWT_SECRET`（いずれも32byte以上、
  ビルド自体には不要だが実行時に必須）。詳細は`.env.example`を参照
- **システム要件**: 単一インスタンス構成前提（クラウド不使用）。特別なメモリ・
  ディスク要件はない

## Build Steps

### 1. 依存関係の解決

```bash
./gradlew dependencies
```

`dependencyLocking`（全Gradleモジュール共通）によりロックファイル
（`gradle.lockfile`）でバージョンを固定している（tech-stack-decisions.md、
Unit 1確立済み）。

### 2. 環境の設定

`.env.example`を`.env`にコピーし、`MM5_CONNECTION_SECRET_KEY`・`MM5_JWT_SECRET`を
生成して設定する（README.md「開発環境セットアップ」参照）。ビルド自体（コンパイル・
テスト・WARパッケージング）はこれらの環境変数なしでも成功する。

### 3. 全体ビルド

```bash
./gradlew clean build
```

このコマンドで以下が一括実行される:
- `libs:java-mustache-processor:cherry-mustache-core`のコンパイル・テスト
- `backend`のコンパイル・単体テスト（`./gradlew :backend:test`相当）
- `npmInstallMakeYouChicUi`/`npmBuildMakeYouChicUi`（`make-you-chic-ui`サブモジュール
  のビルド）
- `npmBuildFrontend`（`vite build`、出力先は`backend/src/main/resources/static/`）
- `backend`の`bootWar`（フロントエンドのビルド成果物を含む単一WARファイル）

フロントエンド自身のテスト（Vitest）はGradleタスクに統合されていない
（`npmBuildFrontend`は`npm run build`のみを実行し`npm test`は実行しない）ため、
別途Step「フロントエンドテスト」を実行する必要がある（unit-test-instructions.md
参照）。

### 4. ビルド成功の確認

- **期待される出力**: `BUILD SUCCESSFUL`
- **ビルド成果物**:
  - `backend/build/libs/backend-*.war`（本番相当のデプロイ可能WAR、フロントエンド
    静的アセット同梱）
  - `backend/build/resources/main/static/`・`backend/src/main/resources/static/`
    （`vite build`によるフロントエンドバンドル。`.gitignore`対象）
- **許容される警告**: `RateLimitConfig.java`/`RateLimitFilterTest.java`の
  非推奨API使用に関する`javac`ノート（bucket4jライブラリの非推奨APIに起因、
  機能に影響なし。本ビルドで再確認済み、既存の既知事項）

本ビルド（2026-08-22実施、全6Unit完了時点）は上記コマンドで`BUILD SUCCESSFUL`
（所要時間 約3分45秒）。

## Troubleshooting

### ビルドが依存関係エラーで失敗する
- **原因**: `libs/make-you-chic-ui`サブモジュールが未初期化、または
  ロックファイルと`build.gradle.kts`の依存宣言が不整合
- **解決策**: `git submodule update --init --recursive`を実行する。依存関係を
  意図的に変更した場合は`./gradlew dependencies --write-locks`でロックファイルを
  更新する

### ビルドがコンパイルエラーで失敗する
- **原因**: Java 25以外のJDKを使用している、またはGradleのtoolchain解決に失敗
  している
- **解決策**: `java -version`でJava 25系であることを確認する。Gradleの
  toolchain機能により自動的にJava 25がダウンロードされる場合もあるが、
  ネットワーク制限下では失敗することがある

### フロントエンドビルドが失敗する
- **原因**: `make-you-chic-ui`サブモジュールが未ビルド（`npmBuildMakeYouChicUi`
  タスクの実行漏れ）
- **解決策**: `./gradlew :backend:npmBuildMakeYouChicUi`を単独実行して原因を
  切り分ける
