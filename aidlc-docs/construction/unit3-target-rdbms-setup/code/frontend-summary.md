# Frontend Summary — Unit 3: 対象RDBMSセットアップ

## 生成したモジュール

### APIクライアント
- `src/api/connections.ts`: `listConnections`/`registerConnection`/
  `deactivateConnection`/`reactivateConnection`/`importSchema`
  （Unit 2の`authenticatedJson`/`authenticatedVoid`ヘルパーをそのまま利用）

### 画面
- `src/routes/admin/ConnectionListScreen.tsx`: 接続一覧、登録モーダル（RDBMS種別・
  ホスト・ポート・データベース名・対象スキーマ名[任意]・ユーザ名・パスワード）、
  スキーマ取込結果モーダル（追加/削除件数のサマリ、削除されたテーブル・カラム一覧）、
  無効化/再有効化/スキーマ取込操作を1画面に統合（Unit 2のAdminUserListScreenと
  同じ構成パターン）

### ルーティング・レイアウト更新（既存ファイル修正）
- `src/App.tsx`: `RequireAuth role="ADMIN"`配下に`/connections`を追加
- `src/layout/AppLayout.tsx`: ADMIN限定で`navItems`に「接続管理」を追加

### i18n
`src/i18n/locales/{ja,en}/common.json`に`nav.connections`/`admin.connections.*`を追加

## 生成したテスト

- `ConnectionListScreen.test.tsx`（3件: 一覧表示、登録モーダル送信・一覧再読込、
  スキーマ取込結果表示）
