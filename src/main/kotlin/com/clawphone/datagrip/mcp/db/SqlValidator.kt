package com.clawphone.datagrip.mcp.db

object SqlValidator {

    private val READ_ONLY_PREFIXES = listOf(
        "SELECT",
        "SHOW",
        "DESCRIBE",
        "DESC",
    )

    private val DML_KEYWORDS = listOf(
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
        "TRUNCATE", "REPLACE", "MERGE", "UPSERT", "GRANT", "REVOKE",
    )

    fun isReadOnly(sql: String): Boolean {
        val normalized = stripComments(sql).trimStart().uppercase()

        // Reject multi-statement SQL (semicolons outside of string literals)
        if (containsSemicolon(normalized)) return false

        // Simple read-only prefixes
        if (READ_ONLY_PREFIXES.any { normalized.startsWith(it) }) return true

        // WITH (CTE): only allow if the final statement is SELECT
        if (normalized.startsWith("WITH")) {
            return cteEndsWithSelect(normalized)
        }

        // EXPLAIN: allow only on read-only inner statements
        // EXPLAIN ANALYZE actually executes DML on some databases, so block DML
        if (normalized.startsWith("EXPLAIN")) {
            val inner = extractExplainInner(normalized)
            return !containsDml(inner)
        }

        return false
    }

    fun validateOrThrow(sql: String, writeEnabled: Boolean) {
        if (!writeEnabled && !isReadOnly(sql)) {
            throw IllegalArgumentException(
                "Write operations are disabled for this datasource. " +
                    "Only SELECT, EXPLAIN (of SELECT), SHOW, DESCRIBE, and WITH...SELECT statements are allowed. " +
                    "Enable writes in Settings > Tools > Database MCP Tools."
            )
        }
    }

    /** Check if SQL contains a semicolon that could indicate multi-statement. */
    private fun containsSemicolon(sql: String): Boolean {
        // Simple check: reject if there's a semicolon followed by non-whitespace
        val trimmed = sql.trimEnd()
        val semiIdx = trimmed.indexOf(';')
        if (semiIdx < 0) return false
        // Allow trailing semicolon (single statement)
        val afterSemi = trimmed.substring(semiIdx + 1).trimStart()
        return afterSemi.isNotEmpty()
    }

    /** Strip SQL line comments (--) and block comments to prevent comment-based bypasses. */
    private fun stripComments(sql: String): String {
        val sb = StringBuilder()
        var i = 0
        var inString = false
        var stringChar = ' '

        while (i < sql.length) {
            if (inString) {
                sb.append(sql[i])
                if (sql[i] == stringChar) {
                    // Check for escaped quote (doubled)
                    if (i + 1 < sql.length && sql[i + 1] == stringChar) {
                        sb.append(sql[i + 1])
                        i += 2
                        continue
                    }
                    inString = false
                }
                i++
                continue
            }

            when {
                sql[i] == '\'' || sql[i] == '"' -> {
                    inString = true
                    stringChar = sql[i]
                    sb.append(sql[i])
                    i++
                }
                i + 1 < sql.length && sql[i] == '-' && sql[i + 1] == '-' -> {
                    // Skip to end of line
                    while (i < sql.length && sql[i] != '\n') i++
                    sb.append(' ')
                }
                i + 1 < sql.length && sql[i] == '/' && sql[i + 1] == '*' -> {
                    // Skip block comment
                    i += 2
                    while (i + 1 < sql.length && !(sql[i] == '*' && sql[i + 1] == '/')) i++
                    i += 2
                    sb.append(' ')
                }
                else -> {
                    sb.append(sql[i])
                    i++
                }
            }
        }
        return sb.toString()
    }

    /** For WITH...CTE, find the final statement keyword and ensure it's SELECT. */
    private fun cteEndsWithSelect(normalized: String): Boolean {
        // Find the last top-level SELECT/INSERT/UPDATE/DELETE after CTE definitions
        // Simple heuristic: find the last occurrence of these DML keywords at word boundary
        for (keyword in DML_KEYWORDS) {
            if (Regex("\\b$keyword\\b").containsMatchIn(normalized)) return false
        }
        return Regex("\\bSELECT\\b").containsMatchIn(normalized)
    }

    /** Extract the inner SQL from EXPLAIN [ANALYZE] [options] <sql>. */
    private fun extractExplainInner(normalized: String): String {
        // Remove EXPLAIN prefix and common options
        var inner = normalized.removePrefix("EXPLAIN").trimStart()
        // Remove ANALYZE keyword
        if (inner.startsWith("ANALYZE")) inner = inner.removePrefix("ANALYZE").trimStart()
        // Remove parenthesized options: EXPLAIN (ANALYZE false, FORMAT TEXT) SELECT ...
        if (inner.startsWith("(")) {
            val closeIdx = inner.indexOf(')')
            if (closeIdx >= 0) inner = inner.substring(closeIdx + 1).trimStart()
        }
        // Remove QUERY PLAN (SQLite)
        if (inner.startsWith("QUERY")) inner = inner.removePrefix("QUERY").trimStart()
        if (inner.startsWith("PLAN")) inner = inner.removePrefix("PLAN").trimStart()
        // Remove FOR (Oracle: EXPLAIN PLAN FOR)
        if (inner.startsWith("FOR")) inner = inner.removePrefix("FOR").trimStart()
        return inner
    }

    /** Check if SQL contains DML keywords. */
    private fun containsDml(sql: String): Boolean {
        val upper = sql.uppercase()
        return DML_KEYWORDS.any { Regex("\\b$it\\b").containsMatchIn(upper) }
    }
}
