# Infrastructure Design — Unit 2: ユーザ管理

Unit 1で確立済みの実行環境（クラウドプロバイダ不使用、自己完結型WAR/Dockerコンテナ、
`eclipse-temurin:25-jre`、H2ファイルベース永続化）をそのまま踏襲する。本ドキュメントでは
Unit 2固有の追加事項（DBマイグレーション、トークンのライフサイクル、Cookie運用、
セキュリティイベントのログ出力）を扱う。

## 内部データベース（H2）へのテーブル追加

- Flywayマイグレーションは、Unit 1からの連番を継続する（`V1`〜使用済みの続きから
  `user`・`password_reset_token`・`refresh_token`・`audit_event`の各テーブルを追加する
  スクリプトを追加、Question 1）。全Unit共通の単一マイグレーション履歴とし、Unit間で
  番号帯を予約しない
- 具体的なバージョン番号・DDLはCode Generationで確定する（Unit 1の既存マイグレーション
  ファイルの続き番号を実装時に確認する）

## トークンのライフサイクル・データ保持

- PasswordResetToken・RefreshTokenの期限切れ/失効済みレコードに対する自動クリーンアップ
  処理はUnit 2の時点では導入しない（Question 2、YAGNI）。同時利用者数約10名規模では
  データ量増加が実運用上の問題にならないと判断する
- AuditEventはSECURITY-14（ログ保持、最低90日）の対象であり、削除処理は設けない
  （閲覧APIはUnit 6で実装するが、記録・保持自体はUnit 2の責務）

## Cookie運用（リフレッシュトークン）

- リフレッシュトークンを格納するCookieには常にSecure属性を付与する（Question 3）。
  `http://localhost`は主要ブラウザ（Chrome/Firefox/Safari）が準セキュアコンテキストとして
  扱うため、`bootRun`・`npm run dev`によるローカル開発でもSecure Cookieは正常に送受信
  される
- 本番相当のDockerコンテナ実行時にTLS終端を行わない場合（コンテナへの直接アクセス）は
  Secureクッキーが送信されないため、本番運用時はリバースプロキシ等でのTLS終端を前提とする
  （requirements.md 3章の「将来的なTomcatへの実行可能WARデプロイ」時も同様の前提とする）

## セキュリティイベントのログ出力

- `AuditLogService.recordEvent`は、AuditEventテーブルへの記録と同時に、Unit 1確立済みの
  構造化ログ（Logback、SECURITY-03）にも同内容を出力する（Question 4）。ログイン失敗・
  アカウントロック・リフレッシュトークン再利用検知等のセキュリティイベントが、Dockerの
  ログドライバ経由でrequirements.md 5章の「ログベースの軽量な検知」の対象になる
- アラート配信の仕組み自体（ログ収集基盤側の設定）はUnit 2のスコープ外（運用フェーズ）

## N/A項目

Deployment Environment（クラウドプロバイダ）、Compute Infrastructure、Messaging
Infrastructure、Shared Infrastructure（マルチテナンシー）は、
unit2-user-management-infrastructure-design-plan.mdに記載の根拠（Unit 1と同一根拠）に
よりN/A。ロードバランサ・APIゲートウェイも単一インスタンス構成のためN/A。
