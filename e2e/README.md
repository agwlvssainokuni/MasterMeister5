# MasterMeister5 E2E Tests

[Playwright](https://playwright.dev/)による、実際のパッケージ済みアプリ（backend WAR
に同梱されたフロントエンド）・devenvの対象RDBMS（PostgreSQL）・MailPitに対する
E2Eテスト。integration-test-instructions.md（
`aidlc-docs/construction/build-and-test/integration-test-instructions.md`）で
「未実施」としていた手動結合スモークテストのシナリオ1〜10を自動化したもの。

モック・スタブは一切使用しない。ログイン・招待メール（MailPit経由）・対象RDBMS
接続登録・スキーマ取込・アクセス権限設定・データ表示・クエリ実行・クエリ履歴・
監査ログ閲覧まで、Unit 1〜6を横断する一連の操作を実際のブラウザ操作で検証する。

## 前提

- devenvのPostgreSQLとMailPitが起動していること:
  ```bash
  cd devenv
  docker compose --profile postgres up -d
  ```
  （`devenv/initdb/postgres/01-schema-and-data.sql`によるサンプルデータが
  投入済みであること。詳細はREADME.mdの「開発環境セットアップ」参照）
- backend自体はE2Eテストのセットアップ（`scripts/start-app.sh`、Playwrightの
  `webServer`設定）が自動的に起動する。事前に手動起動しておく必要はない
  （むしろ既に8080番ポートで何か起動していると衝突するため、事前には起動しない
  こと）

## 実行方法

```bash
cd e2e
npm install
npx playwright install --with-deps chromium   # 初回のみ
npm test
```

`npm test`は内部で`scripts/start-app.sh`を実行し、E2E専用の使い捨て内部H2
データベース（`e2e/.data/`、実行のたびに削除・再作成される）でbackendを起動した
うえでテストを実行する。既存の開発用データベース（`data/mastermeister5`）には
一切影響しない。

失敗したテストのトレース・スクリーンショットは`npm run report`で確認できる。

## 何を検証しているか

`tests/main-journey.spec.ts`が一続きのシナリオとして以下を自動化する
（各ステップは前のステップが作った状態に依存するため、独立したテストではなく
1つの`test()`内の`test.step()`として順に実行する）:

1. 初期管理者でログイン（Unit 2）
2. 一般ユーザを招待する（Unit 2）
3. グループを作成し、招待済みユーザを追加する（Unit 2）
4. 対象RDBMS接続を登録する（Unit 3、devenvのPostgreSQL）
5. スキーマを取込む（Unit 3）
6. グループに`public`スキーマの読み取り権限を付与する（Unit 4）
7. 招待メールをMailPitから取得し、一般ユーザが本登録する（Unit 2）
8. 一般ユーザでログインし、権限付与された範囲のデータを閲覧する（Unit 4・5）
9. クエリ画面でSQLを実行する（Unit 6）
10. クエリ実行履歴に記録されていることを確認する（Unit 6）
11. 管理者で再ログインし、監査ログに一連の操作が記録されていることを確認する
    （Unit 2・6）

## 既知の制約

- 単一のシナリオ（ハッピーパス）のみを自動化しており、異常系・権限拒否等の
  枝分かれは対象外（それらはバックエンド/フロントエンドの単体テストで担保
  済み。unit-test-instructions.md参照）
- MySQL/MariaDBに対する同等のE2Eは未実装（PostgreSQLのみ）。対象RDBMSの
  種類ごとの差異はUnit 3のバックエンド単体テストでカバーする
- CI（GitHub Actions等）への組み込みは未実施。ローカル実行のみ
