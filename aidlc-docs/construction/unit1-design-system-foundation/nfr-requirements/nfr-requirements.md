# NFR Requirements — Unit 1: デザインシステム基盤

## Scalability / Performance / Availability
requirements.md 5章に基づき、Unit 1固有の追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- 応答性能: 具体的な数値目標は定めない
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない

## Security（security-baseline拡張との対応）

Unit 1はプロジェクト全体の技術基盤を確立するUnitであるため、以下のSECURITYルールを
中心に対応方針を定める。

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | Logback + logstash-logback-encoderで構造化（JSON）ログを出力する（Question 3）。相関ID・ログレベル・メッセージを含み、機微情報（パスワード・トークン）は出力しない |
| SECURITY-04（HTTPセキュリティヘッダ） | Spring Securityの`HeaderWriter`設定でCSP・HSTS・X-Content-Type-Options・X-Frame-Options・Referrer-Policyを全HTMLエンドポイントに設定する（Question 1でSpring Security採用） |
| SECURITY-08（アプリケーションレベルアクセス制御） | Spring Securityのフィルタチェーンでデフォルト拒否・JWT検証を行う基盤を整備する。個々のエンドポイントの認可ルールはUnit 2以降のFunctional Design/Code Generationで定義する |
| SECURITY-09（ハードニング） | Spring Bootのエラーハンドリングをカスタマイズし、本番相当の設定ではスタックトレース・内部パスを応答に含めない |
| SECURITY-10（サプライチェーン） | GitHub Dependabotによる依存関係脆弱性スキャンをリポジトリ設定として有効化する（Question 2）。Gradleの依存関係ロック機能（`dependencyLocking`）を有効化し、lockファイルをコミットする |
| SECURITY-11（レート制限） | bucket4jによる軽量レート制限を、ログインだけでなく他の未認証公開エンドポイント（パスワードリセット申請、招待受諾等）にも適用する基盤を整備する（Question 4a） |
| SECURITY-15（例外処理・フェイルセーフ） | Spring Bootのグローバル例外ハンドラ（`@ControllerAdvice`）を整備し、汎用的なエラーレスポンス（内部詳細を含まない）を返す仕組みを構築する |

## Reliability
- グローバル例外ハンドラにより、未処理例外は必ず捕捉されログに記録される（SECURITY-15）
- 外部呼び出し（対象RDBMS接続、メール送信等）のエラーハンドリングは、各Unitの
  Functional Design/Code Generationで個別に定義する

## Maintainability
- テストフレームワーク: JUnit5 + Mockito（バックエンド）、Vitest + React Testing Library
  （フロントエンド）、PBT: jqwik（バックエンド）、fast-check（フロントエンド複雑ロジック時）
  — requirements.md 3章で既に確定済み
- 依存関係のロック・脆弱性スキャン方針は上記SECURITY-10参照

## Usability
requirements.md 5章の多言語対応（日英2言語、初期リリースから）・レスポンシブ対応方針に従う。
Unit 1ではi18n基盤（メッセージリソース解決の仕組み）とAppShellのレスポンシブレイアウトを
確立する。
