# Application Design Plan (MasterMeister5)

`aidlc-docs/inception/requirements/requirements.md` と
`aidlc-docs/inception/user-stories/stories.md` を踏まえ、主要コンポーネント・サービス層の
高レベル設計を行う（詳細な業務ロジックはFunctional Design（Construction phase、ユニットごと）
で扱う）。

## 実行チェックリスト

- [ ] Step A: requirements.md 4章（機能要件）とstories.mdのEpic構成から、主要な業務機能領域を
      洗い出す
- [ ] Step B: 承認された粒度・分離方針（下記Clarifying Questions参照）に従い、コンポーネントの
      責務境界を確定する
- [ ] Step C: `components.md` を生成する（コンポーネント名・目的・責務・インターフェース概要）
- [ ] Step D: `component-methods.md` を生成する（各コンポーネントのメソッドシグネチャ、
      入出力の型、高レベルな目的。詳細な業務ルールはFunctional Designで扱うため含めない）
- [ ] Step E: `services.md` を生成する（サービス定義、責務、オーケストレーションパターン）
- [ ] Step F: `component-dependency.md` を生成する（依存関係マトリクス、コンポーネント間の
      通信パターン、データフロー）
- [ ] Step G: 設計の完全性・一貫性を検証する（requirements.md 4.1〜4.5の各要件がいずれかの
      コンポーネントで扱われているか、循環依存がないか）
- [ ] Step H: 上記4文書を統合した `application-design.md` を生成する

## Clarifying Questions

### Question 1: コンポーネントの粒度
コンポーネント分割の粒度をどの程度にしますか？
（AI推奨: A — requirements.mdの章構成（4.1〜4.5）は既に業務領域として整理されており、
これに横断的関心事（認証基盤・監査ログ出力・通知等）の基盤コンポーネントを加えた中程度の
粒度（8個前後）が、Units Generation・Functional Designへの橋渡しとして扱いやすいため）

A) 中粒度: requirements.mdの章構成（4.1〜4.5）を基本単位とし、横断的関心事（JWT認証基盤、
   通知/メール送信、監査ログ出力等）を独立した基盤コンポーネントとして追加する（8個前後）

B) 粗粒度: 4.1〜4.5をほぼそのまま5個程度のコンポーネントとし、横断的関心事は各コンポーネント
   に内包させる

C) 細粒度: 4.1〜4.5をさらに機能単位（例: 招待管理／ロール管理／トークン管理を別コンポーネント
   に分割）で10個以上に分割する

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: 接続・スキーマ管理とアクセス権限モデルの分離
requirements.md 4.2節（対象RDBMSセットアップ・アクセス制御）は、接続登録・スキーマ取込と、
主権限/補助権限・グループ・YAML入出力という2種類の責務を含んでいます。これらを別コンポーネント
に分離しますか？
（AI推奨: A — 前者は「対象RDBMSの構造をどう取得・保持するか」、後者は「誰が何にアクセスできる
かをどう管理・合成・キャッシュするか」と責務が異なり、依存の方向（権限モデル→スキーマ情報を
参照）も明確なため）

A) 分離する（例: 「接続・スキーマ管理コンポーネント」と「アクセス権限管理コンポーネント」）

B) 分離しない（4.2節全体を1つのコンポーネントとする）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: フロントエンドをApplication Designの対象に含めるか
自前デザインシステムライブラリ`make-you-chic-ui`の組み込みを含むフロントエンド（React）側の
コンポーネント構成も、このApplication Designステージで扱いますか？
（AI推奨: B — Application Designは「サービス層設計」に主眼を置くステージであり、フロントエンド
の画面・状態管理単位は各Unitに対応するFunctional Design、または実装計画（Code Generation）で
扱う方が、画面構成の詳細と実装が近い段階で決まり手戻りが少ないため）

A) 含める。フロントエンドのコンポーネント構成もこのステージで設計する

B) 含めない。このステージはバックエンドのコンポーネント/サービス層設計に絞り、フロントエンドは
   Units Generation・Functional Design・Code Generationで扱う

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 4: 横断的関心事の扱い
JWT認証基盤、暗号化（接続パスワード等）、実効権限キャッシュ（Caffeine）、構造化ログ出力、
i18n（多言語対応）といった横断的関心事を、独立した基盤コンポーネントとして明示しますか？
（AI推奨: A — ほぼ全機能コンポーネントから利用される共通基盤であり、独立したコンポーネントと
して明示した方が、component-dependency.mdでの依存関係の把握や、後続のNFR Design（Construction
phase）での参照がしやすいため）

A) 独立した基盤コンポーネントとして明示する

B) 明示せず、各機能コンポーネントの内部実装詳細として扱う（Application Designでは言及しない）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Mandatory Design Artifacts

- [ ] `aidlc-docs/inception/application-design/components.md`
- [ ] `aidlc-docs/inception/application-design/component-methods.md`
- [ ] `aidlc-docs/inception/application-design/services.md`
- [ ] `aidlc-docs/inception/application-design/component-dependency.md`
- [ ] `aidlc-docs/inception/application-design/application-design.md`（上記4文書の統合版）
