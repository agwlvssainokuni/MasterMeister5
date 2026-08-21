# Unit of Work (MasterMeister5)

unit-of-work-plan.mdの承認結果（00-project-overview.mdの優先順位を採用、基盤コンポーネントは
最初に必要となる機能Unitに同梱、パッケージ構成はハイブリッド方式）に基づく6つのUnit定義。
MasterMeister5は単一WARでデプロイするモノリスのため、各Unitはマイクロサービスではなく、
Per-Unit Loop（Functional Design〜Code Generation）を適用する開発順序上の論理グループ
（Module）である。

## コード構成方針（Greenfield）

- `backend`モジュール配下は **ハイブリッド方式** のパッケージ構成とする: コンポーネント単位の
  トップレベルパッケージ（例: `cherry.mastermeister5.useraccount`、
  `cherry.mastermeister5.accesscontrol`）の下に、レイヤーサブパッケージ（`controller`/
  `service`/`repository`/`entity`等）を持つ
- 横断的関心事（基盤コンポーネント）は独立したトップレベルパッケージとする（例:
  `cherry.mastermeister5.security`、`cherry.mastermeister5.platform`、
  `cherry.mastermeister5.audit`、`cherry.mastermeister5.notification`）
- `frontend`モジュールの構成（画面・状態管理単位）はUnits Generationの対象外とし、各Unitの
  Functional Design／Code Generationで確定する

---

## Unit 1: デザインシステム基盤

- **概要**: プロジェクト全体の技術的土台を整備する。他機能に先立って完了させる
- **スコープ**:
  - Gradleマルチモジュール構成（`backend`/`frontend`/`libs`）の初期構築
  - `make-you-chic-ui`・`java-mustache-processor`のgit submodule組み込み
  - 共通レイアウト（AppShell: Sidebar+Topbar+Content）の構成確定
  - テーマ機能: ライト/ダーク・文字サイズ（ブラウザストレージ保存の個人設定）、ブランドカラー・
    フォント（管理者設定、内部DB永続化、全利用者共通）
  - 構造化ログ基盤・i18n基盤（メッセージリソース解決の仕組み）の整備
- **含まれるコンポーネント**: PlatformInfrastructureComponent
- **対応ストーリー**: なし（技術基盤の整備であり、requirements.md §3由来。個別の番号付き
  ストーリーには対応しない）
- **完了の目安**: AppShellとテーマ切り替え（ライト/ダーク、ブランドカラー・フォント含む）が
  動作し、以降のUnitがこの上に機能を追加できる状態

## Unit 2: ユーザ管理

- **概要**: 招待制ユーザ登録・認証を実装する
- **スコープ**: requirements.md 4.1（ユーザ登録・認証）全体
- **含まれるコンポーネント**: UserAccountComponent、SecurityInfrastructureComponent、
  AuditLogComponent、NotificationComponent
- **対応ストーリー**: US-1.0 〜 US-1.10（11件）
- **完了の目安**: 招待→本登録→ログイン/ログアウト→パスワードリセット/変更→無効化/再有効化の
  一連のフローが動作し、監査ログに認証イベントが記録される状態

## Unit 3: 対象RDBMSセットアップ

- **概要**: 対象RDBMS接続とスキーマ取込を実装する
- **スコープ**: requirements.md 4.2（接続管理・スキーマ取込部分）
- **含まれるコンポーネント**: ConnectionSchemaComponent
- **対応ストーリー**: US-2.1, US-2.2, US-2.3（3件）
- **完了の目安**: 接続の登録・無効化、スキーマ取込・再取込が動作する状態

## Unit 4: アクセス制御

- **概要**: 多階層アクセス権限モデルとグループ管理を実装する
- **スコープ**: requirements.md 4.2（アクセス権限モデル部分）
- **含まれるコンポーネント**: AccessControlComponent、PermissionCacheComponent
- **対応ストーリー**: US-2.4, US-2.5, US-2.6, US-2.7（4件）
- **完了の目安**: 主権限/補助権限設定、グループ管理、実効権限合成・キャッシュ、YAML入出力が
  動作する状態

## Unit 5: データ表示（マスタメンテナンス）

- **概要**: マスタデータの閲覧・編集・カスタマイズを実装する
- **スコープ**: requirements.md 4.3（マスタメンテナンス機能）
- **含まれるコンポーネント**: MasterMaintenanceComponent
- **対応ストーリー**: US-3.1 〜 US-3.7（7件）
- **完了の目安**: 一覧表示・フィルタ・編集反映（オールオアナッシング）・作成・削除・
  カスタマイズ定義のYAML入出力が動作する状態

## Unit 6: その他機能（クエリ・監査ログ閲覧）

- **概要**: クエリビルダー・保存・実行・履歴、および監査ログ閲覧UIを実装する
- **スコープ**: requirements.md 4.4（クエリ関連機能）、4.5（監査ログ閲覧、記録機構自体はUnit 2
  で構築済み）
- **含まれるコンポーネント**: QueryComponent、AuditLogComponent（閲覧APIの追加実装）
- **対応ストーリー**: US-4.1 〜 US-4.6, US-5.1（7件）
- **完了の目安**: クエリビルダー・保存・実行・履歴、監査ログ閲覧画面が動作する状態
