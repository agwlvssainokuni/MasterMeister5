# NFR Design Plan — Unit 3: 対象RDBMSセットアップ

Unit 3のNFR Requirements（AES-256-GCM、接続確認タイムアウト5秒、HikariCP最大プール
サイズ5、JDBCドライバ3種追加、接続エラー分類コード）を、具体的な設計パターン・論理
コンポーネントに落とし込む。

## 実行チェックリスト

- [ ] Step 1: nfr-requirements.md・tech-stack-decisions.mdを分析する
- [ ] Step 2-4: 質問の作成・提示（本ファイル）
- [ ] Step 5: 回答収集・曖昧性分析
- [ ] Step 6: NFR Design成果物生成
  - [ ] `nfr-design-patterns.md`
  - [ ] `logical-components.md`
- [ ] Step 7-9: 完了報告・承認待ち・記録

## カテゴリ評価（MANDATORY: 全カテゴリを評価）

- **Resilience Patterns（耐障害性パターン）**: N/A。resiliency-baseline拡張は適用しない方針、
  NFR Requirements Question 4でスキーマ取込の非同期化・リトライも不要と確定済み
- **Scalability Patterns（スケーリングパターン）**: N/A。Unit 1〜2と同じ根拠（単一
  インスタンス・同時利用者数約10名規模）
- **Performance Patterns（性能最適化パターン）**: 一部該当。HikariCPの詳細設定
  （接続タイムアウト・リーク検知等）をQuestion 5で具体化する
- **Security Patterns（セキュリティ実装パターン）**: 該当。AES-256-GCMの実装パターン
  （IV/nonceの扱い）とJDBC接続URL構築時の安全性をQuestion 1〜2で具体化する
- **Logical Components（論理コンポーネント）**: 該当。Question 3〜6で、暗号化コンポーネントの
  配置・ConnectionSchemaComponentの実装配置・スキーマ全置換のトランザクション境界を
  具体化する

---

## 質問

### Question 1: AES-256-GCMのIV（nonce）の扱い

A) （推奨）暗号化のたびにランダムな96bit IVを生成し、暗号文の先頭に連結して1つの
バイト列として保存する（復号時は先頭からIVを取り出す）。Unit 2の
`SecureTokenGenerator`と同様、`SecureRandom`を使用する

B) IVを暗号文と別のカラムに分けて保存する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2: JDBC接続URL構築時の安全性

ホスト名・ポート・データベース名はSQLパラメータのようにバインドできず、JDBC URL文字列
に直接組み込む必要がある。

A) （推奨）ホスト名・データベース名に許可文字（英数字・ハイフン・アンダースコア・ドット
等、JDBC識別子として妥当な文字）のみを許容する入力検証を行い、範囲外の文字を含む場合は
登録時にエラーとする。ポートは数値型のため別途検証は不要

B) 入力検証は行わず、JDBCドライバ側のURL解析に委ねる

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3: 接続パスワード暗号化コンポーネントの配置

A) （推奨）`cherry.mastermeister5.platform.security`パッケージに`ConnectionSecretCipher`
として配置する（Unit 1・2で確立済みの`platform.security`パッケージにSecurityInfrastructure
Component関連クラスを集約する方針を継続）

B) 新規に`cherry.mastermeister5.connectionschema.security`サブパッケージを設ける

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4: ConnectionSchemaComponentの実装配置パッケージ

A) （推奨）`cherry.mastermeister5.connectionschema`をトップレベルパッケージとし、
`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを持つ
（Unit 2の`useraccount`パッケージ構成を踏襲。Connection/Schema/Table/Column/
ForeignKeyConstraintの5エンティティ・複数画面を抱える規模のため）

B) Unit 1の`platform.theme`と同様、レイヤー分割せず`cherry.mastermeister5.connectionschema`
パッケージ直下にすべてのクラスをフラットに配置する

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5: HikariCPの詳細設定

A) （推奨）`connectionTimeout`は5秒（NFR Requirements Question 2の接続確認タイムアウトと
統一）、`maxLifetime`はデフォルト（30分）のまま、リーク検知（`leakDetectionThreshold`）は
本番相当では10秒に設定する（開発時のデバッグ・接続リーク検知に有用なため）

B) HikariCPのデフォルト設定のまま、プールサイズ以外は変更しない

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6: スキーマ全置換のトランザクション境界

複数スキーマを一括取込する場合（Question 2 for Functional Design = schemaNameHint未指定時）
のトランザクション範囲。

A) （推奨）スキーマ単位でトランザクションを区切る（1スキーマの置換が失敗しても、
他のスキーマの取込結果は確定させる。部分成功を許容し、失敗したスキーマのみエラーとして
報告する）

B) 接続全体（全スキーマ）を1トランザクションとする（一部でも失敗したら全体をロール
バックする、all-or-nothing）

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 次のステップ

全問に回答後、Step 5（回答収集・曖昧性分析）へ進み、NFR Design成果物を生成する。
