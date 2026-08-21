# NFR Design Patterns — Unit 3: 対象RDBMSセットアップ

nfr-requirements.mdの各方針・tech-stack-decisions.mdの技術選定を、具体的な設計パターンに
落とし込む。

## Resilience / Scalability Patterns
N/A（NFR Design Plan参照。resiliency-baseline不適用、単一インスタンス・同時利用者数約10名
規模のため、本Unitでは特別な耐障害性・スケーリングパターンを導入しない）

## Performance Patterns

### HikariCP詳細設定（Question 5）
- `connectionTimeout`: 5秒（NFR Requirements Question 2の接続確認タイムアウトと統一）
- `maximumPoolSize`: 5、`minimumIdle`: 1（NFR Requirements Question 3）
- `maxLifetime`: デフォルト（30分）のまま
- `leakDetectionThreshold`: 10秒（本番相当設定。接続リークの早期検知に用いる）

## Security Patterns

### 接続パスワード暗号化（AES-256-GCM、Question 1）
- 暗号化のたびに`SecureRandom`で96bit IVを生成する（Unit 2の`SecureTokenGenerator`と
  同じ乱数源を再利用するパターン）
- 暗号文は「IV（12byte）+ 暗号化データ + 認証タグ」を連結した単一バイト列としてBase64
  エンコードし、`Connection.encryptedPassword`に保存する（別カラムでのIV管理はしない）
- 暗号鍵は環境変数（`MM5_CONNECTION_SECRET_KEY`、32byte以上）から取得する

### JDBC接続URL構築の安全性（Question 2）
- `host`・`databaseName`・`schemaNameHint`は、英数字・ハイフン・アンダースコア・ドットの
  みを許容する正規表現（`^[A-Za-z0-9._-]+$`）で登録時に検証する。範囲外の文字を含む場合は
  登録を拒否する（BR-1相当の入力検証を拡張）
- `port`は数値型（Integer）のパラメータとして受け取るため、文字列インジェクションの
  余地がない
- RDBMS種別（`rdbmsType`）ごとにJDBC URLテンプレートを用意し（例:
  `jdbc:mysql://{host}:{port}/{databaseName}`）、検証済みの値のみを埋め込む

### 接続エラーメッセージの分類（NFR Requirements Question 6）
- JDBCの`SQLException`を捕捉し、`SQLState`/例外の種類に応じて
  `UNREACHABLE_HOST`/`AUTHENTICATION_FAILED`/`TIMEOUT`/`UNKNOWN`のいずれかに分類する
- 分類コードとi18nメッセージキーのみを管理者に返し、例外の`getMessage()`はログにのみ
  出力する（SECURITY-09）

## スキーマ全置換のトランザクション境界（Question 6）
- 複数スキーマを一括取込する場合、スキーマ単位でトランザクションを区切る
- 1スキーマの取込・置換に失敗しても、既に成功した他スキーマの結果は確定させる
- `SchemaImportResult`は、スキーマごとの成功/失敗を含む形で返却し、失敗したスキーマは
  理由（分類コード）とともに報告する
