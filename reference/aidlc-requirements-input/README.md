# AI-DLC 初期要件インプット一式（使い方）

MasterMeister4と同種のプロダクトを新規にAI-DLCワークフローで開発する際、
Requirements Analysis等の各ステージでAIから出る確認質問を事前に潰しておくための
インプット文書一式。

## ファイル構成

| ファイル | 内容 |
|---|---|
| `00-project-overview.md` | 開発体制・優先順位・対象外範囲・拡張機能のopt-in方針 |
| `01-tech-and-architecture.md` | 技術スタック・データベースアーキテクチャ・データ永続化方針 |
| `02-functional-requirements.md` | 機能要件（機能ブロックごと） |
| `03-nfr.md` | 非機能要件 |
| `04-personas-and-glossary.md` | ユーザー・ペルソナ・ロール、用語集 |

## 使い方

1. 新プロジェクトのリポジトリを作成する。
2. このディレクトリ一式を、新リポジトリの `aidlc-docs/inception/requirements/input/` にコピーする
   （AI-DLCが自身の成果物を書き込む `aidlc-docs/inception/requirements/` 直下ではなく、
   サブディレクトリ `input/` に置いて自分たちの投入物と区別する）。
3. 各ファイルの `[記入]` プレースホルダを埋める。曖昧語（「標準的な」「柔軟に」等）は避け、
   範囲・タイミング・優先順位まで書く（詳しくは各ファイルの記入ガイド参照）。
4. AI-DLCワークフローを開始する最初のメッセージで、5ファイルすべてのパスを明示的に列挙して
   読み込みを指示する。例:

   > 新規にRDBMSマスタメンテナンスアプリケーションを開発したい。初期要件は以下のファイルに
   > まとめてあるので読み込んでください。
   > - aidlc-docs/inception/requirements/input/00-project-overview.md
   > - aidlc-docs/inception/requirements/input/01-tech-and-architecture.md
   > - aidlc-docs/inception/requirements/input/02-functional-requirements.md
   > - aidlc-docs/inception/requirements/input/03-nfr.md
   > - aidlc-docs/inception/requirements/input/04-personas-and-glossary.md

   （ワークスペースは自動的に全文書を探索するわけではなく「言及があれば」読み込む仕様のため、
   パスを省略しない。）

## 現行仕様（MasterMeister4）から変更したい事項の伝え方

新プロジェクトはgreenfield起動であり、AI-DLCはMasterMeister4のコードや過去のaidlc-docsを
直接参照できない。そのため「前回と同じ」「MasterMeister4を踏襲」だけでは要件として成立しない。

**ルール**: 各要件本文は必ずそれ単体で完結する形で書く（フルスペックで書く）。その上で、
MasterMeister4の仕様から変更した箇所にだけ、要件の直前に軽量タグを付けて人間側の追跡と
AIへの注意喚起に使う。

- `[継承]` — MasterMeister4と同じ仕様。そのまま踏襲する。
- `[変更]` — MasterMeister4の仕様から変更する。変更前後を一言で併記する。
- `[追加]` — MasterMeister4にはなかった新規要件。
- `[削除]` — MasterMeister4にあった機能を今回は持たない（対象外）。

タグ自体はAI-DLCの処理に影響しない注記なので、書き忘れても要件本文さえ自己完結していれば
問題ない。ただし変更意図を明示しておくと、AIが「なぜこの仕様なのか」を理解しやすくなり、
関連する確認質問が減る。

例（`02-functional-requirements.md` 内）:

```markdown
### 認証

[変更] JWTアクセストークンの有効期限をデフォルト10分→30分に変更する
（MasterMeister4は同時利用者10名規模の社内ツールだったが、今回は不特定多数向けの
公開サービスであり、頻繁な再認証によるUX低下を避けたいため）。

- アクセストークン: ステートレス。有効期限設定可能（デフォルト30分）
- リフレッシュトークン: ...(以下フルスペックで記述)...
```

## 記入時の注意（共通）

- 曖昧語（「標準的な」「柔軟に」「多言語対応」等）を使うときは、範囲・タイミング・優先順位の
  3点をセットで書く。書かないと高確率で追加の確認質問が発生する。
- 判定ロジックには必ず境界条件（閾値、優先順位、同点時の扱い）を書く。
- エラー時の挙動（オールオアナッシングか部分適用か）を機能ごとに明記する。
- `03-nfr.md`のセキュリティ・可用性の項目は、特に「未実装として割り切る」ものを明示すると
  質問が減る（MasterMeister4のIPアドレス記録・保持期間パージ機能等がその例）。
