# Tech Stack Decisions — Unit 1: デザインシステム基盤

requirements.md 3章で既に確定済みの技術スタックに加え、Unit 1のNFR Requirementsで新たに
確定した項目を記録する。

## 既存確定事項（requirements.md 3章より、参考）
- バックエンド: Java 25 / Spring Boot 4.1 / Gradle 9.6（マルチモジュール）
- フロントエンド: Node.js 24 / React 19 / Vite
- 内部DB: H2 Database（JPA） / 対象RDBMS: NamedParameterJdbcTemplate
- テスト: JUnit5 + Mockito、Vitest + React Testing Library、PBT: jqwik / fast-check
- UIライブラリ: `make-you-chic-ui`（git submodule） / メール: `java-mustache-processor`（git submodule）

## Unit 1で新たに確定した項目

| 項目 | 選定 | 理由（Question） |
|---|---|---|
| 認証・認可基盤 | Spring Security（JWT用カスタムフィルタをフィルタチェーンに組み込み） | Question 1: Spring Bootとの親和性、CSRF/CORS/セキュリティヘッダの一元管理 |
| 依存関係脆弱性スキャン | GitHub Dependabot（リポジトリ設定） | Question 2: CI/CD未構築の現段階ではリポジトリ設定のみで運用開始できる |
| 構造化ログ | Logback + logstash-logback-encoder（JSON出力） | Question 3: 実装コストの低さ、コンテナ環境のログ収集との親和性 |
| レート制限 | bucket4j（アプリケーション内メモリ、ログイン以外の公開エンドポイントにも適用） | Question 4 / Question 4a: SECURITY-11準拠のため対象を拡大 |
| Gradle依存関係管理 | Gradle標準の`dependencyLocking`機能を有効化 | サプライチェーンセキュリティ（SECURITY-10）としてlockファイルをコミットする方針 |

## Unit 2以降への申し送り
- Spring SecurityのSecurityFilterChain設定・JWT検証フィルタの詳細実装は、Unit 2
  （ユーザ管理）のFunctional Design/Code Generationで具体化する
- bucket4jの適用対象エンドポイント一覧は、各Unitで新規に公開エンドポイントが追加される
  たびに更新する
