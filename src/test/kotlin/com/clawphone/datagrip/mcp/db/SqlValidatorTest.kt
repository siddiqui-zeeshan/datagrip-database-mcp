package com.clawphone.datagrip.mcp.db

import org.junit.Assert.*
import org.junit.Test

class SqlValidatorTest {

    @Test
    fun `SELECT is read-only`() {
        assertTrue(SqlValidator.isReadOnly("SELECT * FROM users"))
        assertTrue(SqlValidator.isReadOnly("  select id from users"))
        assertTrue(SqlValidator.isReadOnly("SELECT 1"))
    }

    @Test
    fun `WITH CTE followed by SELECT is read-only`() {
        assertTrue(SqlValidator.isReadOnly("WITH cte AS (SELECT 1) SELECT * FROM cte"))
    }

    @Test
    fun `WITH CTE followed by INSERT is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("WITH cte AS (SELECT 1) INSERT INTO users SELECT * FROM cte"))
    }

    @Test
    fun `WITH CTE followed by DELETE is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("WITH cte AS (SELECT 1) DELETE FROM users WHERE id IN (SELECT * FROM cte)"))
    }

    @Test
    fun `EXPLAIN SELECT is read-only`() {
        assertTrue(SqlValidator.isReadOnly("EXPLAIN SELECT * FROM users"))
        assertTrue(SqlValidator.isReadOnly("EXPLAIN ANALYZE SELECT 1"))
    }

    @Test
    fun `EXPLAIN of DML is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("EXPLAIN ANALYZE INSERT INTO users VALUES (1)"))
        assertFalse(SqlValidator.isReadOnly("EXPLAIN DELETE FROM users"))
    }

    @Test
    fun `EXPLAIN with options of SELECT is read-only`() {
        assertTrue(SqlValidator.isReadOnly("EXPLAIN (ANALYZE false, FORMAT TEXT) SELECT 1"))
    }

    @Test
    fun `SHOW and DESCRIBE are read-only`() {
        assertTrue(SqlValidator.isReadOnly("SHOW TABLES"))
        assertTrue(SqlValidator.isReadOnly("DESCRIBE users"))
        assertTrue(SqlValidator.isReadOnly("DESC users"))
    }

    @Test
    fun `INSERT is not read-only`() {
        assertFalse(SqlValidator.isReadOnly("INSERT INTO users VALUES (1)"))
    }

    @Test
    fun `UPDATE is not read-only`() {
        assertFalse(SqlValidator.isReadOnly("UPDATE users SET name = 'x'"))
    }

    @Test
    fun `DELETE is not read-only`() {
        assertFalse(SqlValidator.isReadOnly("DELETE FROM users"))
    }

    @Test
    fun `DROP is not read-only`() {
        assertFalse(SqlValidator.isReadOnly("DROP TABLE users"))
    }

    @Test
    fun `ALTER is not read-only`() {
        assertFalse(SqlValidator.isReadOnly("ALTER TABLE users ADD COLUMN x INT"))
    }

    // --- Multi-statement bypass prevention ---

    @Test
    fun `multi-statement with SELECT then DROP is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("SELECT 1; DROP TABLE users"))
    }

    @Test
    fun `multi-statement with SELECT then INSERT is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("SELECT 1; INSERT INTO users VALUES (1)"))
    }

    @Test
    fun `trailing semicolon on single statement IS read-only`() {
        assertTrue(SqlValidator.isReadOnly("SELECT 1;"))
        assertTrue(SqlValidator.isReadOnly("SELECT * FROM users ;  "))
    }

    // --- Comment bypass prevention ---

    @Test
    fun `line comment before DML is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("-- comment\nINSERT INTO users VALUES (1)"))
    }

    @Test
    fun `block comment before DML is NOT read-only`() {
        assertFalse(SqlValidator.isReadOnly("/* comment */ INSERT INTO users VALUES (1)"))
    }

    // --- Validation ---

    @Test
    fun `validateOrThrow allows read-only when writes disabled`() {
        SqlValidator.validateOrThrow("SELECT 1", writeEnabled = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validateOrThrow rejects write when writes disabled`() {
        SqlValidator.validateOrThrow("INSERT INTO users VALUES (1)", writeEnabled = false)
    }

    @Test
    fun `validateOrThrow allows write when writes enabled`() {
        SqlValidator.validateOrThrow("INSERT INTO users VALUES (1)", writeEnabled = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validateOrThrow rejects multi-statement bypass`() {
        SqlValidator.validateOrThrow("SELECT 1; DROP TABLE users", writeEnabled = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validateOrThrow rejects WITH INSERT bypass`() {
        SqlValidator.validateOrThrow("WITH cte AS (SELECT 1) INSERT INTO t SELECT * FROM cte", writeEnabled = false)
    }

    @Test
    fun `case insensitive matching`() {
        assertTrue(SqlValidator.isReadOnly("select * from users"))
        assertTrue(SqlValidator.isReadOnly("Select * FROM users"))
        assertFalse(SqlValidator.isReadOnly("insert into users values (1)"))
    }

    @Test
    fun `leading whitespace is ignored`() {
        assertTrue(SqlValidator.isReadOnly("   SELECT 1"))
        assertTrue(SqlValidator.isReadOnly("\n\tSELECT 1"))
        assertFalse(SqlValidator.isReadOnly("   INSERT INTO users VALUES (1)"))
    }
}
