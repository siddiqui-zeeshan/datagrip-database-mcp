package io.github.siddiquizeeshan.datagrip.mcp.db

import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasForeignKey
import com.intellij.database.model.DasIndex
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbDataSource
import com.intellij.database.util.DasUtil
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SchemaIntrospector {

    fun getSchema(dataSource: DbDataSource, schemaFilter: String?): String {
        val tables = DasUtil.getTables(dataSource)
            .filter { table ->
                schemaFilter == null || DasUtil.getSchema(table).equals(schemaFilter, ignoreCase = true)
            }
            .toList()

        val result = buildJsonArray {
            for (table in tables) {
                add(tableToJson(table, detailed = false))
            }
        }
        return result.toString()
    }

    fun getTableInfo(dataSource: DbDataSource, tableName: String, schemaFilter: String?): String? {
        val table = DasUtil.getTables(dataSource)
            .filter { t ->
                t.name.equals(tableName, ignoreCase = true) &&
                    (schemaFilter == null || DasUtil.getSchema(t).equals(schemaFilter, ignoreCase = true))
            }
            .first() ?: return null

        return tableToJson(table, detailed = true).toString()
    }

    fun searchSchema(dataSource: DbDataSource, pattern: String): String {
        val lowerPattern = pattern.lowercase()
        val allTables = DasUtil.getTables(dataSource).toList()

        val matchingTables = allTables.filter { it.name.lowercase().contains(lowerPattern) }

        return buildJsonObject {
            put("tables", buildJsonArray {
                for (t in matchingTables) {
                    add(buildJsonObject {
                        put("name", t.name)
                        put("schema", DasUtil.getSchema(t) ?: "")
                    })
                }
            })
            put("columns", buildJsonArray {
                for (table in allTables) {
                    for (col in DasUtil.getColumns(table)) {
                        if (col.name.lowercase().contains(lowerPattern)) {
                            add(buildJsonObject {
                                put("table", table.name)
                                put("schema", DasUtil.getSchema(table) ?: "")
                                put("column", col.name)
                                put("type", col.dasType.toDataType().typeName)
                            })
                        }
                    }
                }
            })
        }.toString()
    }

    private fun tableToJson(table: DasTable, detailed: Boolean) = buildJsonObject {
        put("name", table.name)
        put("schema", DasUtil.getSchema(table) ?: "")
        put("kind", table.kind.toString())

        put("columns", buildJsonArray {
            for (col in DasUtil.getColumns(table)) {
                add(buildJsonObject {
                    put("name", col.name)
                    put("type", col.dasType.toDataType().typeName)
                    put("nullable", !col.isNotNull)
                    put("isPrimaryKey", DasUtil.isPrimary(col))
                })
            }
        })

        if (detailed) {
            put("indexes", buildJsonArray {
                for (idx in DasUtil.getIndices(table)) {
                    add(buildJsonObject {
                        put("name", idx.name)
                        put("unique", idx.isUnique)
                        put("columns", buildJsonArray {
                            for (colName in idx.columnsRef.names()) {
                                add(JsonPrimitive(colName))
                            }
                        })
                    })
                }
            })

            put("foreignKeys", buildJsonArray {
                for (fk in DasUtil.getForeignKeys(table)) {
                    add(buildJsonObject {
                        put("name", fk.name)
                        put("targetTable", fk.refTableName)
                        put("sourceColumns", buildJsonArray {
                            for (colName in fk.columnsRef.names()) {
                                add(JsonPrimitive(colName))
                            }
                        })
                        put("targetColumns", buildJsonArray {
                            for (colName in fk.refColumns.names()) {
                                add(JsonPrimitive(colName))
                            }
                        })
                    })
                }
            })
        }
    }
}
