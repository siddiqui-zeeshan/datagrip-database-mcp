# Database MCP Tools

A DataGrip plugin that extends its built-in MCP server with 6 database tools. AI assistants like Claude Code can query databases through DataGrip's existing connections -- SSH tunnels, SSL, and auth included -- with zero extra config.

## Requirements

- DataGrip 2025.2+
- JDK 17+

## Building

```
./gradlew buildPlugin
```

The plugin zip is written to `build/distributions/`.

## Installation

1. In DataGrip, go to **Settings > Plugins**.
2. Click the gear icon and select **Install Plugin from Disk...**.
3. Select the zip from `build/distributions/`.
4. Restart DataGrip.

## Setup

1. Enable the MCP server in DataGrip: **Settings > Tools > MCP Server** (requires DataGrip 2025.2+).
2. Datasources auto-connect on demand -- no manual connect step needed (passwords must be saved in DataGrip).
3. Configure Claude Code (or another MCP client) to use DataGrip's MCP server.

**Option A: Database tools only (recommended)**

Use the included filter proxy to expose only the 6 database tools. This strips DataGrip's built-in IDE tools from the MCP response, keeping your AI assistant's context clean.

```json
{
  "mcpServers": {
    "datagrip-db": {
      "command": "python3",
      "args": [
        "/path/to/mcp-filter-proxy.py",
        "/path/to/java",
        "-classpath",
        "/path/to/mcpserver-classpath",
        "com.intellij.mcpserver.stdio.McpStdioRunnerKt"
      ],
      "env": {
        "IJ_MCP_SERVER_PORT": "<port>"
      }
    }
  }
}
```

See [Classpath and port](#classpath-and-port) below for how to find the correct values.

**Option B: All tools (database + built-in IDE tools)**

Connect directly to DataGrip's MCP server without filtering. This exposes all built-in IDE tools alongside the database tools.

```json
{
  "mcpServers": {
    "datagrip": {
      "command": "/path/to/java",
      "args": [
        "-classpath",
        "/path/to/mcpserver-classpath",
        "com.intellij.mcpserver.stdio.McpStdioRunnerKt"
      ],
      "env": {
        "IJ_MCP_SERVER_PORT": "<port>"
      }
    }
  }
}
```

### Classpath and port

The MCP server runs via DataGrip's bundled JRE, not the DataGrip CLI. You need two things:

1. **Java binary**: `<DataGrip install>/Contents/jbr/Contents/Home/bin/java` (macOS) or the equivalent on your OS.
2. **Classpath**: the JARs under `<DataGrip install>/Contents/plugins/mcpserver/lib/` and `<DataGrip install>/Contents/lib/`. See the [global MCP setup](#classpath-and-port) section for details.
3. **Port**: set `IJ_MCP_SERVER_PORT` to the port shown in DataGrip under **Settings > Tools > MCP Server**.

| OS | DataGrip install path |
|----|------|
| macOS (Toolbox) | `~/Library/Application Support/JetBrains/Toolbox/apps/datagrip/` |
| macOS (standalone) | `/Applications/DataGrip.app/` |
| Linux (Toolbox) | `~/.local/share/JetBrains/Toolbox/apps/datagrip/` |
| Windows (Toolbox) | `%LOCALAPPDATA%\JetBrains\Toolbox\apps\datagrip\` |

The 6 database tools will appear automatically once the plugin is installed and the MCP server is enabled.

## Tools

| Tool | Parameters | Returns |
|------|-----------|---------|
| `list_datasources` | _(none)_ | Name, driver, connection status, and write-enabled flag for each datasource |
| `get_schema` | `datasource`, `schema?` | Tables with columns, types, primary keys, and nullability |
| `run_query` | `datasource`, `sql`, `rowLimit?`, `timeout?` | Columns, rows, row count, and whether results were truncated |
| `explain_query` | `datasource`, `sql` | Execution plan (auto-prefixes the correct EXPLAIN syntax for the DB driver) |
| `get_table_info` | `datasource`, `table`, `schema?` | Columns, types, primary keys, foreign keys, and indexes for a single table |
| `search_schema` | `datasource`, `pattern` | Tables and columns matching the pattern, with their types |

Datasources auto-connect on demand. Passwords must be saved in DataGrip for unattended connections.

## Safety

**Read-only by default.** Only `SELECT`, `EXPLAIN`, `SHOW`, `DESCRIBE`, and `WITH` statements are allowed unless writes are explicitly enabled.

- **Per-datasource write toggle**: Settings > Tools > Database MCP Tools
- **Query timeout**: 30 seconds (default), configurable per datasource
- **Row limit**: 1000 rows (default), configurable per datasource and overridable per query via the `rowLimit` parameter
- **Response size cap**: ~100KB -- results are truncated if they exceed this

## Development

Launch a DataGrip sandbox with the plugin loaded:

```
./gradlew runIde
```

Run tests:

```
./gradlew test
```
