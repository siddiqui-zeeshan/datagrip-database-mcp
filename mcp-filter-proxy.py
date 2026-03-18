#!/usr/bin/env python3
"""
MCP filter proxy for DataGrip Database MCP Tools.

Wraps DataGrip's MCP stdio server and filters the tool list to only expose
database tools (list_datasources, get_schema, run_query, explain_query,
get_table_info, search_schema). All other built-in tools are hidden.

Usage in Claude Code MCP config:
{
  "mcpServers": {
    "datagrip-db": {
      "command": "python3",
      "args": ["/path/to/mcp-filter-proxy.py", "/path/to/datagrip", "mcp", "stdio"]
    }
  }
}
"""

import json
import subprocess
import sys
import threading

ALLOWED_TOOLS = {
    "list_datasources",
    "get_schema",
    "run_query",
    "explain_query",
    "get_table_info",
    "search_schema",
}


def filter_response(line: bytes) -> bytes:
    """Filter tools/list responses to only include database tools."""
    try:
        msg = json.loads(line)
    except (json.JSONDecodeError, UnicodeDecodeError):
        return line

    # Filter tools/list response
    result = msg.get("result")
    if isinstance(result, dict) and "tools" in result:
        result["tools"] = [
            t for t in result["tools"] if t.get("name") in ALLOWED_TOOLS
        ]
        return (json.dumps(msg) + "\n").encode()

    return line


def pipe_output(proc: subprocess.Popen):
    """Read from child stdout, filter, and write to our stdout."""
    for line in proc.stdout:
        filtered = filter_response(line)
        sys.stdout.buffer.write(filtered)
        sys.stdout.buffer.flush()


def main():
    if len(sys.argv) < 2:
        print(
            "Usage: mcp-filter-proxy.py <datagrip-command> [args...]",
            file=sys.stderr,
        )
        sys.exit(1)

    cmd = sys.argv[1:]
    proc = subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=sys.stderr,
    )
    assert proc.stdin is not None
    assert proc.stdout is not None

    # Forward child stdout (filtered) in a thread
    output_thread = threading.Thread(target=pipe_output, args=(proc,), daemon=True)
    output_thread.start()

    # Forward our stdin to child stdin
    try:
        for line in sys.stdin.buffer:
            proc.stdin.write(line)
            proc.stdin.flush()
    except BrokenPipeError:
        pass
    finally:
        proc.stdin.close()
        proc.wait()
        sys.exit(proc.returncode)


if __name__ == "__main__":
    main()
