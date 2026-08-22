# NFR Requirements — Unit 4: アクセス制御

## Scalability / Performance / Availability

requirements.md 5章に基づき、Unit 4固有の特別な追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- 実効権限解決: 明示的な性能目標値（ms等）は設定しない（Question 3）。ただし
  キャッシュ未ヒット時の算出処理は、対象Subject（ユーザ＋所属全グループ）分の
  `PermissionEntry`を1回のクエリでまとめて取得してからメモリ上でフォールバック解決する
  設計とし、N+1クエリを避ける
- 実効権限キャッシュ（Caffeine）: TTLは設定せず、無効化イベント
  （権限設定変更/グループ構成変更/スキーマ再取込）でのみクリアする。最大エントリ数
  10,000件、あふれた場合はCaffeine標準のサイズベース退避に委ねる（Question 2）
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない（Unit 1〜3と同じ方針）
- デプロイ形態: 単一インスタンス構成（Unit 1〜3のInfrastructure Designで確定済み）を
  前提とするため、実効権限キャッシュはプロセスローカルなCaffeineで足りる
  （分散キャッシュ・キャッシュ同期の仕組みは不要）

## Security（security-baseline拡張との対応）

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | AuditLogService（Unit 2確立済み）が権限設定変更・グループ操作・YAMLエクスポート/インポートイベントを記録する（Question 5）。機微情報（パスワード等）を記録することはない |
| SECURITY-05（入力検証） | YAML内の`schemaName`/`tableName`/`columnName`は、Unit 3の`ConnectionSchemaServiceImpl#validateIdentifier`と同じ許可文字パターンで検証する。不正な値を含むインポートはエラーとして拒否する（Question 4） |
| SECURITY-09（ハードニング） | YAMLパースは型安全なデータバインディング（Jackson YAMLモジュールのDTOマッピング）のみを使用し、任意Javaクラスのインスタンス化を許可する設定は用いない（YAML deserialization攻撃対策、Question 4） |
| SECURITY-10（サプライチェーン） | 新規追加する`jackson-dataformat-yaml`、Caffeineも既存のGradle `dependencyLocking`・GitHub Dependabotの対象に含める（追加の仕組みは不要） |

## Reliability

- YAMLインポートは単一トランザクション内で全置換する（business-rules.md BR-19、
  Functional Design確定済み）。パース失敗・識別子検証エラー・重複エラーのいずれの場合も
  DBの状態は変化しない
- グループ削除・権限設定変更に伴うキャッシュ無効化の失敗（例外）はトランザクションを
  ロールバックする（キャッシュ無効化前にDBコミットが確定してしまう状態を避ける）

## Maintainability

- テストフレームワーク: JUnit5 + Mockito（バックエンド）、Vitest + React Testing Library
  （フロントエンド）— Unit 1〜3で確定済みの方針を踏襲
- property-based-testing拡張: functional-design/business-logic-model.mdの「テスト対象
  プロパティ」節（PBT-01）で識別した6件をCode Generationで実装する（jqwik使用）

## Usability

requirements.md 5章の多言語対応（日英2言語）に従い、`GroupManagementScreen`/
`PermissionScreen`の文言・エラーメッセージ（識別子検証エラー、重複エラー、Subject未解決
エラー等）をi18n基盤経由で多言語化する。
