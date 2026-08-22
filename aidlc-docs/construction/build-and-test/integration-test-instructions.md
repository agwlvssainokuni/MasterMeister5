# Integration Test Instructions

対象: MasterMeister5（Unit 1〜6、全機能完了時点）

## 前提: 本プロジェクトの「結合テスト」の位置づけ

本プロジェクトには独立した結合テスト用ソースセット（例: `src/integrationTest/`）や
専用のGradleタスクは存在しない。Unit間の結合は、以下の2つの方法で実質的に検証
している。既存になかったものを新規に作らず、確立済みの方針をそのまま踏襲する。

1. **単体テストの枠内で実施する結合検証**（`./gradlew :backend:test`に含まれる）:
   - `@DataJpaTest` + `@ImportAutoConfiguration(FlywayAutoConfiguration.class)`
     によるFlywayマイグレーション×JPAエンティティマッピングの結合確認
     （Unit 1〜6の全リポジトリテスト）
   - 実際のH2データベースを「対象RDBMS」として使う結合確認
     （`MasterMaintenanceServiceImplTest`[Unit 5]・`QueryServiceImplTest`
     [Unit 6]。SQL生成ロジックとJDBCドライバの結合をモックなしで検証する）
   - Unit間イベント連携の結合確認（`ConnectionSchemaServiceImplTest`
     [Unit 3]に追加した、`importSchema`成功時の`SchemaImportedEvent`発行検証。
     Unit 5の`MasterMaintenanceServiceImpl#onSchemaImported`が実際にこのイベントを
     購読し陳腐化整理を行うことは`MasterMaintenanceServiceImplTest`側で検証済み）
   - Unit間の非破壊的拡張の結合確認（Unit 4の
     `AccessControlService#resolveEffectivePermissionsForTable`をUnit 5が呼び出す
     箇所、Unit 2の`AuditLogService#listEvents`拡張オーバーロードをUnit 6が
     呼び出す箇所は、それぞれの呼び出し元テストでモック経由の契約検証を行っている）

2. **手動での結合スモークテスト**（自動化されていない、下記手順で実施する）

## 手動結合スモークテストの手順

### 1. 環境の起動

```bash
docker compose -f devenv/docker-compose.yml --profile postgres up -d mailpit postgres
```

（対象RDBMSはMySQL/MariaDB/PostgreSQLいずれかを選択可能。ここではpostgresを例示）

```bash
export $(grep -v '^#' .env | xargs)   # .envの環境変数を読み込む
./gradlew :backend:bootRun
```

### 2. Unit横断の主要シナリオ

| # | シナリオ | 関与Unit |
|---|---|---|
| 1 | 初期管理者でログイン（`MM5_INITIAL_ADMIN_EMAIL`/`MM5_INITIAL_ADMIN_PASSWORD`） | Unit 2 |
| 2 | 一般ユーザを招待→登録メールをMailPit（http://localhost:8025）で確認→本登録 | Unit 2 |
| 3 | 対象RDBMS接続を登録（devenvのpostgres、host: localhost、port: 5432）→スキーマ取込 | Unit 3 |
| 4 | グループを作成し一般ユーザを追加→アクセス権限画面でテーブル/カラム単位の権限を設定 | Unit 4 |
| 5 | 一般ユーザでログインし直し、「データ表示」画面で権限のある列のみ表示されることを確認→レコードを編集・反映 | Unit 4・5 |
| 6 | 手順3の接続でスキーマ再取込を実行し、手順5で設定したカスタマイズ定義が
      陳腐化整理されないこと（削除されたテーブル/カラムがない限り）を確認 | Unit 3・5 |
| 7 | 「クエリ」画面でSELECT文を作成・保存（公開範囲: 全員）→実行→結果表示を確認 | Unit 6 |
| 8 | 別ユーザで「クエリ」画面から手順7の保存クエリを実行できること（PUBLIC）を確認 | Unit 6 |
| 9 | 「クエリ履歴」画面で手順7・8の実行履歴が表示されることを確認 | Unit 6 |
| 10 | 管理者で「監査ログ」画面を開き、手順1〜9の操作（ログイン・招待・接続登録・
       スキーマ取込・権限変更・レコード変更・クエリ実行等）が記録されていることを
       確認 | Unit 2・6（記録は各Unit、閲覧はUnit 6） |

### 3. ログの確認

- アプリケーションログ（構造化ログ、Logback）に`auditEvent=...`行が出力される
  ことを確認する（`infrastructure-design.md` Question 4、監査イベントは
  DB記録と構造化ログの両方に出力される）

### 4. クリーンアップ

```bash
docker compose -f devenv/docker-compose.yml --profile postgres down
```

## 既知のギャップ

上記スモークテストは自動化されていない。将来的にE2Eテスト（Playwright等）や
専用の結合テストソースセットを追加する場合は、本手順のシナリオ1〜10をベースに
できる。
