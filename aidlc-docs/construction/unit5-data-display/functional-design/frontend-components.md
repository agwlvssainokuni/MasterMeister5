# Unit 5: データ表示 - Frontend Components

Unit 1〜4で確立したパターンを踏襲しつつ、本Unitは初めて「一般ユーザ（ADMINロール
限定ではない）」向けの画面を持つ（US-3.1〜US-3.6は一般ユーザ対象、US-3.7のみ管理者
対象）。

## ルーティング構成（追加分）

| パス | 画面 | レイアウト | 認証要否 |
|---|---|---|---|
| `/data` | MasterDataScreen | AppLayout配下 | 要（ロール問わず、認証済みであれば可） |
| `/data/customization` | CustomizationScreen | AppLayout配下 | 要（ADMINロールのみ） |

`AppLayout`の`navItems`に「データ表示」（全ロール共通）、ADMINロール限定で
「表示・入力カスタマイズ」を追加する。

## 1. MasterDataScreen（US-3.1〜US-3.6）

- **構造**:
  1. 接続選択（Select、ACTIVE状態の接続一覧）
  2. テーブル/ビュー選択（Select、Unit 3の`getSchema`のうち、選択中ユーザが
     いずれかのカラムにREAD以上を持つテーブルのみ列挙する）
  3. フィルタ条件UI（読み取り権限のあるカラムごとに演算子・値を指定する行を
     追加/削除できるビルダー、Question 6の演算子）と、手入力WHERE句/ORDER BY句への
     切替タブ（US-3.3）
  4. レコード一覧テーブル（列: `RecordPage.columns`のラベル・並び順どおり。
     ウィジェット種別に応じたセル表示、`readOnly`でないUPDATE権限列はインライン
     編集可能）
  5. ページネーションコントロール（前へ/次へ、ページ番号、総件数表示）
  6. 「反映」ボタン（編集済みセルがある場合のみ活性化）、「新規作成」ボタン
     （`canCreate`が真の場合のみ表示）、各行の「削除」ボタン（`canDelete`が真の
     場合のみ表示）
- **状態**: `connections`/`tables`/`selectedConnectionId`/`selectedTableRef`、
  `filterConditions`（ビルダー行の配列）/`rawWhereClause`/`rawOrderByClause`/
  `filterMode`（"builder"|"raw"）、`page`データ（`columns`/`rows`/`page`/
  `pageSize`/`totalCount`）、`pendingChanges`（編集・作成・削除の差分を
  `RecordChange`形式で蓄積する）、`applyErrorMessage`
- **操作フロー**:
  - 接続/テーブル選択時: 対象テーブルの`RecordPage`を初期条件（1ページ目、
    デフォルトソート）で取得する
  - フィルタ/ソート変更・ページ送り: `listRecords`を再実行する（未反映の
    `pendingChanges`がある場合は破棄前に確認ダイアログを表示する）
  - セル編集: `pendingChanges`に`UPDATE`エントリを追加/更新する（主キー値で
    行を特定）
  - 新規作成: 空行フォーム（モーダル）を開き、入力内容を`pendingChanges`に
    `CREATE`エントリとして追加する
  - 行削除: 確認モーダル後、`pendingChanges`に`DELETE`エントリを追加する
    （即時APIコールはしない。「反映」時にまとめて送信する、requirements.md
    「反映ボタン押下で単一トランザクション送信」）
  - 反映: `applyChanges` API呼び出し。成功時は`pendingChanges`をクリアし
    一覧を再取得。失敗時（オールオアナッシングで全体拒否）はエラー内容を表示し
    `pendingChanges`は保持する（US-3.4受け入れ基準）
- **API**: component-methods.mdの`listRecords`/`applyChanges`に対応するAPI群、
  Unit 3の`getSchema`（テーブル一覧・カラムメタ情報）
- **アクセス制御**: 認証済みであればロール問わずアクセス可能。表示・編集可否は
  画面内でUnit 4の実効権限に基づき制御する

## 2. CustomizationScreen（US-3.7）

- **構造**: 接続選択後、対象接続の`TableCustomization`一覧（テーブル名、
  カスタマイズ済みカラム数）を表示し、「YAMLエクスポート」「YAMLインポート」
  ボタンを提供する（Unit 4のPermissionScreenのYAML入出力と同じパターン）
- **操作フロー**:
  - エクスポート: `exportCustomizationDefinition` → ファイルダウンロード
  - インポート: ファイル選択 → `importCustomizationDefinition` → 成功時一覧を
    再取得、失敗時（識別子不正・列挙値範囲外）はエラー内容を表示する
- **API**: `getCustomizationDefinition`/`exportCustomizationDefinition`/
  `importCustomizationDefinition`
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth role="ADMIN"`）

## 既存画面への追加表示（Unit 3の`ConnectionListScreen`）

スキーマ取込結果モーダルに、Unit 3の`SchemaImportResult.prunedCustomizationCount`
（Question 5で追加）を「陳腐化のため削除されたカスタマイズ定義: N件」として追加表示する。
