# Unit 2: ユーザ管理 - NFR Requirements Plan

## 対象範囲

- Unit 2のFunctional Design成果物（`aidlc-docs/construction/unit2-user-management/functional-design/`）
- requirements.md 5章（非機能要件）のうち、Unit 2固有の技術選定・NFR確定が必要な項目

Unit 1のNFR Requirementsで既に確定済みの共通基盤（Spring Security導入、構造化ログ
（Logback + logstash-logback-encoder）、bucket4jによる全リクエスト対象の per-IP レート
制限、Gradle依存関係ロック、Dependabot）はそのまま踏襲し、本Planでは再度問わない。
同様に、同時利用者数（約10名）・応答性能目標（定めない）・可用性目標（定めない）等、
requirements.md 5章で確定済みでUnit 2固有の変更が不要な項目も再度問わない。

## 実行計画

- [ ] Step 1: Functional Design分析（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: NFR Requirements成果物生成
  - [ ] `nfr-requirements.md`
  - [ ] `tech-stack-decisions.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: パスワードハッシュアルゴリズム

business-rules.md BR-13「適応型ハッシュアルゴリズム」の具体的な選定。Spring Securityの
`PasswordEncoder`実装として何を採用するか。

A) （推奨）Argon2id（`Argon2PasswordEncoder`）。OWASP Password Storage Cheat Sheetの
第一推奨アルゴリズムであり、GPU/ASIC耐性が高い

B) BCrypt（`BCryptPasswordEncoder`）。Spring Securityのデフォルトであり実績が豊富

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2: 既知漏洩パスワード照合（`checkBreachedPassword`）のデータソース

A) （推奨）アプリケーション埋め込みの静的な既知漏洩/頻出パスワードリスト（数千〜数万件
規模）と照合する。外部ネットワーク依存を持たず、Docker等の閉域環境でも動作する

B) 外部API（Have I Been Pwned等のk-anonymity方式API）にパスワードのハッシュ接頭辞を
問い合わせる。より網羅的だが、外部ネットワーク到達性が前提となる

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: リフレッシュトークンのハッシュ方式

business-rules.md BR-19「ハッシュ化して保存」の具体的な方式。リフレッシュのたびに
高頻度で検索されるため、パスワードハッシュとは異なる方式が必要になる。

A) （推奨）SHA-256（高速な決定的ハッシュ）。リフレッシュトークン自体が十分なエントロピー
（256bit等）を持つランダム値であるため、低速な適応型ハッシュは不要と判断する

B) アプリケーション秘密鍵によるHMAC-SHA256（ペッパー付与）。DB漏洩時にトークン値の
推測をさらに困難にする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: JWTアクセストークンの署名アルゴリズム・鍵管理

A) （推奨）対称鍵HS256。単一WARのモノリス構成であり、トークンの発行者と検証者が同一
プロセス内にあるため、鍵配布の複雑さを伴う非対称鍵は不要と判断する。署名鍵は環境変数
経由で設定する

B) 非対称鍵RS256（秘密鍵で署名、公開鍵で検証）。将来的に別サービスがトークンを検証する
可能性に備える

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 招待/パスワードリセットメール送信失敗時のリトライ方針

business-logic-model.md「1. ユーザ招待」でメール送信失敗時も招待レコードは残す方針は
確定済みだが、自動リトライの要否を確認する。

A) （推奨）自動リトライは行わない。requirements.mdでresiliency-baseline拡張が不適用と
決定済みであることと整合させる。管理者の「招待再送」操作、ユーザの再度のリセット申請が
実質的なリトライ手段となる

B) 送信失敗時に指数バックオフ等で自動リトライする（例: 最大3回）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: 監査ログの改ざん防止の担保レベル（SECURITY-13/14: 改変・削除不可）

A) （推奨）アプリケーションレベルのみで担保する。AuditLogComponentは`recordEvent`
（作成のみ）と`listEvents`（参照のみ）のメソッドしか提供せず、更新・削除操作自体を
コード上に実装しない

B) アプリケーションレベルに加え、DBレベルでもアプリケーションの内部DB接続ユーザーから
UPDATE/DELETE権限を剥奪する（H2の権限設定で追加保証する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7: AuditLogComponentのデータモデル方針

Unit 2で監査ログの記録機構（永続化）を構築する（閲覧APIはUnit 6）。イベント種別ごとに
記録内容（対象UserId、実行者UserId、変更前後の値等）が異なる点をどう扱うか。

A) （推奨）単一の`AuditEvent`テーブルとし、イベント種別（enum）・実行者・対象・
タイムスタンプ等の共通列に加え、イベント固有の詳細情報はJSON列（`details`）に格納する。
将来Unit（3〜6）で追加されるイベント種別にもスキーマ変更なく対応できる

B) イベント種別ごとに個別のテーブルを設ける（例: `login_event`, `role_change_event`等）。
型安全性は高いが、Unit追加のたびにテーブル追加が必要になる

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8: 機微設定（初期管理者パスワード・JWT署名鍵・パスワードリセット用の
暗号材料等）の管理方法

A) （推奨）Unit 1で確立済みの`.env`/環境変数方式を踏襲する（`.env.example`に項目を
追加）。専用シークレットマネージャの導入はrequirements.mdのスコープ外とする

B) このタイミングでHashiCorp Vault等の外部シークレットマネージャ連携を設計に組み込む

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Requirements成果物を生成する。
