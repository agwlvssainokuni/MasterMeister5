# Unit 3: 対象RDBMSセットアップ - NFR Requirements Plan

## 対象範囲

- Unit 3のFunctional Design成果物（`aidlc-docs/construction/unit3-target-rdbms-setup/functional-design/`）
- requirements.md 5章（非機能要件）のうち、Unit 3固有の技術選定・NFR確定が必要な項目

Unit 1・Unit 2で確定済みの共通基盤（Spring Security、構造化ログ、bucket4jレート制限、
Gradle依存関係ロック、BreachedPasswordChecker等）はそのまま踏襲し、本Planでは再度問わない。
同時利用者数・応答性能目標・可用性目標等、requirements.md 5章で確定済みの項目も再度問わない。

## 実行計画

- [ ] Step 1: Functional Design分析（完了）
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: NFR Requirements成果物生成
  - [ ] `nfr-requirements.md`
  - [ ] `tech-stack-decisions.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

---

## 質問

### Question 1: 接続パスワードの暗号化アルゴリズム

business-rules.md BR-3「可逆暗号化」の具体的な選定。`SecurityInfrastructureComponent#
encryptConnectionSecret`/`#decryptConnectionSecret`の実装方式。

A) （推奨）AES-256-GCM（認証付き暗号）。暗号鍵は環境変数由来（Unit 2のJWT署名鍵と同様の
管理方式）とする

B) AES-256-CBC（HMACによる改ざん検知を別途付与）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 2: 接続確認（テスト接続）のタイムアウト

Functional Design Question 6で確定した登録時の疎通確認について、タイムアウト値を確定する。

A) （推奨）5秒（JDBC接続タイムアウト）。管理者の登録操作を長時間ブロックしない範囲で、
一時的なネットワーク遅延を許容する

B) 10秒（やや長めに許容し、タイムアウトによる誤判定を減らす）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 3: コネクションプール（HikariCP）のサイズ設定

A) （推奨）接続あたり最大プールサイズ5、最小アイドル1とする。同時利用者数約10名規模
かつ複数の対象RDBMS接続が並行運用される前提のため、接続ごとに大きなプールを持たせない

B) 最大プールサイズ10（対象RDBMSごとの負荷に余裕を持たせる）

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 4: スキーマ取込のタイムアウト・大規模スキーマへの対応

A) （推奨）特別なタイムアウト延長・バッチ分割は行わない（HTTPリクエストの通常のタイムアウト
範囲内で完了する想定）。同時利用者数約10名規模の社内ツールであり、極端に大規模な
スキーマ（数千テーブル等）は対象外と割り切る

B) スキーマ取込を非同期処理とし、進捗確認・完了通知の仕組みを別途設ける

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 5: JDBCドライバのバージョン管理・脆弱性スキャン

A) （推奨）Unit 1で確立済みのGradle `dependencyLocking`・GitHub Dependabotの対象に
新規JDBCドライバ（mysql-connector-j、postgresql、mariadb-java-client）も含める
（追加の仕組みは不要、既存の仕組みがそのまま適用される）

B) JDBCドライバは特別に手動でのバージョン固定・追跡を行う

C) Other (please describe after [Answer]: tag below)

[Answer]:

### Question 6: 接続エラーメッセージの分類

疎通確認失敗時（Question 2 for Functional Design = 登録時接続確認）、管理者にどこまで
詳細なエラー内容を返すか。

A) （推奨）「接続できませんでした」という一般的なメッセージに加え、原因を大まかに分類した
コード（ホスト到達不可／認証エラー／タイムアウト／その他）のみを返す。SQLエラーコードや
ドライバの内部例外メッセージ（内部パス等を含みうる）はそのまま返さない（SECURITY-09）

B) JDBCドライバの例外メッセージをそのまま管理者に表示する（デバッグしやすいが、内部詳細が
露出するリスクがある）

C) Other (please describe after [Answer]: tag below)

[Answer]:

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Requirements成果物を生成する。
