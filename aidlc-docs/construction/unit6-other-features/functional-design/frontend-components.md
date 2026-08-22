# Unit 6: その他機能 - Frontend Components

## ルーティング構成（追加分）

| パス | 画面 | レイアウト | 認証要否 |
|---|---|---|---|
| `/queries` | QueryScreen | AppLayout配下 | 要（ロール問わず） |
| `/queries/history` | QueryHistoryScreen | AppLayout配下 | 要（ロール問わず） |
| `/audit-log` | AuditLogScreen | AppLayout配下 | 要（ADMINロールのみ） |

`AppLayout`の`navItems`に「クエリ」「クエリ履歴」（全ロール共通）、ADMINロール限定で
「監査ログ」を追加する。

## 1. QueryScreen（US-4.1〜US-4.5）

- **構造**:
  1. モード切替タブ（「クエリビルダー」/「SQL直接入力」）
  2. クエリビルダーモード: SELECT/FROM/JOIN/WHERE/GROUP BY/HAVING/ORDER BY/
     LIMIT OFFSETの各サブタブ（Question 1によりフロントエンドで`buildSql`相当の
     ロジックを実装し、状態変更のたびに下部のSQLプレビューを更新する）
  3. SQL直接入力モード: テキストエリア（手入力、または保存済みクエリ選択時に
     `parseSqlToBuilderState`相当のロジックでビルダー側タブに反映することも可能）
  4. 保存済みクエリ選択（Select、自分が作成したクエリ＋PUBLICなクエリを列挙、
     RETIRED状態は除外）
  5. 「保存」ボタン（モーダルで名前・公開範囲を入力、既存クエリを開いている場合は
     上書き保存も選択可能）、「論理非表示」ボタン（作成者のみ表示）
  6. 実行パネル: 接続選択、スキーマ選択（Unit 3の`getSchema`から選択）、
     検出されたパラメータの値入力欄（`:paramName`ごとに1行）、「実行」ボタン
  7. 実行結果テーブル（make-you-chic-uiの`Table`コンポーネント、読み取り専用
     表示、件数・実行時間を上部に表示）
- **状態**: `mode`（"builder"|"raw"）、`builderState`（各タブの入力）、`sqlText`、
  `selectedSavedQueryId`、`savedQueries`、`selectedConnectionId`/`selectedSchemaName`、
  `detectedParams`/`paramValues`、`queryResult`、`executeErrorMessage`、
  `saveModalOpen`/`saveName`/`saveVisibility`
- **操作フロー**:
  - ビルダー操作: 各サブタブの入力変更のたびに`sqlText`を再生成する
  - 保存済みクエリ選択: `sqlText`をロードし、ビルダーモードであれば
    `parseSqlToBuilderState`相当のロジックでタブに反映する
  - 保存: `saveQuery` API呼び出し（新規/更新はQuestion 5により`savedQueryId`の
    有無で自動判別）
  - 論理非表示: 確認モーダル後`retireQuery` API呼び出し
  - パラメータ自動検出: `sqlText`変更のたびに`:paramName`を検出し、
    `paramValues`の入力欄を同期する
  - 実行: `executeQuery` API呼び出し。読み取り専用でない・スキーマ許可リスト外等の
    エラーはメッセージ表示する
- **API**: component-methods.mdの`saveQuery`/`retireQuery`/`detectParameters`
  （フロントエンド実装、Question 1）/`executeQuery`に対応するAPI群、Unit 3の
  `getSchema`（対象スキーマ選択用）

## 2. QueryHistoryScreen（US-4.6）

- **構造**: フィルタ条件（実行日時範囲、実行者、対象スキーマ、SQLテキスト部分一致）、
  ページング付き履歴一覧テーブル（実行日時・実行者・対象接続/スキーマ・
  保存クエリ名またはSQL先頭部分・結果件数・実行時間。保存クエリ由来か直接入力かを
  アイコン等で区別表示）
- **アクセス制御**: 認証済みであればロール問わずアクセス可能

## 3. AuditLogScreen（US-5.1）

- **構造**: フィルタ条件（イベント種別、実行者、日時範囲。Question 7により実カラムの
  みが対象）、ページング付き監査ログ一覧テーブル（日時・ユーザID・イベント種別・
  詳細[行展開でJSON整形表示、接続ID・対象リソース・結果ステータス等を含む]）
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth role="ADMIN"`）
