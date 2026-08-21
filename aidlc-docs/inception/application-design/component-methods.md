# Component Methods (MasterMeister5)

各コンポーネントのメソッドシグネチャ（高レベル）。詳細な業務ルール・境界条件はFunctional
Design（Construction phase、ユニットごと）で定義する。型は概念的な名称で示す
（実装時の具体的な型はCode Generationで確定）。

## 1. UserAccountComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `inviteUser` | Email, Role | InvitationId | ユーザを招待する |
| `resendInvitation` | InvitationId | void | 招待を再送する |
| `completeRegistration` | InvitationToken, UserProfile, Password | UserId | 招待リンクから本登録を完了する |
| `changeRole` | UserId, Role | void | ユーザのロールを変更する |
| `deactivateUser` | UserId | void | ユーザを無効化する |
| `reactivateUser` | UserId | void | ユーザを再有効化する |
| `authenticate` | Email, Password | AuthenticationResult | 認証情報を検証する（ロック状態判定を含む） |
| `recordLoginFailure` | Email | void | ログイン失敗を記録しロック要否を判定する |
| `requestPasswordReset` | Email | ResetToken | パスワードリセットを申請する |
| `resetPassword` | ResetToken, NewPassword | void | 新しいパスワードを設定する |
| `changePassword` | UserId, CurrentPassword, NewPassword | void | ログイン中ユーザがパスワードを変更する |
| `ensureInitialAdmin` | AdminBootstrapConfig | UserId | 初回起動時に初期管理者を作成する |

## 2. ConnectionSchemaComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `registerConnection` | ConnectionConfig | ConnectionId | 対象RDBMS接続を登録する |
| `deactivateConnection` | ConnectionId | void | 接続を論理削除（無効化）する |
| `importSchema` | ConnectionId | SchemaImportResult | スキーマを取込（再取込含む）する |
| `getSchema` | ConnectionId | SchemaDefinition | 保持しているスキーマ情報を取得する |
| `isSchemaAllowed` | ConnectionId, SchemaName | boolean | クエリ実行時の対象スキーマ許可リスト検証 |

## 3. AccessControlComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `setPrimaryPermission` | Subject, ResourcePath, PrimaryLevel | void | 主権限を設定する（Subject=ユーザ/グループ） |
| `setAuxiliaryPermission` | Subject, ResourcePath, AuxiliaryFlags | void | 補助権限を設定する |
| `createGroup` / `renameGroup` / `deleteGroup` | GroupName / GroupId | GroupId / void | グループを管理する |
| `addUserToGroup` / `removeUserFromGroup` | GroupId, UserId | void | グループ所属を管理する |
| `resolveEffectivePermission` | UserId, ResourcePath | EffectivePermission | 実効権限を合成・返却する |
| `exportPermissions` | ConnectionId | YamlDocument | 権限設定をYAMLでエクスポートする |
| `importPermissions` | ConnectionId, YamlDocument | ImportResult | 権限設定をYAMLで全置換インポートする |

## 4. MasterMaintenanceComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `listRecords` | ConnectionId, TableRef, FilterCriteria, SortCriteria, Page | RecordPage | レコード一覧を取得する |
| `applyChanges` | ConnectionId, TableRef, RecordChangeSet | ApplyResult | 変更を単一トランザクションで一括反映する |
| `getCustomizationDefinition` | ConnectionId, TableRef | CustomizationDefinition | カスタマイズ定義を取得する |
| `importCustomizationDefinition` | ConnectionId, YamlDocument | ImportResult | カスタマイズ定義をYAMLでインポートする |
| `exportCustomizationDefinition` | ConnectionId | YamlDocument | カスタマイズ定義をYAMLでエクスポートする |
| `pruneStaleCustomizations` | ConnectionId, SchemaImportResult | PruneResult | 陳腐化したカスタマイズ定義を自動削除する |

## 5. QueryComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `buildSql` | QueryBuilderState | SqlText | クエリビルダーの指定からSQLを生成する |
| `parseSqlToBuilderState` | SqlText | QueryBuilderState | SQLをクエリビルダー構造へ逆変換する |
| `saveQuery` | SqlText, Name, Visibility | SavedQueryId | クエリを保存する |
| `retireQuery` | SavedQueryId | void | 保存クエリを論理的に非表示にする |
| `executeQuery` | SqlText \| SavedQueryId, ConnectionId, SchemaName, Params | QueryResult | クエリを実行する（読み取り専用検証を含む） |
| `detectParameters` | SqlText | ParameterDescriptor[] | `:param`形式のパラメータを自動検出する |
| `listExecutionHistory` | FilterCriteria, Page | ExecutionHistoryPage | クエリ実行履歴を取得する |

## 6. AuditLogComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `recordEvent` | AuditEvent | void | 監査イベントを記録する |
| `listEvents` | FilterCriteria, Page | AuditEventPage | 監査ログを閲覧・絞込する（管理者限定） |

## 7. SecurityInfrastructureComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `issueAccessToken` | UserId | AccessToken | アクセストークンを発行する |
| `issueRefreshToken` | UserId | RefreshToken | リフレッシュトークンを発行する |
| `rotateRefreshToken` | RefreshToken | RefreshToken | リフレッシュトークンをローテーションする |
| `revokeRefreshToken` | RefreshToken | void | リフレッシュトークンを失効させる（ログアウト時） |
| `detectReuseAndRevokeFamily` | RefreshToken | void | 再利用検知時にトークンファミリを一括失効させる |
| `validateAccessToken` | AccessToken | TokenClaims | アクセストークンを検証する |
| `hashPassword` / `verifyPassword` | Password | PasswordHash / boolean | パスワードをハッシュ化・検証する |
| `checkBreachedPassword` | Password | boolean | 既知漏洩パスワードリストと照合する |
| `encryptConnectionSecret` / `decryptConnectionSecret` | Secret | CipherText / Secret | 接続パスワードを可逆暗号化・復号する |

## 8. PermissionCacheComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `getCached` | UserId, ResourcePath | EffectivePermission? | キャッシュされた実効権限を取得する |
| `put` | UserId, ResourcePath, EffectivePermission | void | 実効権限をキャッシュする |
| `invalidateByUser` / `invalidateByGroup` / `invalidateByConnection` | UserId / GroupId / ConnectionId | void | 変更契機ごとにキャッシュを無効化する |

## 9. PlatformInfrastructureComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `log` | LogLevel, Message, CorrelationId | void | 構造化ログを出力する（機微情報を含まないことを保証） |
| `resolveMessage` | MessageKey, Locale, Params | LocalizedText | ロケールに応じたメッセージを解決する |
| `getUserLocale` | UserId | Locale | ユーザのUI表示言語設定を取得する |
| `setUserLocale` | UserId, Locale | void | ユーザのUI表示言語設定を内部DBへ保存する |
| `getAppTheme` | — | AppTheme(BrandColor, Font) | アプリ全体のテーマ設定（ブランドカラー・フォント）を取得する |
| `setAppTheme` | AppTheme(BrandColor, Font) | void | 管理者がアプリ全体のテーマ設定を変更する（Units Generation Step Fで追加） |

## 10. NotificationComponent

| メソッド | 入力 | 出力 | 目的 |
|---|---|---|---|
| `renderTemplate` | TemplateId, Locale, Params | RenderedContent | メールテンプレートを処理する |
| `sendEmail` | RecipientEmail, RenderedContent | void | メールを送信する |
