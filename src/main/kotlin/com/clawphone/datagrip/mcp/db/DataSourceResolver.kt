package com.clawphone.datagrip.mcp.db

import com.intellij.database.dataSource.DatabaseConnection
import com.intellij.database.dataSource.DatabaseConnectionManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.psi.DbDataSource
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.project.Project

object DataSourceResolver {

    fun resolve(project: Project, name: String): DbDataSource? {
        return DbPsiFacade.getInstance(project).dataSources.find {
            it.name.equals(name, ignoreCase = true)
        }
    }

    fun listAll(project: Project): List<DbDataSource> {
        return DbPsiFacade.getInstance(project).dataSources
    }

    fun isConnected(ds: DbDataSource): Boolean {
        return getActiveConnection(ds) != null
    }

    fun getActiveConnection(ds: DbDataSource): DatabaseConnection? {
        val localDs = ds.delegateDataSource as? LocalDataSource ?: return null
        return DatabaseConnectionManager.getInstance()
            .activeConnections
            .firstOrNull { it.connectionPoint.dataSource == localDs }
    }

    /**
     * Get an existing connection or establish a new one.
     * This allows tools to auto-connect datasources on demand.
     */
    suspend fun ensureConnected(project: Project, ds: DbDataSource): DatabaseConnection {
        // Return existing connection if available
        getActiveConnection(ds)?.let { return it }

        // Auto-connect
        val localDs = ds.delegateDataSource as? LocalDataSource
            ?: throw IllegalStateException("Datasource '${ds.name}' is not a local datasource.")

        val connectionManager = DatabaseConnectionManager.getInstance()
        val guardedRef = connectionManager.build(project, localDs)
            .setAskPassword(false)
            .create()
            ?: throw IllegalStateException(
                "Failed to connect to datasource '${ds.name}'. " +
                    "Check credentials and connection settings in DataGrip."
            )

        return guardedRef.get()
    }
}
