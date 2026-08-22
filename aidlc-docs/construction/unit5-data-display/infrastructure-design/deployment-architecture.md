# Deployment Architecture — Unit 5: データ表示

Unit 1〜4のdeployment-architecture.mdで確定済みの構成に変更はない。Unit 5は新規の
外部コンポーネント・経路を追加しないため、構成図はUnit 4のものをそのまま参照する。

## 開発環境・本番相当構成

Unit 4のdeployment-architecture.mdの構成図（backend → Caffeineキャッシュ/H2 Database/
MailPit/対象RDBMS）に変更はない。Unit 5が追加する`table_customization`/
`column_customization`/`validation_rule`テーブルはH2 Database内に追加されるのみで、
新たな構成要素の図示は不要である。

`SchemaImportedEvent`（Spring `ApplicationEventPublisher`）はアプリケーション
プロセス内で完結するため、構成図上に独立したコンポーネントとしては表現しない
（`backend`プロセス内部の処理として扱う）。

**備考**: ロードバランサ・APIゲートウェイは単一インスタンス構成のため配置しない
（Unit 1〜4と同じ方針）。
