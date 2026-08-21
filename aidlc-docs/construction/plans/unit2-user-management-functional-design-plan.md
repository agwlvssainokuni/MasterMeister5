# Unit 2: ユーザ管理 - Functional Design Plan

## 対象範囲

- **Unit定義**: `aidlc-docs/inception/application-design/unit-of-work.md` Unit 2
- **対応ストーリー**: US-1.0 〜 US-1.10（`aidlc-docs/inception/user-stories/stories.md`）
- **含まれるコンポーネント**: UserAccountComponent、SecurityInfrastructureComponent、
  AuditLogComponent、NotificationComponent
- **参照**: requirements.md 4.1（ユーザ登録・認証）

## 実行計画

- [ ] Step 1: ユニットコンテキスト分析（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: Functional Design成果物生成
  - [ ] `business-logic-model.md`
  - [ ] `business-rules.md`
  - [ ] `domain-entities.md`
  - [ ] `frontend-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

requirements.md/stories.mdで「Functional Designで確定する」と明記されている項目、および
ステートレスJWT方式との整合上ビジネスルールとして確定が必要な項目について質問する。
各質問の推奨案には「（推奨）」を付記した。

### Question 1: ユーザの状態モデル

招待済み・未完了ユーザと本登録完了ユーザを、データモデル上どう表現するか。

A) （推奨）単一の`User`エンティティに状態（INVITED / ACTIVE / DEACTIVATED）を持たせる。
招待時にUserレコードを作成し、本登録完了時に同じレコードを更新する

B) `Invitation`エンティティと`User`エンティティを分離する。招待時はInvitationのみ作成し、
本登録完了時にUserレコードを新規作成する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: アカウント無効化時の既存トークンの扱い（US-1.4の未確定事項）

無効化されたユーザが有効なアクセストークン（JWT、デフォルト10分）を保持している場合、
無効化操作の効果をどこまで即時にするか。

A) （推奨）即時失効は行わない。アクセストークンは有効期限（デフォルト10分）まで有効の
まま失効させ、リフレッシュトークンのみ即座に失効させる（次回リフレッシュ以降ログイン
不可になる）。ステートレスJWTの簡潔性を優先する

B) アクセストークン検証時に毎回ユーザの無効化状態をDB照会し、無効化されていれば即座に
拒否する（ステートレスJWTの利点の一部を犠牲にしてでも即時性を優先する）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: ロール変更の反映タイミング

US-1.3では「変更が即座に反映され」とあるが、ステートレスJWTにロールを含める場合、
発行済みトークンにどう反映させるか。

A) JWTにロールを含めない。認可判定は毎リクエストDB（またはUnit4のPermissionCache）を
参照して最新ロールを確認する（Question 2でBを選んだ場合と一貫性を持たせやすい）

B) （推奨）JWTにロールを含める。ロール変更は次回ログイン（またはトークンリフレッシュ）
以降に反映される（Question 2の推奨案と一貫性がある。US-1.3の「即座」はUI上の管理者画面
表示の即時反映を指すものとして解釈する）

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 4: パスワードリセットトークンの有効期限デフォルト値（US-1.9の未確定事項）

A) （推奨）招待トークンと同じデフォルト3時間とする

B) より短いデフォルト1時間とする（パスワードリセットは招待より悪用時の実害が大きいため）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: 無効化ユーザに対するパスワードリセットの可否（US-1.9の未確定事項）

A) （推奨）無効化されたユーザのメールアドレスに対するリセット申請も、招待済み・未完了と
同様に常に「メールを送信した」という応答を返す（不可否のヒントを与えない）。ただし実際
には無効化ユーザ宛のメールは送信しない、またはリンクを踏んでも無効化されている旨を示し
パスワード変更はできないようにする

B) 無効化ユーザに対してもリセットを許可し、パスワードは変更できる（ただし無効化状態が
継続するためログインはできない）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: リフレッシュトークンの「トークンファミリ」識別方法

再利用検知でファミリ一括失効を行うため、ファミリをどう識別するか。

A) （推奨）ログイン成功時に新規`familyId`（UUID等）を発行し、以降のローテーションでは
同じfamilyIdを引き継ぐ。ログアウトや再利用検知時はfamilyId単位で一括失効する

B) 最初に発行されたリフレッシュトークンのID自体をファミリ識別子として使う（親子関係を
チェーンで辿って失効させる）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7: 初期管理者アカウントの冪等性

アプリ起動のたびに`ensureInitialAdmin`が呼ばれる想定だが、2回目以降の起動時の挙動は。

A) （推奨）環境変数で指定されたメールアドレスの管理者ユーザが既に存在すれば何もしない
（作成は初回のみ、以降は無視）

B) 環境変数で指定されたメールアドレスの管理者ユーザが存在すれば、パスワードを環境変数の
値で上書きする（環境変数変更時に反映させたい運用を想定）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8: ログイン失敗回数のカウント単位

A) （推奨）メールアドレス（アカウント）単位のみでカウントする。IPアドレス単位の制限は
設けない（requirements.mdの「同一アカウントへの」という記述、およびbucket4jによるAPI
レート制限（Unit1で導入済み）が別途IPベースの緩和策として機能するため）

B) メールアドレス単位に加えて、IPアドレス単位でもログイン試行を制限する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 9: 招待メール送信失敗時の挙動

`inviteUser`実行中にメール送信（NotificationComponent経由）が失敗した場合、招待自体を
どう扱うか。

A) （推奨）招待レコードの作成とメール送信を同一トランザクションとはみなさない。招待
レコードは作成済みのまま残し、送信失敗はエラーとして管理者に通知する（管理者は「招待
再送」操作で再試行できる）

B) メール送信失敗時は招待レコード自体をロールバックし、招待操作全体を失敗として扱う
（管理者は最初からやり直す）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 10: 本登録時のユーザ情報（氏名等）の項目・必須性

US-1.6「氏名等のユーザ情報」の具体的な項目は。

A) （推奨）氏名（表示名、1項目・必須）のみとする。詳細なプロフィール項目（部署等）は
requirements.mdのスコープ外

B) 氏名に加えて、任意項目としてUI表示言語（ロケール、Unit1のPlatformInfrastructure
Componentが管理）もこのタイミングで設定できるようにする

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Functional Design成果物を生成する。
