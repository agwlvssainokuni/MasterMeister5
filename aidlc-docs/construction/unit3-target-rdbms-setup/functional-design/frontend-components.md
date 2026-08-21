# Unit 3: 対象RDBMSセットアップ - Frontend Components

Unit 2で確立したAdminUserListScreenのパターン（一覧＋登録モーダル、ADMIN限定ルート）を
踏襲する。

## ルーティング構成（追加分）

| パス | 画面 | レイアウト | 認証要否 |
|---|---|---|---|
| `/connections` | ConnectionListScreen | AppLayout配下 | 要（ADMINロールのみ） |

`AppLayout`の`navItems`に、ADMINロール限定で「接続管理」を追加する。

## 1. ConnectionListScreen（US-2.1〜2.3）

- **構造**: 接続一覧テーブル（接続名／RDBMS種別／ホスト:ポート／データベース名／状態）、
  「接続を登録」ボタン（モーダルで各項目を入力）、各行に「スキーマ取込」（ACTIVE状態のみ
  活性）／「無効化」（ACTIVE状態のみ活性）／「再有効化」（DEACTIVATED状態のみ活性）操作
- **状態**: `connections`（一覧）、`loading`、登録モーダルの各入力値・
  `registerSubmitting`/`registerErrorMessage`、スキーマ取込結果モーダルの
  `importResult`/`importResultOpen`
- **バリデーション（登録モーダル）**: 接続名必須、RDBMS種別必須（Select: MySQL/MariaDB/
  PostgreSQL/H2）、ホスト・ポート・データベース名・ユーザ名・パスワード必須、
  対象スキーマ名は任意項目
- **操作フロー**:
  - 登録: モーダル送信 → `POST /api/admin/connections` → 成功時一覧を再取得しモーダルを
    閉じる → 疎通確認失敗時（BR-2）はモーダル内にエラーメッセージを表示する
  - スキーマ取込: `POST /api/admin/connections/{connectionId}/schema-import` →
    成功時、`SchemaImportResult`（追加/削除件数のサマリ）を結果モーダルに表示する
    （US-2.3の受け入れ基準）
  - 無効化/再有効化: `POST /api/admin/connections/{connectionId}/deactivate` /
    `POST /api/admin/connections/{connectionId}/reactivate`
  - いずれの操作後も一覧を再取得して最新状態を反映する
- **API**: component-methods.mdの`registerConnection`/`deactivateConnection`/
  `importSchema`に対応するAPI群
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth role="ADMIN"`）

## スキーマ取込結果モーダル

- **構造**: 追加されたテーブル数・カラム数、削除されたテーブル数・カラム数のサマリ表示。
  削除された項目がある場合は一覧（テーブル名/カラム名）を表示する
- **操作フロー**: 「閉じる」ボタンでモーダルを閉じる（他画面への遷移は行わない）
