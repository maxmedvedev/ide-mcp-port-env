# IDE MCP Port in Terminal

The plugin sets `IJ_MCP_SERVER_PORT` in each new local terminal session of the IDE.
The value is the port of the MCP server of this IDE instance.

Several IDE instances bind different MCP server ports.
A Claude Code entry with a fixed port often connects to another instance.
With this plugin, the entry can read the port from the environment:

```json
"idea": { "type": "http", "url": "http://127.0.0.1:${IJ_MCP_SERVER_PORT}/stream" }
```

The plugin removes the variable when the MCP server does not run, or when the shell is not local.
The URL has no default port. Thus a missing variable shows "Failed to connect" in Claude Code.

The value is the port at the start of the session. A later restart of the MCP server does not change it.

## Build

- `./gradlew buildPlugin` writes the plugin ZIP to `build/distributions/`.
- `./gradlew test` runs the tests.
- `./gradlew runIde` starts an IDE with the plugin.

The plugin needs IntelliJ IDEA 2026.1 (build 261) or later, with the bundled MCP Server and Terminal plugins.
