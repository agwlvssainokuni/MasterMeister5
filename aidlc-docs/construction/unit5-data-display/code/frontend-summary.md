# Frontend Summary — Unit 5: データ表示

## 生成したモジュール

### APIクライアント
- `src/api/masterData.ts`: `listTables`/`listRecords`/`applyChanges`
- `src/api/customizations.ts`: `getCustomizationDefinition`/
  `exportCustomizationDefinition`/`importCustomizationDefinition`
- `src/api/connections.ts`（既存ファイル修正）: `SchemaImportResultDto`に
  `prunedCustomizationCount`を追加

### 画面
- `src/routes/admin/MasterDataScreen.tsx`: 接続/テーブル選択、手入力WHERE句、
  make-you-chic-uiの`Table`コンポーネント（外部ページング・単一列ソート・
  インライン編集・ドラッグリサイズを標準提供）によるレコード一覧表示、行単位の
  削除マーク、新規作成モーダル、「反映」ボタンでの一括送信（オールオアナッシング、
  US-3.1〜US-3.6）。フィルタ条件ビルダーUI（演算子選択式）は本Stepでは手入力WHERE句
  のみを実装し、UI駆動のフィルタビルダーは今後の改善候補とした（バックエンドAPI
  [`FilterCondition`]は実装済みで、フロントエンドの追加実装のみで対応可能）
- `src/routes/admin/CustomizationScreen.tsx`: 接続選択、テーブル一覧表示、
  YAMLエクスポート/インポート（Unit 4の`PermissionScreen`と同じパターン。
  `FileReader`ベースのテキスト読み込みを使用、jsdomでの`File#text()`未実装問題は
  Unit 4で確立済みの回避策をそのまま踏襲）

### ルーティング・レイアウト更新（既存ファイル修正）
- `src/App.tsx`: `/data`（認証済み全ユーザ）、`/data/customization`
  （`RequireAuth role="ADMIN"`配下）を追加
- `src/layout/AppLayout.tsx`: 全ロール共通navItem「データ表示」、ADMIN限定navItem
  「表示・入力カスタマイズ」を追加
- `src/routes/admin/ConnectionListScreen.tsx`（既存ファイル修正）: スキーマ取込
  結果モーダルに`prunedCustomizationCount`（陳腐化のため削除されたカスタマイズ定義
  件数）を追加表示

### i18n
`src/i18n/locales/{ja,en}/common.json`に`nav.masterData`/`nav.customizations`/
`admin.masterData.*`/`admin.customizations.*`/
`admin.connections.importResult.prunedCustomizations`を追加

## 生成したテスト

- `MasterDataScreen.test.tsx`（2件: 接続/テーブル選択後のレコード表示、
  削除マーク→反映ボタンでのAPI呼び出し内容検証）
- `CustomizationScreen.test.tsx`（2件: テーブル一覧表示、YAMLインポート失敗時の
  エラー表示）
- `ConnectionListScreen.test.tsx`（既存ファイル修正）: スキーマ取込結果モーダルの
  `prunedCustomizationCount`表示を検証するアサーションを追加
