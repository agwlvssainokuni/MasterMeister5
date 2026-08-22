# Infrastructure Design — Unit 5: データ表示

Unit 1〜4で確立済みの実行環境（クラウドプロバイダ不使用、自己完結型WAR/Dockerコンテナ、
`eclipse-temurin:25-jre`、H2ファイルベース永続化、単一インスタンス構成）をそのまま踏襲する。
本ドキュメントではUnit 5固有の追加事項（DBマイグレーション、イベント機構の位置づけ）を扱う。

## 内部データベース（H2）へのテーブル追加

- Flywayマイグレーションは、Unit 1〜4からの連番を継続する（`V14`から、
  `table_customization`・`column_customization`・`validation_rule`の各テーブルを
  追加する、Question 1）
- 具体的なDDLはCode Generationで確定する

## `SchemaImportedEvent`のインフラ的位置づけ（Question 2）

- Spring `ApplicationEventPublisher`によるアプリケーションプロセス内のインメモリ
  イベント機構であり、外部メッセージキュー・ブローカーは導入しない
- 単一インスタンス構成・同一トランザクション内の同期呼び出しであるため、配信保証・
  再送・永続化キュー等のインフラ的な考慮は不要
- 新規の依存関係追加は不要（Spring Frameworkの標準機能）

## 既存依存の再利用

Jackson YAMLモジュール（Unit 4で追加済み）をカスタマイズ定義のYAML入出力にそのまま
再利用する。新規ライブラリの追加はない。

## N/A項目

Deployment Environment（クラウドプロバイダ）、Compute Infrastructure、Networking
Infrastructure、Monitoring Infrastructure、Shared Infrastructure（マルチテナンシー）は、
unit5-data-display-infrastructure-design-plan.mdに記載の根拠（Unit 1〜4と同一根拠）
によりN/A。ロードバランサ・APIゲートウェイも単一インスタンス構成のためN/A。
