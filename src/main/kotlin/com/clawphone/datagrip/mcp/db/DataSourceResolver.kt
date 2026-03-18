package com.clawphone.datagrip.mcp.db

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
        val localDs = ds.delegateDataSource as? LocalDataSource ?: return false
        return DatabaseConnectionManager.getInstance()
            .activeConnections
            .any { it.connectionPoint == localDs }
    }
}
