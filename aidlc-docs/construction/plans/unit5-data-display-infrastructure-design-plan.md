# Infrastructure Design Plan — Unit 5: データ表示

Unit 1〜4で確定済みの基盤（クラウド不使用・自己完結型WAR/Dockerコンテナ、H2ファイルベース
永続化、devenvのDocker Compose、単一インスタンス構成）をそのまま踏襲する。Unit 5は
新規の外部インフラを導入しない。YAML処理（Jackson YAMLモジュール）はUnit 4で追加済みの
依存をそのまま再利用する。

## 実行チェックリスト

- [ ] Step 1: nfr-design/logical-components.mdを分析する（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: Infrastructure Design成果物生成
  - [ ] `infrastructure-design.md`
  - [ ] `deployment-architecture.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Deployment Environment（クラウドプロバイダ選定）**: N/A。Unit 1〜4と同じ根拠
- **Compute Infrastructure**: N/A。Unit 1〜4と同じ根拠（単一インスタンス）
- **Storage Infrastructure**: 該当。Unit 5で追加するテーブル（`table_customization`、
  `column_customization`、`validation_rule`）のFlywayマイグレーション追加方針を
  Question 1で具体化する
- **Messaging Infrastructure**: 該当（部分的）。NFR Designで確定したSpring
  `ApplicationEventPublisher`（プロセス内・同期イベント）は外部メッセージング基盤
  ではないため、インフラ的な追加要素はない。Question 2で明確化する
- **Networking Infrastructure**: N/A。Unit 5は新規の外部ネットワーク経路を追加しない
- **Monitoring Infrastructure**: N/A。Unit 1〜4と同じ根拠
- **Shared Infrastructure（マルチテナンシー等）**: N/A。Unit 1〜4と同じ根拠

---

## 質問

### Question 1: Flywayマイグレーションのバージョニング

Unit 1〜4で`V1`〜`V13`を使用済み。Unit 5で追加するテーブル（`table_customization`、
`column_customization`、`validation_rule`）のバージョン番号をどう採番するか。

A) （推奨）Unit 1〜4からの連番を継続する（`V14`から開始）。全Unit共通の単一
マイグレーション履歴とする方針を継続する

B) Unitごとにマイグレーション番号の帯を予約する

C) Other (please describe after [Answer]: tag below)

[Answer]: 

### Question 2: `SchemaImportedEvent`のインフラ的位置づけの確認

A) （推奨）Spring `ApplicationEventPublisher`はアプリケーションプロセス内のインメモリ
イベント機構であり、外部メッセージキュー・ブローカー（Kafka、RabbitMQ等）は
導入しない。単一インスタンス構成・同一トランザクション内の同期呼び出しであるため、
配信保証やインフラ的な冗長化の考慮は不要である

B) 将来のマルチインスタンス化を見越し、この時点で外部メッセージブローカーの導入を
検討する

C) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、Infrastructure Design成果物を生成する。
