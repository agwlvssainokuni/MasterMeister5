# Infrastructure Design — Unit 4: アクセス制御

Unit 1〜3で確立済みの実行環境（クラウドプロバイダ不使用、自己完結型WAR/Dockerコンテナ、
`eclipse-temurin:25-jre`、H2ファイルベース永続化、単一インスタンス構成）をそのまま踏襲する。
本ドキュメントではUnit 4固有の追加事項（DBマイグレーション、Caffeine依存追加）を扱う。

## 内部データベース（H2）へのテーブル追加

- Flywayマイグレーションは、Unit 1〜3からの連番を継続する（`V11`から、`user_group`・
  `group_membership`・`permission_entry`の各テーブルを追加する、Question 1）
- 具体的なDDLはCode Generationで確定する

## Caffeine依存の追加（Question 2）

- `com.github.ben-manes.caffeine:caffeine`を`backend/build.gradle.kts`に追加する
- 外部キャッシュサーバ（Redis等）は導入しない。単一インスタンス構成のため、
  アプリケーションプロセス内のヒープメモリ上のキャッシュで要件を満たす
- 既存のGradle `dependencyLocking`・GitHub Dependabotの対象にそのまま含まれる
  （Unit 3のJDBCドライバ追加と同じ扱い）

## N/A項目

Deployment Environment（クラウドプロバイダ）、Compute Infrastructure、Messaging
Infrastructure、Networking Infrastructure、Monitoring Infrastructure、Shared
Infrastructure（マルチテナンシー）は、unit4-access-control-infrastructure-design-plan.md
に記載の根拠（Unit 1〜3と同一根拠）によりN/A。ロードバランサ・APIゲートウェイも単一
インスタンス構成のためN/A。
