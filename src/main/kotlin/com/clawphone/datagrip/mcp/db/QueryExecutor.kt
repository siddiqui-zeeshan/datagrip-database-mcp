package com.clawphone.datagrip.mcp.db

import com.intellij.database.psi.DbDataSource
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object QueryExecutor {

    private const val DEFAULT_ROW_LIMIT = 1000
    private const val DEFAULT_TIMEOUT_SECONDS = 30
    private const val MAX_RESPONSE_BYTES = 100_000

    suspend fun execute(
        project: Project,
        dataSource: DbDataSource,
        sql: String,
        rowLimit: Int? = null,
        timeoutSeconds: Int? = null,
    ): String {
        val limit = rowLimit ?: DEFAULT_ROW_LIMIT
        val timeout = timeoutSeconds ?: DEFAULT_TIMEOUT_SECONDS

        val dbConnection = DataSourceResolver.ensureConnected(project, dataSource)
        val remoteConnection = dbConnection.remoteConnection
        val stmt = remoteConnection.createStatement()
        try {
            stmt.setQueryTimeout(timeout)
            stmt.setMaxRows(limit + 1)

            val hasResultSet = stmt.execute(sql)
            if (!hasResultSet) {
                val updateCount = stmt.getUpdateCount()
                return buildJsonObject {
                    put("updateCount", updateCount)
                    put("message", "$updateCount row(s) affected")
                }.toString()
            }

            val rs = stmt.getResultSet()
            try {
                val meta = rs.getMetaData()
                val columnCount = meta.getColumnCount()
                val columns = (1..columnCount).map { meta.getColumnLabel(it) }
                val rows = mutableListOf<kotlinx.serialization.json.JsonArray>()
                var totalBytes = 0
                var truncatedBySize = false

                while (rs.next() && rows.size <= limit) {
                    val row = buildJsonArray {
                        for (i in 1..columnCount) {
                            val value = rs.getString(i)
                            if (value == null) {
                                add(JsonNull)
                            } else {
                                add(JsonPrimitive(value))
                            }
                        }
                    }
                    totalBytes += row.toString().length
                    if (totalBytes > MAX_RESPONSE_BYTES) {
                        truncatedBySize = true
                        break
                    }
                    rows.add(row)
                }

                val truncated = rows.size > limit || truncatedBySize
                val finalRows = if (rows.size > limit) rows.take(limit) else rows

                return buildJsonObject {
                    put("columns", buildJsonArray { columns.forEach { add(JsonPrimitive(it)) } })
                    put("rows", buildJsonArray { finalRows.forEach { add(it) } })
                    put("rowCount", finalRows.size)
                    put("truncated", truncated)
                }.toString()
            } finally {
                rs.close()
            }
        } finally {
            stmt.close()
        }
    }
}
