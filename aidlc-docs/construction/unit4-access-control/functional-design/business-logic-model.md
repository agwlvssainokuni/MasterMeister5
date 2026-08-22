# Unit 4: アクセス制御 - Business Logic Model

component-methods.md記載のAccessControlComponent／PermissionCacheComponentのメソッドを
実現する業務フローを定義する。技術非依存（具体的なAPI形式・DB方式はCode Generationで確定）。

## 1. 主権限・補助権限の設定（US-2.4）

1. 管理者が`setPrimaryPermission(Subject, ResourcePath, PrimaryLevel)` /
   `setAuxiliaryPermission(Subject, ResourcePath, AuxiliaryFlags)`を実行する
2. 対象Subject（USER/GROUP、Question 3）・ResourcePath（Question 1）に一致する
   `PermissionEntry`が既存であれば更新、なければ新規作成する（upsert）
3. `setPrimaryPermission`は`primaryLevel`のみ、`setAuxiliaryPermission`は
   `auxCreate`/`auxDelete`のみを更新する（同一Subject×ResourcePathの行を両メソッドで
   共有するため、片方の呼び出しで他方の値を消さない）
4. AuditLogComponentに権限設定変更イベントを記録する
5. `PermissionCacheComponent#invalidateByUser` / `invalidateByGroup`
   （Subjectの種別に応じて）を呼び出し、実効権限キャッシュを無効化する（US-2.4）

## 2. グループ管理（US-2.7）

1. 管理者が`createGroup(GroupName)` / `renameGroup(GroupId, GroupName)` /
   `deleteGroup(GroupId)`を実行する
2. `createGroup`/`renameGroup`はグループ名の一意性を検証する
3. `deleteGroup`は以下をカスケードで削除する（トランザクション内）:
   - 当該グループの`GroupMembership`（全所属関係）
   - 当該グループがSubjectである`PermissionEntry`（全権限設定）
4. `addUserToGroup(GroupId, UserId)` / `removeUserFromGroup(GroupId, UserId)`は
   `GroupMembership`を作成/削除する
5. グループ削除・所属変更はいずれも`PermissionCacheComponent#invalidateByGroup` /
   影響を受けるユーザへの`invalidateByUser`を呼び出す（所属が変わったユーザは実効権限の
   合成結果が変わりうるため）
6. AuditLogComponentにグループ作成/改名/削除、所属追加/削除イベントを記録する

## 3. 実効権限の算出（US-2.4、`resolveEffectivePermission`）

`PermissionCacheComponent#getCached(UserId, ResourcePath)`にヒットしなければ以下の
アルゴリズムで算出し、`put`でキャッシュする。

### 3.1 主権限の算出

1. **ユーザ自身の直接設定を解決する**: 対象UserのPermissionEntry（subjectType=USER）
   のみを対象に、ResourcePathの階層フォールバック（COLUMN→TABLE→SCHEMA、Question 4）を
   適用し、最初に見つかった明示設定の`primaryLevel`を`userOwn`とする。ユーザ自身の
   どの階層にも明示設定がなければ`userOwn`は「未設定」
2. **グループ合成を解決する**: ユーザが所属する各グループについて、そのグループの
   PermissionEntry（subjectType=GROUP、当該グループ）のみを対象に同じ階層フォールバックを
   適用して`groupLevel`を求め、全グループの`groupLevel`のうち最も許可的な値（UPDATE >
   READ > NONE、requirements.md「より許可的な方で合成」）を`groupComposed`とする。
   所属グループがない、またはどのグループにも明示設定がなければ`groupComposed`は`NONE`
3. **最終判定**: `userOwn`が「未設定」でなければ`userOwn`を実効主権限とする
   （ユーザの明示的な個別設定はグループの合成結果より常に優先、requirements.md）。
   `userOwn`が「未設定」であれば`groupComposed`を実効主権限とする

### 3.2 補助権限の算出

主権限と同じ「ユーザ直接設定が優先、なければグループ合成」の原則を、階層フォールバック
TABLE→SCHEMA（COLUMN階層には補助権限が存在しないため）に適用して`canCreate`（AND対象は
下記レコード作成可否）／`canDelete`のベースとなる`auxCreate`/`auxDelete`をそれぞれ独立に
算出する。グループ合成はOR（いずれかのグループが真なら真）。

