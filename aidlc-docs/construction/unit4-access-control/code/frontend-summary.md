# Frontend Summary — Unit 4: アクセス制御

## 生成したモジュール

### APIクライアント
- `src/api/groups.ts`: `listGroups`/`createGroup`/`renameGroup`/`deleteGroup`/
  `listMembers`/`addMember`/`removeMember`（Unit 2の`authenticatedJson`/
  `authenticatedVoid`ヘルパーをそのまま利用）
- `src/api/permissions.ts`: `listPermissionEntries`/`setPrimaryPermission`/
  `setAuxiliaryPermission`/`exportPermissions`（`authenticatedFetch`でYAMLテキストを
  直接取得）/`importPermissions`
- `src/api/connections.ts`（既存ファイル修正）: `getSchema`（PermissionScreenの
  スキーマツリー表示用）、`SchemaViewDto`/`TableViewDto`/`ColumnViewDto`を追加

### 画面
- `src/routes/admin/GroupManagementScreen.tsx`: グループ一覧、作成/改名モーダル、
  削除、選択中グループの所属メンバー一覧・追加/削除（Unit 2/3のリスト画面パターンを
  踏襲）
- `src/routes/admin/PermissionScreen.tsx`: 接続選択→Subject種別（ユーザ/グループ）→
  Subject選択→スキーマ/テーブル/カラムツリー表示、各ノードでの主権限
  （NONE/READ/UPDATE）・補助権限（作成可/削除可、スキーマ/テーブルのみ）設定、
  YAMLエクスポート（`Blob`+一時的な`<a download>`要素によるダウンロード）/インポート
  （`FileReader`でテキスト読み込み後にAPI呼び出し。`File#text()`はテスト環境
  [jsdom]で未実装のため、より広く対応する`FileReader`ベースの読み込みを採用した）

### ルーティング・レイアウト更新（既存ファイル修正）
- `src/App.tsx`: `RequireAuth role="ADMIN"`配下に`/groups`/`/permissions`を追加
- `src/layout/AppLayout.tsx`: ADMIN限定で`navItems`に「グループ管理」
  「アクセス権限」を追加

### i18n
`src/i18n/locales/{ja,en}/common.json`に`nav.groups`/`nav.permissions`/
`admin.groups.*`/`admin.permissions.*`を追加

## 生成したテスト

- `GroupManagementScreen.test.tsx`（3件: 一覧表示、作成モーダル送信・一覧再読込、
  メンバー選択表示）
- `PermissionScreen.test.tsx`（3件: 接続/Subject選択後のスキーマツリー表示、
  主権限変更のAPI呼び出し内容検証、YAMLインポート失敗時のエラー表示）
