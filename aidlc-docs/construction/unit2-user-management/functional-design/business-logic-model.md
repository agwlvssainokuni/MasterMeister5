# Unit 2: ユーザ管理 - Business Logic Model

component-methods.md記載のUserAccountComponent/SecurityInfrastructureComponentのメソッドを
実現する業務フローを定義する。技術非依存（具体的なAPI形式・DB方式はCode Generationで確定）。

## 1. ユーザ招待（US-1.1）

1. 管理者が`inviteUser(Email, Role)`を実行する
2. 対象メールアドレスの既存Userレコードを検索する
   - ACTIVE状態が存在する → エラー（登録済み）を返す
   - INVITED状態が存在する → 重複登録操作としてエラーを返す（Question 9関連：自動再送は
     行わない。管理者は別途`resendInvitation`を呼ぶ必要がある）
   - 存在しない → 新規Userレコードを状態INVITEDで作成する
3. 招待トークンを生成し、ハッシュ化してUserレコードに保存する。有効期限を
   `invitedAt + 招待トークン有効期限（デフォルト3時間）`に設定する
4. NotificationComponent経由で招待メールを送信する
5. メール送信に失敗した場合も招待レコード自体はロールバックしない（Question 9推奨A）。
   送信失敗は管理者にエラーとして通知し、管理者は`resendInvitation`で再試行できる

## 2. 招待再送（US-1.2）

1. 管理者が`resendInvitation(InvitationId)`を実行する
2. 対象UserがINVITED状態であることを確認する（ACTIVE/DEACTIVATEDの場合はエラー）
3. 新しい招待トークンを生成し、有効期限を再設定する（旧トークンは上書きにより無効化される）
4. 招待メールを再送する

## 3. 本登録完了（US-1.6）

1. ユーザが招待リンク（トークン）にアクセスする
2. トークンのハッシュ値でUserレコードを検索し、以下を検証する
   - トークンが一致するINVITED状態のUserが存在するか
   - `invitationTokenExpiresAt`が現在時刻より後か（期限切れなら失敗、管理者による招待再送が
     必要である旨を案内する）
3. 検証成功後、氏名（必須、Question 10推奨A）とパスワード（パスワードポリシー準拠）を
   受け取る
4. パスワードポリシーを検証する: 最小8文字、SecurityInfrastructureComponent.
   `checkBreachedPassword`による漏洩パスワード照合
5. `hashPassword`でパスワードをハッシュ化し、Userレコードを更新する:
   status=ACTIVE、name設定、passwordHash設定、registeredAt設定、招待トークン関連フィールドを
   クリアする
6. AuditLogComponentに登録完了イベントを記録する

## 4. ロール変更（US-1.3）

1. 管理者が`changeRole(UserId, Role)`を実行する
2. 対象Userのroleを更新する（状態INVITED/ACTIVE/DEACTIVATEDいずれでも変更可能）
3. Question 3（推奨B）により、この変更は次回ログイン（またはトークンリフレッシュ）以降に
   発行されるJWTに反映される。管理者画面上のユーザ一覧表示は即座に新しいロールを表示する
4. AuditLogComponentにロール変更イベントを記録する

## 5. 無効化・再有効化（US-1.4, US-1.5）

1. 管理者が`deactivateUser(UserId)` / `reactivateUser(UserId)`を実行する
2. Userのstatusを DEACTIVATED / ACTIVE に更新する
3. `deactivateUser`実行時、対象ユーザの有効なRefreshTokenをすべて失効（`revokedAt`設定）
   する（Question 2推奨A：リフレッシュトークンは即座に失効させるが、既発行のアクセス
   トークン（JWT、デフォルト10分）自体は有効期限まで有効なままとする）
4. AuditLogComponentに無効化/再有効化イベントを記録する

## 6. ログイン（US-1.0, US-1.7）

1. ユーザが`authenticate(Email, Password)`を実行する
2. 対象Userを検索する。存在しない場合も「認証情報が正しくありません」という一般的な
   エラーを返す（メールアドレス列挙対策。ただし管理者向け招待時の重複検知はセキュリティ
   対策の対象外としてrequirements.md記載の通り明確に返す）
3. `lockedUntil`が現在時刻より後であればログイン失敗（ロック中）として拒否する
4. status が ACTIVE 以外（INVITED/DEACTIVATED）であればログイン失敗として拒否する
5. `verifyPassword`でパスワードを検証する
   - 失敗 → `recordLoginFailure`を呼び、`failedLoginCount`をインクリメントする。設定回数
     （デフォルト5回、Question 8: メールアドレス単位のみでカウント）に達したら
     `lockedUntil = now + ロック時間（デフォルト15分）`を設定する。AuditLogComponentに
     ログイン失敗イベントを記録する
   - 成功 → `failedLoginCount`をリセットし、`issueAccessToken`（ロールクレーム含む、
     Question 3）と`issueRefreshToken`（新規familyId発行、Question 6）を発行する。
     AuditLogComponentにログイン成功イベントを記録する
6. 初期管理者（US-1.0）は同じ`authenticate`フローに乗る。起動時の`ensureInitialAdmin`に
   よりACTIVE状態のUserレコードが事前に作成されているため、特別な認証経路は不要

## 7. トークンリフレッシュ・再利用検知（US-1.7）

