# Services (MasterMeister5)

複数コンポーネントにまたがるユースケースのオーケストレーションを担うサービス層。
各サービスは1つ以上のコンポーネントのメソッドを呼び出して、ストーリー（stories.md）に
対応するユースケースを完結させる。

## InvitationService
**対応ストーリー**: US-1.1, US-1.2

- `UserAccountComponent.inviteUser` でユーザを招待済み状態にする
- `SecurityInfrastructureComponent`（トークン発行の一部として招待トークンの生成に相当する
  仕組みを利用、または招待トークン自体はUserAccountComponent内で発行しSecurityInfrastructure
  Componentのハッシュ化機構を利用）
- `NotificationComponent.renderTemplate` + `sendEmail` で招待メールを送信する
- `AuditLogComponent.recordEvent` で招待・再送イベントを記録する
- `PlatformInfrastructureComponent.getUserLocale`（招待先ユーザの言語が未確定な場合は
  デフォルト言語を使用）でメール文面の言語を決定する

## RegistrationService
**対応ストーリー**: US-1.6

- `UserAccountComponent.completeRegistration` で本登録を完了する
- `SecurityInfrastructureComponent.hashPassword` / `checkBreachedPassword` でパスワードを
  検証・保存する
- `AuditLogComponent.recordEvent` で本登録完了イベントを記録する

## AuthenticationService
**対応ストーリー**: US-1.0, US-1.7, US-1.8

- `UserAccountComponent.authenticate` / `recordLoginFailure` で認証情報とロック状態を判定する
- `SecurityInfrastructureComponent.issueAccessToken` / `issueRefreshToken` /
  `rotateRefreshToken` / `revokeRefreshToken` / `detectReuseAndRevokeFamily` でトークンを
  管理する
- `AuditLogComponent.recordEvent` でログイン・ログアウト・ログイン失敗イベントを記録する

## PasswordRecoveryService
**対応ストーリー**: US-1.9, US-1.10

- `UserAccountComponent.requestPasswordReset` / `resetPassword` / `changePassword`
- `SecurityInfrastructureComponent.hashPassword` / `checkBreachedPassword`
- `NotificationComponent` でリセットメールを送信する
- `AuditLogComponent.recordEvent` でパスワードリセット/変更イベントを記録する

## SchemaImportService
**対応ストーリー**: US-2.3

- `ConnectionSchemaComponent.importSchema` でスキーマを取込む
- `MasterMaintenanceComponent.pruneStaleCustomizations` で陳腐化したカスタマイズ定義を
  自動削除する
- `PermissionCacheComponent.invalidateByConnection` で実効権限キャッシュを無効化する
- `AuditLogComponent.recordEvent` でスキーマ取込操作とその結果を記録する

## PermissionManagementService
**対応ストーリー**: US-2.4, US-2.5, US-2.6, US-2.7

- `AccessControlComponent.setPrimaryPermission` / `setAuxiliaryPermission` /
  グループ管理メソッド / `exportPermissions` / `importPermissions`
- `PermissionCacheComponent.invalidateByUser` / `invalidateByGroup` で変更を反映する
- `AuditLogComponent.recordEvent` で権限変更・グループ変更・YAML入出力イベントを区別して記録する

## MasterDataUpdateService
**対応ストーリー**: US-3.1 〜 US-3.6

- `MasterMaintenanceComponent.listRecords` / `applyChanges`
- `AccessControlComponent.resolveEffectivePermission`（`PermissionCacheComponent`経由）で
  カラム単位の権限を検証してから反映する
- `ConnectionSchemaComponent.getSchema` で対象テーブル/カラム構造を参照する
- `AuditLogComponent.recordEvent` でデータ更新操作・大量データ取得イベントを記録する

## CustomizationDefinitionService
**対応ストーリー**: US-3.7

- `MasterMaintenanceComponent.getCustomizationDefinition` /
  `importCustomizationDefinition` / `exportCustomizationDefinition`
- `AccessControlComponent.resolveEffectivePermission` でREAD権限のない列が表示側に
  漏れないことを検証する
- `AuditLogComponent.recordEvent`

## QueryExecutionService
**対応ストーリー**: US-4.1 〜 US-4.6

- `QueryComponent.buildSql` / `parseSqlToBuilderState` / `saveQuery` / `retireQuery` /
  `executeQuery` / `detectParameters` / `listExecutionHistory`
- `ConnectionSchemaComponent.isSchemaAllowed` で対象スキーマを許可リスト検証する
- `AuditLogComponent.recordEvent` でクエリ実行イベント（結果件数・実行時間を含む）を記録する

## AuditReviewService
**対応ストーリー**: US-5.1

- `AuditLogComponent.listEvents`
- `PlatformInfrastructureComponent.resolveMessage` で操作種別等の表示文言をローカライズする
