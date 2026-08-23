/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useCallback, useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, FormField, Modal, Select, Table, TextInput, useToast, type SortState, type TableColumn } from "make-you-chic-ui";
import { listConnections, type ConnectionSummaryDto } from "../../api/connections";
import {
  applyChanges,
  listRecords,
  listTables,
  type ColumnDefDto,
  type RecordChangeDto,
  type RecordPageDto,
  type TableSummaryDto,
} from "../../api/masterData";
import { ApiError } from "../../api/auth";

type Row = Record<string, unknown>;

function primaryKeyColumns(columns: ColumnDefDto[]): string[] {
  return columns.filter((c) => c.primaryKey).map((c) => c.columnName);
}

function rowIdOf(row: Row, pkColumns: string[]): string {
  return JSON.stringify(pkColumns.map((c) => row[c]));
}

function primaryKeyValuesOf(row: Row, pkColumns: string[]): Record<string, unknown> {
  const values: Record<string, unknown> = {};
  for (const c of pkColumns) {
    values[c] = row[c];
  }
  return values;
}

/** frontend-components.md MasterDataScreen (US-3.1〜US-3.6). */
export function MasterDataScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const toast = useToast();

  const [connections, setConnections] = useState<ConnectionSummaryDto[]>([]);
  const [selectedConnectionId, setSelectedConnectionId] = useState("");
  const [tables, setTables] = useState<TableSummaryDto[]>([]);
  const [selectedTableKey, setSelectedTableKey] = useState("");

  const [rawWhereClause, setRawWhereClause] = useState("");
  const [sortState, setSortState] = useState<SortState | null>(null);
  const [page, setPage] = useState(0);
  const pageSize = 50;

  const [recordPage, setRecordPage] = useState<RecordPageDto | null>(null);
  const [editsByRowId, setEditsByRowId] = useState<Map<string, Row>>(new Map());
  const [deletedRowIds, setDeletedRowIds] = useState<Set<string>>(new Set());
  const [newRows, setNewRows] = useState<Row[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [createDraft, setCreateDraft] = useState<Row>({});

  useEffect(() => {
    listConnections().then((all) => setConnections(all.filter((c) => c.status === "ACTIVE")));
  }, []);

  useEffect(() => {
    if (!selectedConnectionId) {
      setTables([]);
      return;
    }
    listTables(Number(selectedConnectionId)).then(setTables);
  }, [selectedConnectionId]);

  const selectedTable = useMemo(
    () => tables.find((t) => `${t.schemaName}.${t.tableName}` === selectedTableKey) ?? null,
    [tables, selectedTableKey],
  );

  const pkColumns = useMemo(() => (recordPage ? primaryKeyColumns(recordPage.columns) : []), [recordPage]);

  const reloadRecords = useCallback(() => {
    if (!selectedConnectionId || !selectedTable) {
      setRecordPage(null);
      return;
    }
    listRecords(Number(selectedConnectionId), selectedTable.schemaName, selectedTable.tableName, {
      rawWhereClause: rawWhereClause || undefined,
      sortColumn: sortState?.direction ? sortState.key : undefined,
      sortDirection: sortState?.direction === "desc" ? "DESC" : sortState?.direction === "asc" ? "ASC" : undefined,
      page,
      pageSize,
    }).then(setRecordPage);
  }, [selectedConnectionId, selectedTable, rawWhereClause, sortState, page]);

  useEffect(() => {
    reloadRecords();
  }, [reloadRecords]);

  function handleCellEdit(rowId: string, columnKey: string, value: unknown) {
    setEditsByRowId((prev) => {
      const next = new Map(prev);
      next.set(rowId, { ...(next.get(rowId) ?? {}), [columnKey]: value });
      return next;
    });
  }

  function handleToggleDelete(rowId: string) {
    setDeletedRowIds((prev) => {
      const next = new Set(prev);
      if (next.has(rowId)) {
        next.delete(rowId);
      } else {
        next.add(rowId);
      }
      return next;
    });
  }

  function handleAddNewRow() {
    setNewRows((prev) => [...prev, createDraft]);
    setCreateDraft({});
    setCreateOpen(false);
  }

  const hasPendingChanges = editsByRowId.size > 0 || deletedRowIds.size > 0 || newRows.length > 0;

  async function handleApply() {
    if (!selectedConnectionId || !selectedTable || !recordPage) {
      return;
    }
    const changes: RecordChangeDto[] = [];
    for (const [rowId, edits] of editsByRowId.entries()) {
      if (deletedRowIds.has(rowId)) {
        continue;
      }
      const row = recordPage.rows.find((r) => rowIdOf(r, pkColumns) === rowId);
      if (!row) {
        continue;
      }
      changes.push({ operation: "UPDATE", primaryKeyValues: primaryKeyValuesOf(row, pkColumns), columnValues: edits });
    }
    for (const rowId of deletedRowIds) {
      const row = recordPage.rows.find((r) => rowIdOf(r, pkColumns) === rowId);
      if (!row) {
        continue;
      }
      changes.push({ operation: "DELETE", primaryKeyValues: primaryKeyValuesOf(row, pkColumns), columnValues: {} });
    }
    for (const newRow of newRows) {
      changes.push({ operation: "CREATE", primaryKeyValues: {}, columnValues: newRow });
    }
    if (changes.length === 0) {
      return;
    }
    try {
      await applyChanges(Number(selectedConnectionId), selectedTable.schemaName, selectedTable.tableName, changes);
      setEditsByRowId(new Map());
      setDeletedRowIds(new Set());
      setNewRows([]);
      reloadRecords();
    } catch (err) {
      toast.show({
        message: err instanceof ApiError ? err.message : t("admin.masterData.apply.error"),
        variant: "danger",
      });
    }
  }

  const editableColumnKeys = useMemo(
    () => (recordPage ? recordPage.columns.filter((c) => !c.readOnly).map((c) => c.columnName) : []),
    [recordPage],
  );

  const tableColumns: TableColumn<Row>[] = useMemo(() => {
    if (!recordPage) {
      return [];
    }
    const columns: TableColumn<Row>[] = recordPage.columns.map((c) => ({
      key: c.columnName,
      header: c.displayLabel,
      sortable: true,
      editable: editableColumnKeys.includes(c.columnName),
    }));
    if (selectedTable?.canDelete) {
      columns.push({
        key: "__actions__",
        header: t("admin.masterData.columns.actions"),
        render: (row) => {
          const rowId = rowIdOf(row, pkColumns);
          return (
            <Button
              variant="danger"
              size="sm"
              onClick={() => handleToggleDelete(rowId)}
              data-testid={`master-data-row-${rowId}-delete-button`}
            >
              {deletedRowIds.has(rowId) ? t("admin.masterData.undoDeleteButton") : t("admin.masterData.deleteButton")}
            </Button>
          );
        },
      });
    }
    return columns;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recordPage, editableColumnKeys, deletedRowIds, pkColumns, selectedTable]);

  const canCreate = selectedTable?.canCreate ?? false;

  return (
    <div>
      <h1>{t("admin.masterData.title")}</h1>

      <Select
        options={[
          { label: t("common.selectPlaceholder"), value: "" },
          ...connections.map((c) => ({ label: c.name, value: String(c.id) })),
        ]}
        value={selectedConnectionId}
        onChange={(value) => {
          setSelectedConnectionId(value);
          setSelectedTableKey("");
        }}
        data-testid="master-data-connection-select"
      />
      <Select
        options={[
          { label: t("common.selectPlaceholder"), value: "" },
          ...tables.map((table) => ({
            label: `${table.schemaName}.${table.tableName}`,
            value: `${table.schemaName}.${table.tableName}`,
          })),
        ]}
        value={selectedTableKey}
        onChange={setSelectedTableKey}
        data-testid="master-data-table-select"
      />

      {selectedTable && (
        <FormField label={t("admin.masterData.rawWhereClause")}>
          <TextInput
            value={rawWhereClause}
            onChange={(value) => {
              setRawWhereClause(value);
              setPage(0);
            }}
            data-testid="master-data-raw-where-input"
          />
        </FormField>
      )}

      {recordPage && (
        <>
          <Table
            columns={tableColumns}
            data={recordPage.rows}
            totalCount={recordPage.totalCount}
            getRowId={(row) => rowIdOf(row, pkColumns)}
            sortState={sortState}
            onSortChange={setSortState}
            page={page}
            pageSize={pageSize}
            onPageChange={setPage}
            onCellEdit={handleCellEdit}
            aria-label={t("admin.masterData.title")}
          />
          <Button
            onClick={handleApply}
            disabled={!hasPendingChanges}
            data-testid="master-data-apply-button"
          >
            {t("admin.masterData.applyButton")}
          </Button>
          {canCreate && (
            <Button onClick={() => setCreateOpen(true)} data-testid="master-data-create-button">
              {t("admin.masterData.createButton")}
            </Button>
          )}
        </>
      )}

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title={t("admin.masterData.create.title")}>
        {recordPage && (
          <div data-testid="master-data-create-form">
            {recordPage.columns
              .filter((c) => editableColumnKeys.includes(c.columnName))
              .map((c) => (
                <FormField key={c.columnName} label={c.displayLabel}>
                  <TextInput
                    value={String(createDraft[c.columnName] ?? "")}
                    onChange={(value) => setCreateDraft((prev) => ({ ...prev, [c.columnName]: value }))}
                    data-testid={`master-data-create-form-${c.columnName}-input`}
                  />
                </FormField>
              ))}
            <Button onClick={handleAddNewRow} data-testid="master-data-create-form-add-button">
              {t("admin.masterData.create.add")}
            </Button>
          </div>
        )}
      </Modal>
    </div>
  );
}
