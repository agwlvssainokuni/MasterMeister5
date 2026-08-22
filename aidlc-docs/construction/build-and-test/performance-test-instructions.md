# Performance Test Instructions

対象: MasterMeister5（Unit 1〜6、全機能完了時点）

## 適用方針

requirements.md 5章およびUnit 1〜6のNFR Requirementsで一貫して確認済みの通り、
本プロジェクトは**単一インスタンス構成・同時利用者数約10名規模**の社内ツールを
前提としており、resiliency-baseline拡張は不適用（Requirements Analysis段階で
オプトアウト済み）。このため、JMeter/k6等の負荷試験ツールの導入や、
スループット・同時接続数を対象とした自動化された性能テストはスコープ外と
判断してきた（Unit 1〜6のNFR Design全てで同じ結論）。本Build and Testでも
この判断を変更しない。

## 定量的な性能要件（NFR Requirementsで確定済みのもの）

自動負荷試験の対象ではないが、以下は個別の実装内で確定済みの性能関連の制約であり、
単体テストで検証済みである:

| 項目 | 値 | 検証方法 | Unit |
|---|---|---|---|
| クエリ実行結果の上限件数 | 1,000件（`setMaxRows`） | `QueryServiceImplTest`のProperty（大量データ取得閾値の境界確認） | 6 |
| クエリ実行タイムアウト | 30秒（`setQueryTimeout`） | 実装のみ、負荷下でのタイムアウト発火は未検証 | 6 |
| 対象RDBMS接続プール | 最大5・最小アイドル1（HikariCP） | 実装のみ | 3 |
| 対象RDBMS接続確認タイムアウト | 5秒 | 実装のみ | 3 |
| レート制限（bucket4j） | IPアドレス単位、10トークン/60秒 | `RateLimitFilterTest` | 1 |
| ページングのデフォルト件数 | 50件（OFFSET/LIMIT） | 各種Controller/ServiceTest | 5・6 |

## 手動での軽量な性能確認手順（自動化なし）

負荷試験ツールは導入していないため、開発者が手動で以下を確認することを推奨する:

### 1. クエリ実行結果の打ち切り確認

```bash
# devenvのpostgresに1,000件超のテストデータを用意した上で
# 「クエリ」画面から SELECT * FROM <大量データテーブル> を実行し、
# 結果が1,000件で打ち切られ「結果が多いため一部のみ表示」が表示されることを確認する
```

### 2. レート制限の動作確認

```bash
# 同一IPから短時間に大量のログイン試行を行い、
# 一定回数を超えた時点で429 Too Many Requestsが返ることを確認する
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" -d '{"email":"test@example.com","password":"wrong"}'
done
```

## 結論

- **Status**: N/A（要件上、自動化された性能テストは対象外と判断済み）
- 将来的に想定利用者数が拡大する場合は、resiliency-baseline拡張の再評価
  （Requirements Analysisへの差し戻し）から着手する必要がある
