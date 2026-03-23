package io.github.siddiquizeeshan.datagrip.mcp.settings

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class DbMcpSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var tableModel: DefaultTableModel? = null

    override fun getDisplayName(): String = "Database MCP Tools"

    override fun createComponent(): JComponent {
        val settings = DbMcpSettings.getInstance()

        val datasourceNames = ProjectManager.getInstance().openProjects
            .flatMap { DbPsiFacade.getInstance(it).dataSources }
            .map { it.name }
            .distinct()
            .sorted()

        tableModel = object : DefaultTableModel(
            arrayOf("Datasource", "Write Enabled", "Timeout (s)", "Row Limit"),
            0,
        ) {
            override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
                1 -> java.lang.Boolean::class.java
                else -> String::class.java
            }

            override fun isCellEditable(row: Int, column: Int): Boolean = column != 0
        }.apply {
            for (name in datasourceNames) {
                addRow(arrayOf(
                    name,
                    settings.isWriteEnabled(name),
                    settings.timeoutOverrides[name]?.toString() ?: "",
                    settings.rowLimitOverrides[name]?.toString() ?: "",
                ))
            }
        }

        val table = JBTable(tableModel)
        val scrollPane = JBScrollPane(table)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Per-datasource settings:", scrollPane)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = DbMcpSettings.getInstance()
        val model = tableModel ?: return false

        for (i in 0 until model.rowCount) {
            val name = model.getValueAt(i, 0) as String
            val writeEnabled = model.getValueAt(i, 1) as? Boolean ?: false
            val timeout = (model.getValueAt(i, 2) as? String)?.toIntOrNull()
            val rowLimit = (model.getValueAt(i, 3) as? String)?.toIntOrNull()

            if (writeEnabled != settings.isWriteEnabled(name)) return true
            if (timeout != settings.timeoutOverrides[name]) return true
            if (rowLimit != settings.rowLimitOverrides[name]) return true
        }
        return false
    }

    override fun apply() {
        val settings = DbMcpSettings.getInstance()
        val model = tableModel ?: return

        settings.writeEnabledDatasources.clear()
        settings.timeoutOverrides.clear()
        settings.rowLimitOverrides.clear()

        for (i in 0 until model.rowCount) {
            val name = model.getValueAt(i, 0) as String
            val writeEnabled = model.getValueAt(i, 1) as? Boolean ?: false
            val timeout = (model.getValueAt(i, 2) as? String)?.toIntOrNull()
            val rowLimit = (model.getValueAt(i, 3) as? String)?.toIntOrNull()

            if (writeEnabled) settings.writeEnabledDatasources.add(name)
            if (timeout != null) settings.timeoutOverrides[name] = timeout
            if (rowLimit != null) settings.rowLimitOverrides[name] = rowLimit
        }
    }

    override fun reset() {}

    override fun disposeUIResources() {
        panel = null
        tableModel = null
    }
}
