# Frontend Summary — Unit 6: その他機能

## 生成したモジュール

### APIクライアント
- `src/api/query.ts`: `listSavedQueries`/`saveQuery`/`retireQuery`/
  `detectParameters`/`executeQuery`/`listExecutionHistory`
- `src/api/auditLog.ts`: `listAuditEvents`

### クエリビルダーロジック（フロントエンド専用、Functional Design Question 1）
- `src/routes/admin/queryBuilder.ts`: `buildSql`（タブ構成の状態からSQL文字列を
  組み立てる）/`parseSqlToBuilderState`（単純な単一テーブルSELECT文のベストエフォート
  逆変換。`buildSql`自身が生成した形式であれば往復変換できることをテストで確認済み。
  複雑な結合・サブクエリは完全な逆変換を保証しない、business-logic-model.md
  Section 1の設計判断通り）。バックエンドはこのロジックに一切関与しない

### 画面
- `src/routes/admin/QueryScreen.tsx`: モード切替タブ（クエリビルダー/SQL直接入力）、
  SELECT/FROM/JOIN/WHERE/GROUP BY/HAVING/ORDER BY/LIMIT OFFSETの各サブタブ
  （state変更のたびに`buildSql`でSQLプレビューを再生成）、保存済みクエリ選択
  （PUBLIC全件＋自分のPRIVATE件）、保存モーダル（新規/更新は`savedQueryId`の
  有無で自動判別）、論理非表示ボタン（作成者本人のみ表示、`useAuth()`の
  `user.id`とクエリの`creatorUserId`を比較）、接続/スキーマ選択（Unit 3の
  `getSchema`）、パラメータ自動検出→入力欄の動的生成、実行結果テーブル
  （make-you-chic-uiの`Table`コンポーネント、読み取り専用表示、`truncated`時は
  警告メッセージを表示。US-4.1〜US-4.5）
- `src/routes/admin/QueryHistoryScreen.tsx`: フィルタ（接続・スキーマ・SQL部分
  一致・実行日時範囲）、`Table`コンポーネントによる外部ページング付き履歴一覧
  （`savedQueryId`の有無で「保存クエリ」「直接入力」を列表示、US-4.6）
- `src/routes/admin/AuditLogScreen.tsx`: フィルタ（イベント種別・実行者ID・
  日時範囲。Question 7により実カラムのみ）、`Table`コンポーネントによる
  ページング付き一覧、行ごとの詳細表示ボタンで`details`のJSON整形表示（US-5.1）

### ルーティング・レイアウト更新（既存ファイル修正）
- `src/App.tsx`: `/queries`・`/queries/history`（認証済み全ユーザ）、
  `/audit-log`（`RequireAuth role="ADMIN"`配下）を追加
- `src/layout/AppLayout.tsx`: 全ロール共通navItem「クエリ」「クエリ履歴」、
  ADMIN限定navItem「監査ログ」を追加

### i18n
`src/i18n/locales/{ja,en}/common.json`に`nav.query`/`nav.queryHistory`/
`nav.auditLog`/`admin.query.*`/`admin.queryHistory.*`/`admin.auditLog.*`を追加
（クエリビルダーのタブラベル[SELECT/FROM/JOIN等]はSQL構文キーワードであり
日英で変わらないため、i18nキーを介さず直接リテラルとした）

## 生成したテスト

- `queryBuilder.test.ts`（6件。`buildSql`の句の組み立て順序・空欄省略、
  `parseSqlToBuilderState`の単純SELECT分解・`buildSql`との往復・非SELECT文への
  フォールバック）
- `QueryScreen.test.tsx`（2件: クエリ実行→結果表示のAPI呼び出し内容検証、
  新規クエリ保存のAPI呼び出し内容検証）
- `QueryHistoryScreen.test.tsx`（2件: 履歴一覧表示、スキーマフィルタ適用時の
  再フェッチ確認）
- `AuditLogScreen.test.tsx`（2件: イベント一覧表示、詳細JSON表示のトグル確認）
