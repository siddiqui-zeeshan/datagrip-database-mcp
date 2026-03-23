package io.github.siddiquizeeshan.datagrip.mcp.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseToolsetTest {

    private val toolset = DatabaseToolset()

    @Test
    fun `postgres uses EXPLAIN with ANALYZE false`() {
        assertEquals(
            "EXPLAIN (ANALYZE false, FORMAT TEXT) SELECT 1",
            toolset.buildExplainSql("postgres", "SELECT 1"),
        )
    }

    @Test
    fun `mysql uses plain EXPLAIN`() {
        assertEquals(
            "EXPLAIN SELECT 1",
            toolset.buildExplainSql("mysql", "SELECT 1"),
        )
    }

    @Test
    fun `mariadb uses plain EXPLAIN`() {
        assertEquals(
            "EXPLAIN SELECT 1",
            toolset.buildExplainSql("mariadb", "SELECT 1"),
        )
    }

    @Test
    fun `oracle uses EXPLAIN PLAN FOR`() {
        assertEquals(
            "EXPLAIN PLAN FOR SELECT 1",
            toolset.buildExplainSql("oracle", "SELECT 1"),
        )
    }

    @Test
    fun `sqlserver uses SET SHOWPLAN_TEXT`() {
        assertEquals(
            "SET SHOWPLAN_TEXT ON; SELECT 1; SET SHOWPLAN_TEXT OFF",
            toolset.buildExplainSql("sqlserver", "SELECT 1"),
        )
    }

    @Test
    fun `mssql uses SET SHOWPLAN_TEXT`() {
        assertEquals(
            "SET SHOWPLAN_TEXT ON; SELECT 1; SET SHOWPLAN_TEXT OFF",
            toolset.buildExplainSql("mssql", "SELECT 1"),
        )
    }

    @Test
    fun `sqlite uses EXPLAIN QUERY PLAN`() {
        assertEquals(
            "EXPLAIN QUERY PLAN SELECT 1",
            toolset.buildExplainSql("sqlite", "SELECT 1"),
        )
    }

    @Test
    fun `unknown driver uses plain EXPLAIN`() {
        assertEquals(
            "EXPLAIN SELECT 1",
            toolset.buildExplainSql("cockroachdb", "SELECT 1"),
        )
    }

    @Test
    fun `already prefixed with EXPLAIN is returned as-is`() {
        assertEquals(
            "EXPLAIN ANALYZE SELECT 1",
            toolset.buildExplainSql("postgres", "EXPLAIN ANALYZE SELECT 1"),
        )
    }

    @Test
    fun `leading whitespace before EXPLAIN is handled`() {
        assertEquals(
            "  EXPLAIN SELECT 1",
            toolset.buildExplainSql("postgres", "  EXPLAIN SELECT 1"),
        )
    }
}
