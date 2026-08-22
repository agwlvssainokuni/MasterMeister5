# NFR Design Patterns — Unit 4: アクセス制御

nfr-requirements.mdの各方針・tech-stack-decisions.mdの技術選定を、具体的な設計パターンに
落とし込む。

## Resilience / Scalability Patterns

N/A。resiliency-baseline不適用、単一インスタンス・同時利用者数約10名規模のため、
本Unitでは特別な耐障害性・スケーリングパターンを導入しない（Unit 1〜3と同じ方針）。

## Performance Patterns

### 実効権限キャッシュ（Caffeine、Question 2）

- `PermissionCacheComponent`実装は単一の`Cache<CacheKey, EffectivePermission>`を保持する
- `CacheKey`（値オブジェクト）: `userId` + `connectionId` + `resourceLevel` +
  `schemaName` + `tableName`（nullable） + `columnName`（nullable）
- TTLなし、最大10,000エントリ（NFR Requirements Question 2）
- `invalidateByUser(userId)`: 該当`userId`を持つキーのみ削除
- `invalidateByGroup(groupId)`/`invalidateByConnection(connectionId)`: 同時利用者数
  約10名規模を踏まえ、`Cache#asMap()`を走査して条件に一致するキーを削除する方式とする
  （逆引きインデックス等の追加構造は持たない）。`invalidateByGroup`は、キャッシュキー
  自体にgroupId情報を持たないため、呼び出し時点でのグループ所属ユーザ一覧を取得し、
  各ユーザについて`invalidateByUser`相当の処理を行う

### 実効権限解決のクエリバッチング（Question 3）

- `resolveEffectivePermission(userId, resourcePath)`は以下の順で処理する:
  1. `GroupMembershipRepository`からユーザの所属グループID一覧を1クエリで取得する
  2. `PermissionEntryRepository`に、Subject条件（`(USER, userId)`＋
     `(GROUP, groupId)`×所属グループ数）をIN句としたクエリを1回発行し、対象接続・
     対象スキーマ配下の全`PermissionEntry`をまとめて取得する
  3. メモリ上で、business-logic-model.mdのアルゴリズム（ユーザ自身の階層フォールバック
     優先→未設定ならグループ合成）を適用する
- N+1クエリ（階層ごと・グループごとの個別クエリ）は発生しない設計とする

## Security Patterns

### 識別子入力検証（Question 4）

- Unit 3の`ConnectionSchemaServiceImpl#validateIdentifier`と同じ許可文字パターン
  （`^[A-Za-z0-9._-]+$`）による検証を、Unit 4側（`PermissionEntry`関連のservice実装）に
  `private static`メソッドとして複製する。共通化のための抽象化は行わない（現時点で
  2箇所のみであり、プロジェクトの既存方針＝過度な抽象化を避ける、に従う）
- YAMLインポート時の`schemaName`/`tableName`/`columnName`にこの検証を適用し、不正な値を
  含むエントリが1件でもあればインポート全体を拒否する

### YAML入出力の安全性（Question 5）

- Jackson YAMLモジュール（`YAMLFactory`）は標準構成のまま使用する。ポリモーフィック型
  解決機能（`enableDefaultTyping`等）は一切使わない
- YAML入出力用DTOはrecord型で定義する（`PermissionExportEntry`/`PermissionImportEntry`
  等）。デシリアライズはこれらのrecordへの型安全なマッピングのみで完結する

### グループ名・Subject識別子のYAML検証

- インポート時、`email`/`groupName`で解決できないSubjectが1件でもあれば、インポート
  全体を検証エラーとして拒否する（Functional Design Question 6、business-rules.md BR-17）

## Reliability Patterns

- YAMLインポートの全置換（削除→再構築）は単一トランザクション内で実行する。検証エラー
  （識別子不正・Subject未解決・重複エントリ）はトランザクション開始前に検出し、DBの
  状態を変化させない
- キャッシュ無効化（`invalidateByConnection`等）の呼び出しはトランザクションコミット後に
  行う（コミット前にキャッシュを無効化すると、ロールバック時に古いキャッシュが誤って
  破棄されるため）
