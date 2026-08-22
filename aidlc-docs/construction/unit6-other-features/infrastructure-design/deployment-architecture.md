# Deployment Architecture — Unit 6: その他機能

Unit 1〜5のdeployment-architecture.mdで確定済みの構成に変更はない。Unit 6は新規の
外部コンポーネント・経路を追加しないため、構成図はUnit 4・Unit 5のものをそのまま参照する。

## 開発環境・本番相当構成

Unit 4のdeployment-architecture.mdの構成図（backend → Caffeineキャッシュ/H2 Database/
MailPit/対象RDBMS）に変更はない。Unit 6が追加する`saved_query`/`query_execution_history`
テーブルはH2 Database内に追加されるのみで、新たな構成要素の図示は不要である。

クエリ実行時の対象RDBMSへの読み取り専用アクセスは、Unit 3の`ConnectionPoolRegistry`が
既に確立済みの「backend → 対象RDBMS」経路をそのまま利用する。新たな経路の追加はない。

**備考**: ロードバランサ・APIゲートウェイは単一インスタンス構成のため配置しない
（Unit 1〜5と同じ方針）。
