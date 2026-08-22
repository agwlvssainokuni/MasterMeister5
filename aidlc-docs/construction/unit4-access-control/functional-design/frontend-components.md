# Unit 4: アクセス制御 - Frontend Components

Unit 2/3で確立した「一覧＋モーダル、ADMIN限定ルート」パターンを踏襲する。

## ルーティング構成（追加分）

| パス | 画面 | レイアウト | 認証要否 |
|---|---|---|---|
| `/groups` | GroupManagementScreen | AppLayout配下 | 要（ADMINロールのみ） |
| `/permissions` | PermissionScreen | AppLayout配下 | 要（ADMINロールのみ） |

`AppLayout`の`navItems`に、ADMINロール限定で「グループ管理」「アクセス権限」を追加する。

## 1. GroupManagementScreen（US-2.7）

- **構造**: グループ一覧テーブル（グループ名／所属ユーザ数）、「グループを作成」ボタン
  （モーダルでグループ名入力）、各行に「改名」「削除」操作、行選択（または展開）で
  所属メンバー一覧（メールアドレス表示）とメンバー追加（ユーザ選択セレクト）／削除
  操作を表示する
- **状態**: `groups`（一覧）、`loading`、作成/改名モーダルの入力値・
  `submitting`/`errorMessage`、選択中グループの`selectedGroupId`、
  `members`（選択中グループの所属ユーザ一覧）
- **バリデーション**: グループ名必須、一意性エラーはAPIレスポンスをそのままモーダルに
  表示する
- **操作フロー**:
  - 作成/改名: モーダル送信 → `POST /api/admin/groups` /
    `PATCH /api/admin/groups/{groupId}` → 成功時一覧を再取得
  - 削除: 確認モーダル（カスケード削除される旨を明示） →
    `DELETE /api/admin/groups/{groupId}` → 成功時一覧を再取得
  - メンバー追加/削除: `POST /api/admin/groups/{groupId}/members` /
    `DELETE /api/admin/groups/{groupId}/members/{userId}` → 成功時メンバー一覧を再取得
- **API**: component-methods.mdの`createGroup`/`renameGroup`/`deleteGroup`/
  `addUserToGroup`/`removeUserFromGroup`に対応するAPI群
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth role="ADMIN"`）

## 2. PermissionScreen（US-2.4、US-2.5、US-2.6）

Question 8により「ツリー＋選択中Subjectへの一括設定」形式を採用する。

- **構造**:
  1. 接続選択（Select、ACTIVE状態の接続一覧。Unit 3の`GET /api/admin/connections`を利用）
  2. Subject選択（ラジオ切替: ユーザ／グループ、選択後にセレクトで対象を1件選ぶ）
  3. スキーマ/テーブル/カラムのツリー表示（Unit 3の`getSchema`相当APIを利用し、
     スキーマ→テーブル→カラムの階層で展開可能なツリーとして表示）
  4. 各ツリーノードに、主権限セレクト（NONE/READ/UPDATE、未設定＝空選択肢を含む）を
     表示する。スキーマ/テーブルノードのみ、補助権限チェックボックス（CREATE/DELETE、
     3値: 未設定/true/false）も表示する
  5. 「YAMLエクスポート」「YAMLインポート」ボタン（画面上部、接続単位）
- **状態**: `connections`（Select用一覧）、`selectedConnectionId`、`subjectType`
  （USER/GROUP）、`subjectOptions`（ユーザ一覧/グループ一覧）、`selectedSubjectId`、
  `schemaTree`（スキーマ/テーブル/カラム階層）、`permissionEntries`（選択中Subject×
  選択中接続の既存`PermissionEntry`一覧、ツリーの各行の初期値に反映）、`saving`、
  `importErrorMessage`
- **操作フロー**:
  - 接続・Subject選択時: 対象接続のスキーマツリー（未取込ならその旨を表示）と、
    選択中Subjectの既存権限設定を取得してツリーに反映する
  - ツリーノードの主権限/補助権限変更: 変更のたびに即時保存せず、ノード単位で
    「保存」操作（またはツリー全体の一括保存ボタン）により
    `POST /api/admin/permissions`（`setPrimaryPermission`/`setAuxiliaryPermission`に
    対応）を呼び出す
  - YAMLエクスポート: `GET /api/admin/connections/{connectionId}/permissions/export` →
    ファイルダウンロード
  - YAMLインポート: ファイル選択 → `POST /api/admin/connections/{connectionId}/permissions/import`
    → 成功時ツリーを再取得、失敗時（検証エラー/重複エラー）は`importErrorMessage`に
    エラー内容を表示する
- **API**: component-methods.mdの`setPrimaryPermission`/`setAuxiliaryPermission`/
  `resolveEffectivePermission`（画面表示用に既存設定を取得する用途）/`exportPermissions`/
  `importPermissions`に対応するAPI群、Unit 3の`GET /api/admin/connections`/スキーマ参照API
- **アクセス制御**: ADMINロールのみアクセス可能（`RequireAuth role="ADMIN"`）

### スキーマ未取込時の表示

選択した接続にスキーマ取込済みデータがない場合、ツリーの代わりに「この接続はまだ
スキーマが取り込まれていません。接続管理画面から取込を行ってください」というメッセージと
`/connections`へのリンクを表示する。
