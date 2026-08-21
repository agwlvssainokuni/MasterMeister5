# Application Design (MasterMeister5) — 統合版

本書は `components.md`・`component-methods.md`・`services.md`・`component-dependency.md`
の内容を統合したサマリである。詳細は各ファイルを参照。

## 設計方針（application-design-plan.mdの承認結果）

- コンポーネント粒度: requirements.md 4.1〜4.5を基本単位とする中粒度＋横断的関心事を独立
  コンポーネント化（合計10個）
- requirements.md 4.2節は「接続・スキーマ管理」と「アクセス権限管理」に分離
- フロントエンド（React/`make-you-chic-ui`）はこのステージの対象外（Units Generation・
  Functional Design・Code Generationで扱う）
- 横断的関心事（JWT認証基盤、暗号化、実効権限キャッシュ、構造化ログ、i18n）は独立した
  基盤コンポーネントとして明示

## コンポーネント一覧

### ドメインコンポーネント（6）
| # | コンポーネント | 対応要件 |
|---|---|---|
| 1 | UserAccountComponent | 4.1 ユーザ登録・認証 |
| 2 | ConnectionSchemaComponent | 4.2（接続管理・スキーマ取込） |
| 3 | AccessControlComponent | 4.2（アクセス権限モデル） |
| 4 | MasterMaintenanceComponent | 4.3 マスタメンテナンス機能 |
| 5 | QueryComponent | 4.4 クエリ関連機能 |
| 6 | AuditLogComponent | 4.5 監査ログ |

### 基盤コンポーネント（4）
| # | コンポーネント | 役割 |
|---|---|---|
| 7 | SecurityInfrastructureComponent | JWT基盤、パスワードハッシュ化、接続情報暗号化 |
| 8 | PermissionCacheComponent | 実効権限キャッシュ（Caffeine） |
| 9 | PlatformInfrastructureComponent | 構造化ログ出力、i18nメッセージ解決 |
| 10 | NotificationComponent | メールテンプレート処理・送信 |

詳細な責務・インターフェース概要は [components.md](components.md) を参照。
メソッドシグネチャは [component-methods.md](component-methods.md) を参照。

## サービス層（オーケストレーション）

| サービス | 対応ストーリー |
|---|---|
| InvitationService | US-1.1, US-1.2 |
| RegistrationService | US-1.6 |
| AuthenticationService | US-1.0, US-1.7, US-1.8 |
| PasswordRecoveryService | US-1.9, US-1.10 |
| SchemaImportService | US-2.3 |
| PermissionManagementService | US-2.4〜US-2.7 |
| MasterDataUpdateService | US-3.1〜US-3.6 |
| CustomizationDefinitionService | US-3.7 |
| QueryExecutionService | US-4.1〜US-4.6 |
| AuditReviewService | US-5.1 |

詳細は [services.md](services.md) を参照。

## コンポーネント間依存関係

詳細な依存関係マトリクス・通信パターン・データフロー図は
[component-dependency.md](component-dependency.md) を参照。

要点:
- 基盤コンポーネント4つはドメインコンポーネントへ依存しないリーフノードであり、循環依存は
  存在しない
- ドメインコンポーネント間の直接依存は AccessControl→ConnectionSchema、
  MasterMaintenance→ConnectionSchema・AccessControl、Query→ConnectionSchema の3方向のみ

## 設計完全性・一貫性の検証（Step G）

| requirements.md セクション | 対応コンポーネント | カバー状況 |
|---|---|---|
| 4.1 ユーザ登録・認証 | UserAccountComponent（+SecurityInfrastructure, Notification, AuditLog） | ✅ |
| 4.2 接続・スキーマ管理 | ConnectionSchemaComponent | ✅ |
| 4.2 アクセス権限モデル | AccessControlComponent（+PermissionCache） | ✅ |
| 4.3 マスタメンテナンス機能 | MasterMaintenanceComponent | ✅ |
| 4.4 クエリ関連機能 | QueryComponent | ✅ |
| 4.5 監査ログ | AuditLogComponent | ✅ |
| 横断: JWT/暗号化 | SecurityInfrastructureComponent | ✅ |
| 横断: 構造化ログ/i18n | PlatformInfrastructureComponent | ✅ |
| 横断: メール送信 | NotificationComponent | ✅ |

抜け漏れは検出されなかった。循環依存も検出されなかった（依存関係マトリクス参照）。

## 留意事項（Functional Designへの申し送り）

- `QueryComponent`が`AccessControlComponent`の主権限/補助権限モデルと連携するか（対象スキーマ
  許可リストのみで足りるか、カラム単位のREAD権限もクエリ実行時に検証すべきか）は
  requirements.mdに明記がなく、Functional Designでの確認が必要
- `AuditLogComponent.recordEvent`を各業務トランザクションと同一トランザクションで確定させる
  か、非同期的に記録するかはNFR Design/Functional Designで確定する
