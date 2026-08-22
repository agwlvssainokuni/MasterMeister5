# NFR Requirements — Unit 5: データ表示

## Scalability / Performance / Availability

requirements.md 5章に基づき、Unit 5固有の特別な追加要件はない。
- 同時利用者数: 約10名（全Unit共通）
- 実効権限判定のバッチ化: `listRecords`はテーブルの全カラムをまとめて判定する新規
  メソッド（`resolveEffectivePermissionsForTable`）をUnit 4に追加し、DB取得を1回に
  抑える（Question 1）
- レコード一覧はキャッシュしない（マスタデータの鮮度を優先、Question 5）。
  ページングはOFFSET/LIMIT方式、デフォルト50件（Functional Design Question 9）
- 可用性: 特別なSLAは定めない。resiliency-baseline拡張は適用しない（Unit 1〜4と
  同じ方針）

## Security（security-baseline拡張との対応）

| ルール | 対応方針 |
|---|---|
| SECURITY-03（アプリケーションログ） | AuditLogService（Unit 2確立済み）がレコード変更（作成/更新/削除件数のサマリ）・カスタマイズ定義のYAMLエクスポート/インポートイベントを記録する（Question 6） |
| SECURITY-05（入力検証） | フィルタ条件の値は`NamedParameterJdbcTemplate`のバインドパラメータとして渡す。識別子（スキーマ名・テーブル名・カラム名）はUnit 3の`validateIdentifier`と同じ許可文字パターンで検証する（Question 2） |
| SECURITY-09（ハードニング） | 手入力WHERE/ORDER BY句は、セミコロン・SQLコメント開始（`--`、`/*`）を検出するブロックリスト方式で拒否する（Question 3）。専用SQLパーサは導入しない |

## Reliability

- `applyChanges`は、全`RecordChange`の権限チェック・ValidationRule検証を先に完了
  させたうえで、1つのDBトランザクション内で実際の変更を実行する（Question 4）。
  検証エラーがあればDBに一切変更を加えない（オールオアナッシング）
- カスタマイズ定義YAMLインポートも、Unit 3/4と同じ単一トランザクション全置換方式
  とする

## Maintainability

- テストフレームワーク: JUnit5 + Mockito + jqwik（バックエンド）、Vitest + React
  Testing Library（フロントエンド）— Unit 1〜4で確定済みの方針を踏襲
- property-based-testing拡張: functional-design/business-logic-model.mdの「テスト対象
  プロパティ」節（PBT-01）で識別した6件をCode Generationで実装する（jqwik使用）

## Usability

requirements.md 5章の多言語対応（日英2言語）に従い、`MasterDataScreen`/
`CustomizationScreen`の文言・エラーメッセージ（検証エラー、オールオアナッシング
拒否時のメッセージ等）をi18n基盤経由で多言語化する。
