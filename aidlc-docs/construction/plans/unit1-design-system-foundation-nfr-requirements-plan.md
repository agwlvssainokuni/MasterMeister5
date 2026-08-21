# NFR Requirements Plan — Unit 1: デザインシステム基盤

Unit 1はFunctional Designをスキップしたため、requirements.md（3章 技術スタック・アーキテクチャ、
5章 非機能要件）とapplication-design.mdを直接分析する。

## 実行チェックリスト

- [ ] Step A: requirements.md 3章・5章、application-design.mdを分析し、Unit 1で確定すべき
      技術選定・非機能要件を洗い出す
- [ ] Step B: 承認された回答（下記Clarifying Questions参照）を反映する
- [ ] Step C: `aidlc-docs/construction/unit1-design-system-foundation/nfr-requirements/nfr-requirements.md` を生成する
- [ ] Step D: `aidlc-docs/construction/unit1-design-system-foundation/nfr-requirements/tech-stack-decisions.md` を生成する

## 既に確定済み・N/A判定（回答不要、根拠を明記）

- **Scalability/Performance/Availability**: requirements.md 5章に「同時利用者数約10名」
  「特別なSLAは定めない」「resiliency-baseline拡張は適用しない」と明記済みのため、Unit 1で
  追加の意思決定は不要
- **PBTフレームワーク選定（Tech Stack Selection）**: requirements.md 3章に「PBT: jqwik
  （バックエンド）、fast-check（フロントエンドに複雑ロジックがある場合）」と明記済み
- **Usability**: requirements.md 5章に多言語対応（日英2言語、初期リリースから）・
  レスポンシブ対応方針が明記済み。Unit 1のAppShell/テーマ機能はこの方針に従う

## Clarifying Questions

### Question 1: 認証・認可の実装基盤
JWT検証・エンドポイント認可（SECURITY-08: デフォルト拒否、ロールベースの認可）を、Spring
Securityのフィルタチェーンで実装しますか、それとも独自のインターセプタ/フィルタで実装しますか？
（AI推奨: A — Spring Bootエコシステムとの親和性が高く、CSRF・CORS・セッション管理等の
セキュリティヘッダ設定（SECURITY-04）もSpring Securityの設定として一元管理できるため）

A) Spring Securityを使用する（JWT用のカスタムフィルタをSpring Securityのフィルタチェーンに
   組み込む）

B) 独自の認可フィルタ/インターセプタを実装する（Spring Securityは使用しない）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 2: 依存関係の脆弱性スキャンツール（SECURITY-10）
依存関係の脆弱性スキャン（SECURITY-10）に使用するツールを選定してください。CI/CD構築自体は
開発最終段階に後回しですが、ローカル/手動実行可能な形でツール自体は早期に導入することを
想定しています。
（AI推奨: A — Gradleプラグインとしてローカル実行でき、CI/CD未構築の現段階でも
`./gradlew dependencyCheckAnalyze`等で単独運用できるため）

A) OWASP Dependency-Check Gradle Plugin

B) GitHub Dependabot（リポジトリ設定のみで、ローカル実行は不要な運用を想定）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 3: 構造化ログの実装方式（SECURITY-03）
アプリケーションログの構造化出力（JSON形式、相関ID付与）をどう実装しますか？
（AI推奨: A — Spring Bootの標準ロギング（Logback）にJSON出力用のエンコーダを追加する形が
実装コストが低く、コンテナ環境（Docker）でのログ収集とも親和性が高いため）

A) Logback + logstash-logback-encoderでJSON構造化ログを出力する

B) Spring Boot標準のログ設定（プレーンテキスト）をベースに、必要な項目を自前でフォーマットする

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 4: レート制限の実装方式（SECURITY-11）
公開エンドポイント（ログイン、パスワードリセット申請等）へのレート制限/スロットリングを
どう実装しますか？
（AI推奨: A — アプリケーション内メモリで完結する軽量ライブラリで、単一インスタンス構成
（同時利用者数10名規模）の本プロジェクトには過不足のない実装コストのため）

A) bucket4j等の軽量レート制限ライブラリをアプリケーション内に組み込む

B) UserAccountComponentのアカウントロック機構（ログイン失敗5回で15分ロック）のみで対応し、
   汎用的なレート制限ライブラリは導入しない

C) Other (please describe after [Answer]: tag below)

[Answer]:

## Mandatory Artifacts

- [ ] `aidlc-docs/construction/unit1-design-system-foundation/nfr-requirements/nfr-requirements.md`
- [ ] `aidlc-docs/construction/unit1-design-system-foundation/nfr-requirements/tech-stack-decisions.md`
