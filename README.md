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

**Option A: All tools (database + built-in IDE tools)**

```json
{
  "mcpServers": {
    "datagrip": {
      "command": "/path/to/datagrip",
      "args": ["mcp", "stdio"]
    }
  }
}
```

**Option B: Database tools only (recommended)**

Add the following deny list to `.claude/settings.local.json` (or `.claude/settings.json` for team-wide config) to hide DataGrip's built-in IDE tools and keep only the 6 database tools:

```json
{
  "permissions": {
    "deny": [
      "mcp__jetbrains__open_file_in_editor",
      "mcp__jetbrains__reformat_file",
      "mcp__jetbrains__execute_run_configuration",
      "mcp__jetbrains__get_run_configurations",
      "mcp__jetbrains__build_project",
      "mcp__jetbrains__get_file_problems",
      "mcp__jetbrains__get_project_dependencies",
      "mcp__jetbrains__get_project_modules",
      "mcp__jetbrains__create_new_file",
      "mcp__jetbrains__find_files_by_glob",
      "mcp__jetbrains__find_files_by_name_keyword",
      "mcp__jetbrains__get_all_open_file_paths",
      "mcp__jetbrains__list_directory_tree",
      "mcp__jetbrains__get_file_text_by_path",
      "mcp__jetbrains__replace_text_in_file",
      "mcp__jetbrains__search_in_files_by_regex",
      "mcp__jetbrains__search_in_files_by_text",
      "mcp__jetbrains__get_symbol_info",
      "mcp__jetbrains__rename_refactoring",
      "mcp__jetbrains__get_repositories"
    ]
  }
}
```

Replace `/path/to/datagrip` with the actual path:

| OS | Path |
|----|------|
| macOS (Toolbox) | `~/Library/Application Support/JetBrains/Toolbox/scripts/datagrip` |
| macOS (standalone) | `/Applications/DataGrip.app/Contents/MacOS/datagrip` |
| Linux (Toolbox) | `~/.local/share/JetBrains/Toolbox/scripts/datagrip` |
| Windows (Toolbox) | `%LOCALAPPDATA%\JetBrains\Toolbox\scripts\datagrip.cmd` |

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
