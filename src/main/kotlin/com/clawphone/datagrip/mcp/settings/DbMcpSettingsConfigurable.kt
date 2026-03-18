package com.clawphone.datagrip.mcp.settings

import com.intellij.openapi.options.Configurable
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

        tableModel = DefaultTableModel(
            arrayOf("Datasource", "Write Enabled", "Timeout (s)", "Row Limit"),
            0,
        ).apply {
            for (ds in settings.writeEnabledDatasources) {
                addRow(arrayOf(
                    ds,
                    true,
                    settings.timeoutOverrides[ds] ?: "",
                    settings.rowLimitOverrides[ds] ?: "",
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

        val currentNames = (0 until model.rowCount).map { model.getValueAt(it, 0) as String }.toSet()
        return currentNames != settings.writeEnabledDatasources
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