1. クライアントが保持するリフレッシュトークンで`rotateRefreshToken(RefreshToken)`を呼ぶ
2. トークンのハッシュ値でRefreshTokenレコードを検索する
   - 見つからない、または`revokedAt`が設定済み（使用済み） → `detectReuseAndRevokeFamily`を
     呼び、同一`familyId`の全RefreshTokenを失効させる。AuditLogComponentに再利用検知
     イベントを記録し、リフレッシュを拒否する
   - `expiresAt`を過ぎている → リフレッシュ失敗（再ログインが必要）
   - 有効 → 現在のトークンを`revokedAt`設定で失効させ、同じ`familyId`で新しいRefreshToken・
     AccessTokenを発行する

## 8. ログアウト（US-1.8）

1. ユーザが`revokeRefreshToken(RefreshToken)`（ログアウトAPI）を実行する
2. 対象RefreshTokenの`revokedAt`を現在時刻に設定する（同一familyIdの他トークン＝他端末の
   セッションには影響しない）
3. AuditLogComponentにログアウトイベントを記録する

## 9. パスワードリセット申請・実行（US-1.9）

1. ユーザが`requestPasswordReset(Email)`を実行する
2. 対象Userの有無・状態にかかわらず、常に「メールを送信した」旨の応答を返す
   （Question 5推奨A：INVITED/ACTIVE/DEACTIVATED/存在しない、いずれの場合も応答は同一）
3. 対象Userが存在し、かつstatusがACTIVEの場合のみ実際にリセットトークンを発行しメールを
   送信する。INVITED（本登録未完了）やDEACTIVATED、存在しないメールアドレスの場合は
   実際には何も送信しない（列挙対策）
4. リセットトークン発行時、同一ユーザの未使用（`usedAt`が null）な既存トークンを失効させ、
   新しいPasswordResetTokenを作成する（有効期限デフォルト3時間、Question 4）
5. ユーザが`resetPassword(ResetToken, NewPassword)`を実行する
6. トークンのハッシュ値でPasswordResetTokenを検索し、`expiresAt`と`usedAt`（未使用である
   こと）を検証する
7. 検証成功後、パスワードポリシー検証（4.本登録と同様）を行い、Userのpasswordhashを更新、
   `usedAt`を設定する。当該ユーザの全RefreshTokenも予防的に失効させる（漏洩の疑いがある
   経路のため）
8. AuditLogComponentにパスワードリセット完了イベントを記録する

## 10. パスワード変更（US-1.10）

1. ログイン中ユーザが`changePassword(UserId, CurrentPassword, NewPassword)`を実行する
2. `verifyPassword`で現パスワードを検証する。不一致ならエラーとし変更しない
3. 新パスワードのパスワードポリシー検証を行う
4. passwordHashを更新する
5. AuditLogComponentにパスワード変更イベントを記録する

## 11. 初期管理者アカウント作成（起動時、US-1.0の前提）

1. アプリ起動時、`ensureInitialAdmin(AdminBootstrapConfig)`（環境変数由来）を実行する
2. 指定メールアドレスのUserが既に存在する場合は何もしない（Question 7推奨A）
3. 存在しない場合、status=ACTIVE、role=ADMINのUserレコードを直接作成する（招待フローを
   経ない）

## テスト対象プロパティ（PBT-01: property-based-testing拡張）

property-based-testing.mdのRule PBT-01に基づき、Unit 2の業務ロジックから識別した
プロパティテスト対象を列挙する。カテゴリはPBT-01記載の分類に対応する。

| 対象 | カテゴリ | プロパティ |
|---|---|---|
| パスワードハッシュ化・検証（`hashPassword`/`verifyPassword`） | Invariant | 任意のパスワード `p` に対し、`verifyPassword(hashPassword(p), p) = true` が常に成立する。異なる `p2 ≠ p` に対しては `verifyPassword(hashPassword(p), p2) = false` |
| User状態遷移（INVITED/ACTIVE/DEACTIVATED） | Invariant | domain-entities.mdの状態遷移図に定義されていない遷移（例: DEACTIVATED→INVITED、INVITED→DEACTIVATED直接）は常に拒否される |
| ログイン失敗カウント（`recordLoginFailure`） | Invariant | `failedLoginCount`は常に0以上。ログイン成功時は必ず0にリセットされる。設定回数に達した場合のみ`lockedUntil`が設定される |
| アカウントロック解除 | Idempotence | ロック中に複数回ログイン試行しても`lockedUntil`はさらに延長されない（ロック時間は最初の到達時点からのみ起算） |
| リフレッシュトークンローテーション（`rotateRefreshToken`） | Invariant | ローテーション後も同一`familyId`が引き継がれる。旧トークンは必ず失効し、有効なトークンは常に1つの子孫チェーンに1件のみ存在する |
| 再利用検知によるファミリ一括失効（`detectReuseAndRevokeFamily`） | Idempotence | 同一familyIdに対して複数回失効処理を実行しても結果は変わらない（既に失効済みのトークンへの再失効は無害） |
| パスワードリセットトークンの単一有効性 | Invariant | 同一ユーザに対し、常に未使用かつ有効期限内のリセットトークンは高々1件のみ存在する（新規発行時に既存未使用トークンが失効するため） |
| 招待トークンの有効期限判定 | Idempotence | 有効期限切れ判定を複数回実行しても結果は変わらない（副作用のない純粋な時刻比較） |

これらのプロパティはCode GenerationでのPBTテスト要件として引き継ぐ
（jqwikによる実装、backend/build.gradle.ktsに導入済み）。
