# NFR Design Plan — Unit 1: デザインシステム基盤

Unit 1のNFR Requirements（Spring Security、Logback+JSON、GitHub Dependabot、bucket4j、
Gradle dependencyLocking）を、具体的な設計パターン・論理コンポーネントに落とし込む。

## 実行チェックリスト

- [ ] Step A: nfr-requirements.md・tech-stack-decisions.mdを分析する
- [ ] Step B: 承認された回答（下記Clarifying Questions参照）を反映する
- [ ] Step C: `aidlc-docs/construction/unit1-design-system-foundation/nfr-design/nfr-design-patterns.md` を生成する
- [ ] Step D: `aidlc-docs/construction/unit1-design-system-foundation/nfr-design/logical-components.md` を生成する

## 既にN/A判定（回答不要、根拠を明記）

- **Resilience Patterns（耐障害性パターン）**: N/A。resiliency-baseline拡張は適用しない
  方針が確定済み（requirements.md 5章、NFR Requirements）。リトライ・サーキットブレーカー等の
  パターンは導入しない
- **Scalability Patterns（スケーリングパターン）**: N/A。単一インスタンス・同時利用者数
  約10名規模のため、オートスケーリング等のパターンは不要
- **Performance Patterns（性能最適化パターン）**: N/A。具体的な性能目標値を定めない方針が
  確定済み（requirements.md 5章）。JVM/コネクションプールのチューニングは各Unitの実装状況を
  見て個別対応とする

## Clarifying Questions

### Question 1: bucket4jのレート制限単位・閾値
bucket4jによるレート制限（NFR Requirements Question 4a）の制限単位と初期閾値をどう設定
しますか？
（AI推奨: A — 未認証の公開エンドポイント（ログイン・パスワードリセット申請・招待受諾）は
ユーザIDが確定していないため、IPアドレス単位が唯一実用的な制限軸である。閾値は
アカウントロックの閾値（5回/操作の性質上の目安）と大きく矛盾しない範囲で、業務利用を妨げない
程度に緩めに設定する）

A) IPアドレス単位、1分あたり10リクエストまで（トークンバケット、毎分補充）。エンドポイントに
   よらず共通の閾値からスタートし、必要に応じて後で調整する

B) IPアドレス単位だが、エンドポイントごとに異なる閾値を個別設計する（ログインは厳しめ、
   招待受諾は緩め等）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: CSP（Content-Security-Policy）の具体的な許可ソース
SECURITY-04のCSPポリシーについて、`default-src 'self'`を基本としつつ、追加で許可すべき
外部ソース（フォントCDN、画像CDN、外部API等）はありますか？
（AI推奨: A — `make-you-chic-ui`はフォント（Noto Sans/Serif JP）を同梱・自己完結させる
方針であり、外部CDNへの依存はrequirements.mdのどこにも記載がないため、`default-src 'self'`
のみで開始し、必要が生じた時点で個別に緩和する）

A) 追加の許可ソースなし。`default-src 'self'`のみとする（`script-src`/`style-src`等も
   `'self'`に統一し、`unsafe-inline`/`unsafe-eval`は使用しない）

B) 追加の許可ソースがある（Other欄に詳細を記述してください）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 相関ID（Correlation ID）の生成・伝播方式
構造化ログ（SECURITY-03）に含める相関IDは、どのように生成・伝播しますか？
（AI推奨: A — フロントエンドがリクエストごとにUUIDを生成しヘッダで送信する方式は
フロントエンド側の実装負担が増えるため、まずはバックエンドが単一のフィルタでリクエストごとに
生成する方式から始め、必要になれば拡張する）

A) バックエンドのServletフィルタでリクエストごとにUUIDを生成し、レスポンスヘッダにも
   付与する（フロントエンドからの伝播は求めない）

B) フロントエンドがリクエストごとに相関IDを生成し、専用HTTPヘッダ（例: `X-Correlation-Id`）
   で送信する。バックエンドはヘッダがあれば採用し、なければ生成する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: グローバル例外ハンドラのエラーレスポンス形式
SECURITY-15のグローバル例外ハンドラが返すエラーレスポンスの形式はどうしますか？
（AI推奨: A — OpenAPI仕様の自動生成（requirements.md 3章）と相性がよく、フロントエンドでの
エラー表示処理を一貫させやすい構造化フォーマットのため）

A) 統一エラーレスポンス構造（例: `{errorCode, message, correlationId}`）をJSON形式で返す。
   `message`はi18n対応（PlatformInfrastructureComponent経由でロケールに応じたメッセージを
   解決する）

B) HTTPステータスコードのみで表現し、レスポンスボディは最小限（詳細メッセージなし）とする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Mandatory Artifacts

- [ ] `aidlc-docs/construction/unit1-design-system-foundation/nfr-design/nfr-design-patterns.md`
- [ ] `aidlc-docs/construction/unit1-design-system-foundation/nfr-design/logical-components.md`
