# Unit of Work Story Map (MasterMeister5)

stories.mdの全32ストーリーがいずれかのUnitに割り当てられていることを確認する。

| Unit | ストーリー | 件数 |
|---|---|---|
| Unit 1: デザインシステム基盤 | （なし。requirements.md §3由来の技術基盤） | 0 |
| Unit 2: ユーザ管理 | US-1.0, US-1.1, US-1.2, US-1.3, US-1.4, US-1.5, US-1.6, US-1.7, US-1.8, US-1.9, US-1.10 | 11 |
| Unit 3: 対象RDBMSセットアップ | US-2.1, US-2.2, US-2.3 | 3 |
| Unit 4: アクセス制御 | US-2.4, US-2.5, US-2.6, US-2.7 | 4 |
| Unit 5: データ表示 | US-3.1, US-3.2, US-3.3, US-3.4, US-3.5, US-3.6, US-3.7 | 7 |
| Unit 6: その他機能 | US-4.1, US-4.2, US-4.3, US-4.4, US-4.5, US-4.6, US-5.1 | 7 |
| **合計** | | **32** |

stories.md記載の総ストーリー数（32）と一致しており、未割り当てのストーリーは存在しない。

## 検証結果（Step F）

- [x] 全32ストーリーがいずれか1つのUnitに割り当てられている（重複割り当てなし）
- [x] requirements.md 4.1〜4.5の全要件がいずれかのUnitでカバーされている
      （unit-of-work.mdの各Unitの「スコープ」欄参照）
- [x] Unit間の依存関係に循環依存がない（unit-of-work-dependency.md参照）
- [x] Application Designの10コンポーネントすべてがいずれかのUnitに割り当てられている
      （PlatformInfrastructure→Unit1、UserAccount/SecurityInfrastructure/AuditLog/
      Notification→Unit2、ConnectionSchema→Unit3、AccessControl/PermissionCache→Unit4、
      MasterMaintenance→Unit5、Query→Unit6。AuditLogComponentはUnit2で記録機構を構築し、
      Unit6で閲覧APIを追加する形でUnit2・Unit6の双方に関与する）
