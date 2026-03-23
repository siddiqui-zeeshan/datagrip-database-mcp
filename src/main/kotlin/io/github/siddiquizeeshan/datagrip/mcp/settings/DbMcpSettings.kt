package io.github.siddiquizeeshan.datagrip.mcp.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "DbMcpSettings",
    storages = [Storage("dbMcpSettings.xml")],
)
class DbMcpSettings : PersistentStateComponent<DbMcpSettings> {

    /** Datasource names for which writes are enabled. */
    var writeEnabledDatasources: MutableSet<String> = mutableSetOf()

    /** Per-datasource query timeout overrides (seconds). Key = datasource name. */
    var timeoutOverrides: MutableMap<String, Int> = mutableMapOf()

    /** Per-datasource row limit overrides. Key = datasource name. */
    var rowLimitOverrides: MutableMap<String, Int> = mutableMapOf()

    fun isWriteEnabled(datasourceName: String): Boolean =
        datasourceName in writeEnabledDatasources

    fun getTimeout(datasourceName: String): Int? =
        timeoutOverrides[datasourceName]

    fun getRowLimit(datasourceName: String): Int? =
        rowLimitOverrides[datasourceName]

    override fun getState(): DbMcpSettings = this

    override fun loadState(state: DbMcpSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): DbMcpSettings =
            ApplicationManager.getApplication().getService(DbMcpSettings::class.java)
    }
}
