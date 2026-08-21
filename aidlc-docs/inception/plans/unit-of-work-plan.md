# Unit of Work Plan (MasterMeister5)

Application Design（10コンポーネント）とrequirements.md・stories.mdを踏まえ、システムを
開発可能な単位（Unit of Work）に分割する。MasterMeister5は単一WARでデプロイするモノリスの
ため、各Unitはマイクロサービスではなく、開発順序・設計粒度を管理するための論理的な
グルーピング（Module）である。

## 実行チェックリスト

- [ ] Step A: requirements.md・stories.md・application-design.mdを踏まえ、コンポーネント・
      ストーリーの自然なグルーピング候補を洗い出す
- [ ] Step B: 承認された内訳方針（下記Clarifying Questions参照）に従いUnit境界を確定する
- [ ] Step C: `aidlc-docs/inception/application-design/unit-of-work.md` を生成する
      （Unit定義・責務・含まれるコンポーネント。greenfieldのためコード構成方針も含める）
- [ ] Step D: `aidlc-docs/inception/application-design/unit-of-work-dependency.md` を生成する
      （Unit間の依存関係マトリクス、開発順序）
- [ ] Step E: `aidlc-docs/inception/application-design/unit-of-work-story-map.md` を生成する
      （全32ストーリーがいずれかのUnitに割り当てられていることを確認）
- [ ] Step F: Unit境界・依存関係を検証する（循環依存がないか、全コンポーネント・全ストーリーが
      カバーされているか）

## 前提となる評価（回答不要・根拠を明記した判断）

- **Team Alignment（チーム編成に関する境界）**: N/A。00-project-overview.mdに「単独開発者に
  よる初期開発」と明記されており、複数チーム間の分担を考慮したUnit境界は不要
- **Technical Considerations（Unit間でスケーラビリティ/デプロイ要件が異なるか）**: N/A。
  01-tech-and-architecture.mdに「単一WARファイルとして自己完結型デプロイ」と明記されており、
  全Unitが同一デプロイ単位・同一スケーリング特性を共有する
- **Dependencies（Unit間の開発順序の扱い）**: CLAUDE.mdのPer-Unit Loopにより「各ユニットは
  設計・実装を完全に完了させてから次のユニットへ進む」ことが既定されているため、Unitの完了
  基準についての追加質問は不要。ただし依存方向（どのUnitを先に完了させる必要があるか）は
  unit-of-work-dependency.mdで明示する

## Clarifying Questions

### Question 1: Unitの分割方針
00-project-overview.mdに記載された開発優先順位（デザインシステム基盤 → ユーザ管理 →
対象RDBMSセットアップ → アクセス制御 → データ表示 → その他機能）をそのままUnit境界として
採用しますか？
（AI推奨: A — 優先順位は既にユーザーが明示した開発順序であり、各段階が前段階の成果物に
自然に依存する構造（例: アクセス制御は対象RDBMSセットアップの後）になっているため、追加の
並べ替えは不要）

A) 採用する。6つのUnit（デザインシステム基盤／ユーザ管理／対象RDBMSセットアップ／アクセス
   制御／データ表示／その他機能）とする

B) 一部統合する（例: 対象RDBMSセットアップとアクセス制御を1つのUnitにまとめる）

C) さらに細分化する（例: クエリ機能と監査ログを別々のUnitに分ける）

D) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 2: 横断的関心事（基盤コンポーネント）の配置
Application Designで定義した基盤コンポーネント（SecurityInfrastructure、PlatformInfrastructure、
AuditLog、Notification）は、それ単独のUnitにはせず、最初に必要となる機能Unitに含める形で
配置することを提案します。具体的には:
- PlatformInfrastructureComponent（構造化ログ・i18n基盤）→ Unit 1「デザインシステム基盤」
  （プロジェクトの技術的な土台として最初に整備する）
- SecurityInfrastructureComponent・AuditLogComponent・NotificationComponent → Unit 2
  「ユーザ管理」（招待・認証機能が最初にこれらを必要とするため）
- PermissionCacheComponent → Unit 4「アクセス制御」（実効権限モデルとともに導入）

この配置方針でよいですか？
（AI推奨: A — 基盤コンポーネントを独立Unit化すると「使う機能がまだ存在しない基盤だけの
Unit」ができてしまい、Unitごとに動くソフトウェアを完成させるという原則に反する。最初に
必要とする機能Unitに同梱する方が、各Unit完了時点で実際に動作する機能が揃う）

A) 上記の配置方針（最初に必要となる機能Unitに同梱）で進める

B) 基盤コンポーネントのみを独立した「Unit 0: 基盤」として先行させる

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 3: バックエンドのパッケージ構成方針
`backend`モジュール配下のJavaパッケージ構成はどちらの方針としますか？
（AI推奨: A — 機能（コンポーネント）ごとの凝集度を保ちながら、各機能内部でも役割が
追いやすいハイブリッド構成が、10コンポーネント規模のモノリスでは可読性・保守性のバランスが
よいため）

A) ハイブリッド: コンポーネント単位のトップレベルパッケージ（例: `useraccount`、
   `accesscontrol`）の下に、レイヤーサブパッケージ（`controller`/`service`/`repository`/
   `entity`等）を持つ

B) レイヤー優先: トップレベルを`controller`/`service`/`repository`等のレイヤーで分割し、
   その下にコンポーネント単位のサブパッケージを持つ

C) Other (please describe after [Answer]: tag below)

[Answer]:

## Mandatory Unit Artifacts

- [ ] `aidlc-docs/inception/application-design/unit-of-work.md`
- [ ] `aidlc-docs/inception/application-design/unit-of-work-dependency.md`
- [ ] `aidlc-docs/inception/application-design/unit-of-work-story-map.md`
