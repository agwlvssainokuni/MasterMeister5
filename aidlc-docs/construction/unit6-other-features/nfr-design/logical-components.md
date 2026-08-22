# Logical Components — Unit 6: その他機能

nfr-design-patterns.mdで定めたパターンを実現するために、Unit 6で新設する論理コンポーネント
を定義する。詳細な実装（クラス名・パッケージ配置等）はCode Generationで確定する。

`QueryComponent`関連は`cherry.mastermeister5.query`パッケージ配下に
`entity`/`repository`/`service`/`controller`のレイヤーサブパッケージを持つ
（Unit 2〜5のパッケージ構成方針を踏襲）。

## コントローラ（REST API）

| 論理コンポーネント | 役割 |
|---|---|
| QueryController | クエリ保存・論理非表示・パラメータ検出・クエリ実行・実行履歴閲覧（`/api/query/**`）。認証済みユーザ全員がアクセス可能（ロール制限なし） |
| AuditLogController（`audit.controller`パッケージ、新規） | 監査ログ閲覧（`/api/admin/audit-events/**`）。ADMINロール限定 |

## QueryComponent実装

| 論理コンポーネント | 役割 |
|---|---|
| QueryService | QueryComponentの実装。`saveQuery`/`retireQuery`（作成者検証込み）/`detectParameters`（`NamedParameterUtils`利用）/`executeQuery`（読み取り専用検証・スキーマ適用・実行・履歴記録・監査ログ記録）/`listExecutionHistory` |
| SavedQueryJpaRepository / QueryExecutionHistoryJpaRepository | 各エンティティのJPAリポジトリ（Spring Data JPA直接利用、Unit 2〜5と同方針） |
| QueryException | 業務例外（`ConnectionException`/`AccessControlException`/`MasterMaintenanceException`と同型パターン。読み取り専用違反・スキーマ不許可・編集権限違反等をファクトリメソッドで表現） |
| ReadOnlySqlValidator（内部ヘルパー） | 手入力SQLのブロックリスト検証（`;`/`--`/`/*`検出、Unit 5の同種ヘルパーと同じ「共通化のための抽象化を避ける」方針で`query.service`パッケージに個別実装する） |

## 既存Unitへの変更

### Unit 2（`audit`パッケージ）

| 変更対象 | 内容 |
|---|---|
| `AuditLogService` | `listEvents(AuditEventFilterCriteria, Pageable)`オーバーロードを追加（既存の`listEvents(Pageable)`は変更しない） |
| `AuditLogServiceImpl` | 上記メソッドを実装。`eventType`/`actorUserId`/`occurredAt`期間でフィルタする |
| （新規）`AuditEventFilterCriteria` | フィルタ条件の値オブジェクト |
| （新規）`audit.controller.AuditLogController` | 監査ログ閲覧REST API（ADMINロール限定） |

### Unit 5（`mastermaintenance`パッケージ）

| 変更対象 | 内容 |
|---|---|
| `MasterMaintenanceServiceImpl#listRecords` | 返却件数が`MM5_BULK_ACCESS_THRESHOLD`（デフォルト100）以上の場合に「大量データ取得」監査イベントを追加記録する |

## 依存関係

```
QueryController → QueryService（saveQuery/retireQuery/detectParameters/executeQuery/
                   listExecutionHistory）
AuditLogController（audit.controller） → AuditLogService（listEvents拡張オーバーロード）

QueryService → SavedQueryJpaRepository/QueryExecutionHistoryJpaRepository
QueryService → ReadOnlySqlValidator（内部ヘルパー、executeQuery）
QueryService → AuditLogService（Unit 2確立済み、クエリ保存/論理非表示/実行/大量データ
               取得イベント記録）
QueryService → Unit 3のConnectionSchemaService（isSchemaAllowed）、
               ConnectionPoolRegistry（対象RDBMSへのJDBC接続取得）

AuditLogServiceImpl（拡張オーバーロード） → AuditEventJpaRepository（Unit 2確立済み）

MasterMaintenanceServiceImpl（Unit 5） → AuditLogService（大量データ取得イベント記録、
               既存の依存を再利用）
```

`query`パッケージは`mastermaintenance`パッケージの型をimportしない（Unit 3・Unit 5間の
ような循環依存の懸念は生じない。`audit`パッケージへの依存は既存Unit全体で確立済みの
一方向依存であり、本Unitでもそのまま踏襲する）。