### 3.3 レコード作成・削除可否の最終判定（business-rules.md参照）

`EffectivePermission.canCreate`/`canDelete`は、3.2で求めた補助権限に加え、対象テーブルの
主キー構成列の実効主権限（3.1のロジックを各主キー列に適用）を合成して決定する
（requirements.md「レコード作成可否」「レコード削除可否」の定義）。

## 4. 実効権限キャッシュ（`PermissionCacheComponent`）

1. `getCached(UserId, ResourcePath)`はキャッシュ済みの`EffectivePermission`を返す
   （未キャッシュなら「なし」を返し、呼び出し元がSection 3のロジックで算出して`put`する）
2. キャッシュ無効化は以下の契機で発生する（requirements.md「権限設定・グループ構成・
   スキーマ再取込のいずれかが発生した場合はキャッシュを無効化する」、US-2.4）:
   - 権限設定変更（Section 1） → `invalidateByUser`（Subject=USER時）または
     `invalidateByGroup`（Subject=GROUP時、当該グループの現在の所属メンバー全員分を
     内部的に無効化する）
   - グループ構成変更（Section 2） → 影響を受けたユーザの`invalidateByUser`、
     グループ削除/権限変更時は`invalidateByGroup`
   - スキーマ再取込（Unit 3の`importSchema`） → `invalidateByConnection`
     （当該接続に紐づく全ユーザの実効権限キャッシュを無効化する）

## 5. YAMLエクスポート・インポート（US-2.5、US-2.6）

### エクスポート（`exportPermissions`）

1. 管理者が`exportPermissions(ConnectionId)`を実行する
2. 指定接続に紐づく全`PermissionEntry`を取得する
3. 各エントリのSubjectを、USER なら`User.email`、GROUP なら`UserGroup.name`に変換して
   YAML化する（Question 6。内部DBの自動採番IDはYAMLに含めない）
4. YAMLドキュメントを返す（ダウンロードはAPI層の責務）

### インポート（`importPermissions`）

1. 管理者が`importPermissions(ConnectionId, YamlDocument)`を実行する
2. YAML内の各エントリについて、`email`/`groupName`で対応する`User`/`UserGroup`を検索する。
   見つからないエントリが1件でもあれば、インポート全体を検証エラーとして拒否する
   （Question 6）
3. Subject（subjectType+識別子）×ResourcePathの組み合わせが2回以上出現する場合、
   インポート全体を重複エラーとして拒否する（Question 7）
4. 検証をすべて通過した場合のみ、単一トランザクション内で、指定接続に紐づく既存の
   `PermissionEntry`を全削除したうえで、YAMLの内容から再構築する（全置換、US-2.6）
5. `PermissionCacheComponent#invalidateByConnection`を呼び出す
6. AuditLogComponentにインポートイベント（件数サマリ）を記録する
7. `ImportResult`（取込件数、拒否理由があれば理由）を返す

グループ自体（`UserGroup`）はYAMLインポートでは作成されない。インポート対象のグループは
事前にUI（US-2.7）で作成済みである必要がある。

## テスト対象プロパティ（PBT-01: property-based-testing拡張）

| 対象 | カテゴリ | プロパティ |
|---|---|---|
| 実効主権限の合成 | Invariant | 実効主権限は、ユーザ自身の明示設定・所属グループの合成結果のいずれかであり、それ以外の値を取らない（NONE/READ/UPDATEの3値に閉じる） |
| ユーザ設定のグループ優先 | Invariant | ユーザ自身に明示設定がある場合、所属グループの権限がどれだけ許可的でも実効主権限はユーザ自身の設定と一致する |
| グループ合成のOR/MAX性 | Invariant | 複数グループ所属時の合成結果は、各グループ単独の実効権限のいずれよりも制限的にならない（より許可的な方向にのみ合成される） |
| 階層フォールバック | Invariant | COLUMN階層に明示設定がある場合、その値がTABLE/SCHEMAの設定内容によらず実効主権限として採用される |
| YAMLインポートの全置換 | Invariant | インポート成功後の`PermissionEntry`集合は、常にYAML内容と一致する（インポート前の残存データが混在しない） |
| YAMLインポートの重複拒否 | Invariant | Subject×ResourcePathの重複が1件でも存在するYAMLは、常にインポート全体が拒否され、DBの状態が変化しない |
