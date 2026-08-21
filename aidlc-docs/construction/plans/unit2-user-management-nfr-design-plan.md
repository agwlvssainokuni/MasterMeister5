# NFR Design Plan — Unit 2: ユーザ管理

Unit 2のNFR Requirements（パスワードハッシュ=BCrypt、既知漏洩パスワード照合=埋め込み静的
リスト、リフレッシュトークンハッシュ=SHA-256、JWT署名=HS256、監査ログ改ざん防止=
アプリケーションレベルのみ、AuditLogComponent=単一テーブル+JSON詳細列）を、具体的な
設計パターン・論理コンポーネントに落とし込む。

## 実行チェックリスト

- [ ] Step 1: nfr-requirements.md・tech-stack-decisions.mdを分析する
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: NFR Design成果物生成
  - [ ] `nfr-design-patterns.md`
  - [ ] `logical-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Resilience Patterns（耐障害性パターン）**: N/A。resiliency-baseline拡張は適用しない方針
  （requirements.md 5章）に加え、Unit 2のNFR Requirements Question 5でメール送信の自動
  リトライも不要と確定済み。リトライ・サーキットブレーカー等のパターンは導入しない
- **Scalability Patterns（スケーリングパターン）**: N/A。Unit 1のNFR Designと同じ根拠
  （単一インスタンス・同時利用者数約10名規模）により、オートスケーリング等のパターンは不要
- **Performance Patterns（性能最適化パターン）**: 一部該当。パスワードハッシュ
  （BCrypt）のコストパラメータは認証系APIの応答時間に直接影響するため、Question 1で
  具体化する。それ以外の性能目標値は定めない方針（requirements.md 5章）を維持する
- **Security Patterns（セキュリティ実装パターン）**: 該当。Question 2〜3で、ログインAPIの
  実装方式・フロントエンドでのトークン保存方式（functional-design/frontend-components.mdで
  NFR Design送りとした項目）を具体化する
- **Logical Components（論理コンポーネント）**: 該当。Question 4〜6で、JWT実装ライブラリ・
  招待/リセットトークンの生成方式・監査ログ記録の実装パターンを具体化する

---

## 質問

### Question 1: パスワードハッシュ（BCrypt）のコストパラメータ

A) （推奨）`BCryptPasswordEncoder`のデフォルト強度（strength=10）をそのまま採用する。
同時利用者数約10名規模では、より高いコストパラメータによるCPU負荷増よりも実装の単純さを
優先する

B) strength=12等、デフォルトより高いコストパラメータを明示的に設定する（セキュリティを
やや優先し、認証系APIの応答時間増を許容する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: ログインAPIの実装方式

A) （推奨）独自のログインコントローラ/サービスから`UserAccountComponent.authenticate`を
直接呼び出す。Spring SecurityのAuthenticationManager/UserDetailsService/
AuthenticationProviderは経由しない。ログインは公開エンドポイントでありSecurityContextを
必要としないため、Spring Securityの認証フレームワークに乗せる必要性が薄く、アカウント
ロック等のカスタムロジックを素直に実装できる

B) Spring SecurityのUserDetailsService + DaoAuthenticationProviderを実装し、
AuthenticationManager経由でログインを処理する（Spring Securityの標準機構に乗せる）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: フロントエンドでのトークン保存方式（functional-design/frontend-components.mdでNFR Design送りとした項目）

A) （推奨）リフレッシュトークンはHttpOnly + Secure + SameSite=StrictのCookieとして
サーバから発行する（`/api/auth/login`・`/api/auth/refresh`のレスポンスでSet-Cookie）。
アクセストークンはCookieにはせず、レスポンスボディで返しフロントエンドはメモリ
（Reactの状態等）にのみ保持する。JavaScriptからリフレッシュトークンを一切参照できないため
XSSによる窃取を防止する（SECURITY-12のセッション管理要件に沿う）

B) アクセストークン・リフレッシュトークンの双方をJSONレスポンスボディで返し、
フロントエンドはメモリにのみ保持する（Cookieを使わない）。実装はシンプルだが、
ページリロード時にセッションが失われ再ログインが必要になる

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: JWT実装ライブラリ

Unit 1で配置したプレースホルダ`NoopJwtTokenValidator`を実際のJWT発行・検証実装に
置き換える。使用するJWTライブラリは。

A) （推奨）`io.jsonwebtoken:jjwt`（jjwt）。APIがシンプルで導入実績が豊富

B) `com.nimbusds:nimbus-jose-jwt`。JOSE仕様への準拠度が高く高機能だが、APIがやや複雑

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 5: 招待/パスワードリセットトークンの生成方式

A) （推奨）`SecureRandom`で256bit（32byte）の乱数を生成し、Base64URL（パディングなし）
エンコードして招待/リセットリンクのトークン文字列とする

B) UUID（v4、約122bitのランダム性）をそのままトークン文字列として使う。実装が簡単だが
エントロピーがやや低い

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: 監査ログ記録（`AuditLogComponent.recordEvent`呼び出し）の実装パターン

A) （推奨）各業務メソッド（招待・ログイン・ロール変更等）内で、処理の完了直後に明示的に
`recordEvent`を呼び出す。記録漏れのリスクはメソッドごとのコードレビューで担保するが、
イベントごとに記録する詳細情報（対象・変更前後の値等）を柔軟に制御できる

B) Spring AOP（`@Around`アドバイス等）でサービスメソッド呼び出しを横断的に捕捉し、
自動的に記録する。記録漏れを防げるが、イベント固有の詳細情報（JSON詳細列の内容）を
横断的な仕組みだけで表現しにくい

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Design成果物を生成する。
