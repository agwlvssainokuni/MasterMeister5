# Infrastructure Design Plan — Unit 2: ユーザ管理

Unit 1のInfrastructure Designで確定済みの基盤（クラウドプロバイダ不使用・自己完結型WAR/
Dockerコンテナ、`eclipse-temurin:25-jre`ベースイメージ、H2ファイルベース永続化）をそのまま
踏襲する。Unit 2ではNFR Designの論理コンポーネント（UserAccountService、
JwtTokenProvider/JwtTokenValidatorImpl、RefreshTokenService、AuditLogService等）が新たに
必要とするストレージ・運用面の決定事項を扱う。

## 実行チェックリスト

- [ ] Step 1: functional-design/domain-entities.md、nfr-design/logical-components.md、
      Unit 1のinfrastructure-design.mdを分析する
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: Infrastructure Design成果物生成
  - [ ] `infrastructure-design.md`
  - [ ] `deployment-architecture.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。Unit 1で確定済み
  （requirements.md 3章「クラウドプロバイダは使用せず自己完結型WAR/Dockerコンテナ」）と
  同じ根拠。Unit 2固有の変更はない
- **Compute Infrastructure**: N/A。Unit 1と同じ根拠（単一インスタンス、同時利用者数
  約10名規模）。JWT検証・パスワードハッシュ計算はアプリケーションプロセス内で完結し、
  専用の計算リソースを追加する必要はない
- **Storage Infrastructure**: 該当。User/PasswordResetToken/RefreshToken/AuditEvent
  エンティティ（functional-design/domain-entities.md、nfr-design/logical-components.md）を
  Unit 1で確立済みのH2内部DB・Flywayマイグレーション基盤にどう追加するか、および期限切れ
  データの扱いをQuestion 1〜2で具体化する
- **Messaging Infrastructure**: N/A。Unit 1と同じ根拠（メッセージキュー等の非同期基盤は
  スコープ外、component-dependency.md）
- **Networking Infrastructure**: 一部該当。ロードバランサ・APIゲートウェイは単一インスタンス
  のためUnit 1と同じくN/Aだが、NFR DesignでリフレッシュトークンをSecure Cookieとして配布する
  方針（nfr-design-patterns.md）を採用したため、TLS終端のないローカル開発環境との両立を
  Question 3で具体化する
- **Monitoring Infrastructure**: 一部該当。requirements.md 5章「ログベースの軽量な検知の
  仕組み（ログイン失敗・認可失敗等のセキュリティイベント）」の実現方法をQuestion 4で
  具体化する。本格的な監視ダッシュボードはUnit 1と同じ根拠でN/A
- **Shared Infrastructure（マルチテナンシー等）**: N/A。Unit 1と同じ根拠（単一テナントの
  社内ツール）

---

## 質問

### Question 1: Flywayマイグレーションのバージョニング

Unit 1で`V1__...`から始まるFlywayマイグレーションを導入済み。Unit 2で追加するテーブル
（user、password_reset_token、refresh_token、audit_event等）のバージョン番号をどう
採番するか。

A) （推奨）Unit 1からの連番を継続する（Unit 1が`V1`〜`V3`程度を使用済みであれば
`V4`から開始する等）。全Unit共通の単一マイグレーション履歴とし、Flyway標準の運用方針に
従う

B) Unitごとにマイグレーション番号の帯（例: Unit 2は`V200`番台）を予約する。Unit間の
並行作業時に番号衝突を避けやすい

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 期限切れ・失効済みトークンのクリーンアップ方針

PasswordResetToken（使用済み/期限切れ）・RefreshToken（失効済み/期限切れ）は、
処理自体はbusiness-rules.mdで確定済みだが、DB上に蓄積され続ける。物理削除の要否を
確定する。AuditEventはSECURITY-14（ログ保持、最低90日）の対象のため対象外とする。

A) （推奨）Unit 2の時点では自動クリーンアップの仕組みは導入しない。同時利用者数
約10名規模でのデータ量増加は実運用上問題にならないと判断し、必要になった時点で
別途バッチ処理を追加する（YAGNI）

B) アプリ起動時または定期的（`@Scheduled`）に、期限切れ・失効済みのPasswordResetToken/
RefreshTokenを物理削除するクリーンアップ処理をUnit 2で実装する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: Secure Cookie属性とローカル開発環境（TLS未使用）の両立

nfr-design-patterns.mdでリフレッシュトークンをSecure属性付きCookieとして配布する方針を
確定済み。ローカル開発（`bootRun`、`npm run dev`のプロキシ経由）はHTTPで動作する。

A) （推奨）Cookieは常にSecure属性を付与する。ブラウザは`localhost`を準セキュアコンテキスト
として扱うため、`http://localhost`上での開発・動作確認はSecure属性付きCookieでも問題なく
機能する（Chrome/Firefox/Safari共通の挙動）。本番相当（Docker）はTLS終端をリバース
プロキシ等で行う前提とする

B) 環境変数でSecure属性の付与有無を切り替え可能にする（開発環境ではSecure属性を外す）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: セキュリティイベントのログベース検知（requirements.md 5章）

AuditLogComponent.recordEventで記録するセキュリティ関連イベント（ログイン失敗、
アカウントロック、リフレッシュトークン再利用検知等）を、requirements.md 5章が求める
「ログベースの軽量な検知の仕組み」でどう扱うか。

A) （推奨）AuditEventテーブルへの記録に加え、同じタイミングでUnit 1確立済みの構造化ログ
（Logback、SECURITY-03）にも同内容をログ出力する（`AuditLogService.recordEvent`内で
両方行う）。ログ収集基盤（コンテナのログドライバ等）側でのアラート設定は運用フェーズの
対象とし、Unit 2ではログに出力するところまでを実装範囲とする

B) AuditEventテーブルへの記録のみとし、構造化ログへの出力は行わない（Unit 6の監査ログ
閲覧画面で確認する運用とする）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Infrastructure Design成果物を生成する。
