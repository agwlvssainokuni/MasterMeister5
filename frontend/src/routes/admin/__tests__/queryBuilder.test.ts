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

import { describe, expect, it } from "vitest";
import { buildSql, parseSqlToBuilderState, EMPTY_QUERY_BUILDER_STATE } from "../queryBuilder";

describe("buildSql", () => {
  it("builds a minimal SELECT when only select/from are set", () => {
    const sql = buildSql({ ...EMPTY_QUERY_BUILDER_STATE, select: "id, name", from: "t1" });

    expect(sql).toBe("SELECT id, name\nFROM t1");
  });

  it("includes every non-empty clause in the correct order", () => {
    const sql = buildSql({
      select: "id",
      from: "t1",
      join: "JOIN t2 ON t1.id = t2.t1_id",
      where: "id = :id",
      groupBy: "id",
      having: "COUNT(*) > 1",
      orderBy: "id DESC",
      limitOffset: "LIMIT 10 OFFSET 0",
    });

    expect(sql).toBe(
      "SELECT id\n" +
        "FROM t1\n" +
        "JOIN t2 ON t1.id = t2.t1_id\n" +
        "WHERE id = :id\n" +
        "GROUP BY id\n" +
        "HAVING COUNT(*) > 1\n" +
        "ORDER BY id DESC\n" +
        "LIMIT 10 OFFSET 0",
    );
  });

  it("defaults an empty select to *", () => {
    const sql = buildSql({ ...EMPTY_QUERY_BUILDER_STATE, select: "", from: "t1" });

    expect(sql).toBe("SELECT *\nFROM t1");
  });
});

describe("parseSqlToBuilderState", () => {
  it("splits a simple SELECT statement into its clauses", () => {
    const state = parseSqlToBuilderState("SELECT id, name FROM t1 WHERE id = :id ORDER BY id DESC");

    expect(state.select).toBe("id, name");
    expect(state.from).toBe("t1");
    expect(state.where).toBe("id = :id");
    expect(state.orderBy).toBe("id DESC");
  });

  it("round-trips a statement produced by buildSql", () => {
    const original = { ...EMPTY_QUERY_BUILDER_STATE, select: "id", from: "t1", where: "id = 1" };
    const sql = buildSql(original);

    const reparsed = parseSqlToBuilderState(sql);

    expect(reparsed.select).toBe("id");
    expect(reparsed.from).toBe("t1");
    expect(reparsed.where).toBe("id = 1");
  });

  it("falls back to putting the whole text in select when the statement is not a simple SELECT", () => {
    const state = parseSqlToBuilderState("not a select statement");

    expect(state.select).toBe("not a select statement");
    expect(state.from).toBe("");
  });
});
