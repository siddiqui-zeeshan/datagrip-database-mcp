package com.clawphone.datagrip.mcp.tools

import com.clawphone.datagrip.mcp.db.DataSourceResolver
import com.clawphone.datagrip.mcp.db.QueryExecutor
import com.clawphone.datagrip.mcp.db.SchemaIntrospector
import com.clawphone.datagrip.mcp.db.SqlValidator
import com.clawphone.datagrip.mcp.settings.DbMcpSettings
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseToolset : McpToolset {

    private fun resolveProject(projectPath: String?): Project {
        val openProjects = ProjectManager.getInstance().openProjects
        if (projectPath != null) {
            openProjects.firstOrNull { it.basePath == projectPath }?.let { return it }
        }
        return openProjects.firstOrNull()
            ?: throw IllegalStateException("No open project found")
    }

    @McpTool(name = "list_datasources")
    @McpDescription(
        "List all configured database datasources. Returns name, driver, " +
            "connection status, and whether writes are enabled for each datasource."
    )
    suspend fun list_datasources(projectPath: String? = null): String {
        val project = resolveProject(projectPath)
        val dataSources = DataSourceResolver.listAll(project)
        val settings = DbMcpSettings.getInstance()

        val result = dataSources.map { ds ->
            val localDs = ds.delegateDataSource as? LocalDataSource
            DatasourceInfo(
                name = ds.name,
                driver = localDs?.databaseDriver?.name ?: "unknown",
                connected = DataSourceResolver.isConnected(ds),
                writeEnabled = settings.isWriteEnabled(ds.name),
            )
        }
        return Json.encodeToString(result)
    }

    @McpTool(name = "get_schema")
    @McpDescription(
        "Get the database schema for a datasource. Returns tables with their columns, " +
            "types, primary keys, and nullability. Optionally filter by schema name."
    )
    suspend fun get_schema(datasource: String, schema: String? = null, projectPath: String? = null): String {
        val project = resolveProject(projectPath)
        val ds = DataSourceResolver.resolve(project, datasource)
            ?: mcpFail("Datasource '$datasource' not found. Use list_datasources to see available datasources.")

        return SchemaIntrospector.getSchema(ds, schema)
    }

    @McpTool(name = "run_query")
    @McpDescription(
        "Execute a SQL query against a datasource. Returns columns, rows, row count, " +
            "and whether results were truncated. By default only read-only queries (SELECT, " +
            "EXPLAIN, SHOW, DESCRIBE, WITH) are allowed unless writes are enabled for the datasource."
    )
    suspend fun run_query(
        datasource: String,
        sql: String,
        rowLimit: Int? = null,
        timeout: Int? = null,
        projectPath: String? = null,
    ): String {
        val project = resolveProject(projectPath)
        val ds = DataSourceResolver.resolve(project, datasource)
            ?: mcpFail("Datasource '$datasource' not found. Use list_datasources to see available datasources.")

        val settings = DbMcpSettings.getInstance()
        SqlValidator.validateOrThrow(sql, settings.isWriteEnabled(ds.name))

        val effectiveTimeout = timeout ?: settings.getTimeout(ds.name)
        val effectiveRowLimit = rowLimit ?: settings.getRowLimit(ds.name)

        return QueryExecutor.execute(project, ds, sql, effectiveRowLimit, effectiveTimeout)
    }

    @McpTool(name = "explain_query")
    @McpDescription(
        "Get the execution plan for a SQL query. Automatically prefixes the query with " +
            "the appropriate EXPLAIN syntax for the database driver."
    )
    suspend fun explain_query(datasource: String, sql: String, projectPath: String? = null): String {
        val project = resolveProject(projectPath)
        val ds = DataSourceResolver.resolve(project, datasource)
            ?: mcpFail("Datasource '$datasource' not found. Use list_datasources to see available datasources.")

        // Validate the inner SQL is read-only (EXPLAIN ANALYZE can execute DML on some DBs)
        val settings = DbMcpSettings.getInstance()
        SqlValidator.validateOrThrow(sql, settings.isWriteEnabled(ds.name))

        val localDs = ds.delegateDataSource as? LocalDataSource
        val driverName = localDs?.databaseDriver?.name?.lowercase() ?: ""
        val explainSql = buildExplainSql(driverName, sql)

        val effectiveTimeout = settings.getTimeout(ds.name)

        return QueryExecutor.execute(project, ds, explainSql, rowLimit = null, timeoutSeconds = effectiveTimeout)
    }

    @McpTool(name = "get_table_info")
    @McpDescription(
        "Get detailed information about a specific table including columns, types, " +
            "primary keys, foreign keys, and indexes."
    )
    suspend fun get_table_info(datasource: String, table: String, schema: String? = null, projectPath: String? = null): String {
        val project = resolveProject(projectPath)
        val ds = DataSourceResolver.resolve(project, datasource)
            ?: mcpFail("Datasource '$datasource' not found. Use list_datasources to see available datasources.")

        return SchemaIntrospector.getTableInfo(ds, table, schema)
            ?: mcpFail("Table '$table' not found in datasource '$datasource'.")
    }

    @McpTool(name = "search_schema")
    @McpDescription(
        "Search for tables and columns matching a pattern in a datasource. " +
            "Returns matching table names and columns with their types."
    )
    suspend fun search_schema(datasource: String, pattern: String, projectPath: String? = null): String {
        val project = resolveProject(projectPath)
        val ds = DataSourceResolver.resolve(project, datasource)
            ?: mcpFail("Datasource '$datasource' not found. Use list_datasources to see available datasources.")

        return SchemaIntrospector.searchSchema(ds, pattern)
    }

    private fun buildExplainSql(driverName: String, sql: String): String {
        if (sql.trimStart().uppercase().startsWith("EXPLAIN")) return sql

        return when {
            "postgres" in driverName -> "EXPLAIN (ANALYZE false, FORMAT TEXT) $sql"
            "mysql" in driverName || "mariadb" in driverName -> "EXPLAIN $sql"
            "oracle" in driverName -> "EXPLAIN PLAN FOR $sql"
            "sqlserver" in driverName || "mssql" in driverName -> "SET SHOWPLAN_TEXT ON; $sql; SET SHOWPLAN_TEXT OFF"
            "sqlite" in driverName -> "EXPLAIN QUERY PLAN $sql"
            else -> "EXPLAIN $sql"
        }
    }
}

@Serializable
data class DatasourceInfo(
    val name: String,
    val driver: String,
    val connected: Boolean,
    val writeEnabled: Boolean,
)
