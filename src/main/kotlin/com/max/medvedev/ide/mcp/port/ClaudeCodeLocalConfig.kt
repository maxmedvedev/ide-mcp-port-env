package com.max.medvedev.ide.mcp.port

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** The name of the server entry in the Claude Code config. */
internal const val IDE_SERVER_NAME: String = "idea"

/** Claude Code expands the variable from its own environment, so the entry connects to the IDE whose terminal started it. */
internal val IDE_SERVER_ENTRY: JsonObject = buildJsonObject {
  put("type", JsonPrimitive("http"))
  put("url", JsonPrimitive("http://127.0.0.1:\${$IJ_MCP_SERVER_PORT}/stream"))
}

/** The config file of Claude Code. The local scope of a project is the entry for the project path under `projects`. */
internal val claudeCodeConfigPath: Path
  get() = Path.of(System.getProperty("user.home"), ".claude.json")

private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }

/**
 * Writes [IDE_SERVER_ENTRY] to the local scope of [projectPath] in the Claude Code config at [configPath].
 * It replaces an existing entry with the name [IDE_SERVER_NAME] and keeps all other content.
 *
 * A config that is not a JSON object throws an exception, and the file stays as it is.
 */
internal fun addIdeServerToLocalConfig(configPath: Path, projectPath: String) {
  val config = if (configPath.exists()) json.parseToJsonElement(configPath.readText()).jsonObject else JsonObject(emptyMap())
  val tmp = configPath.resolveSibling("${configPath.fileName}.ide-mcp-port.tmp")
  tmp.writeText(json.encodeToString(JsonObject.serializer(), withIdeServer(config, projectPath)))
  try {
    Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  }
  catch (_: AtomicMoveNotSupportedException) {
    Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING)
  }
}

/** [config] with [IDE_SERVER_ENTRY] in `projects.<projectPath>.mcpServers`. */
internal fun withIdeServer(config: JsonObject, projectPath: String): JsonObject {
  val projects = config.objectAt("projects")
  val project = projects.objectAt(projectPath)
  val servers = project.objectAt("mcpServers")
  return config.with("projects", projects.with(projectPath, project.with("mcpServers", servers.with(IDE_SERVER_NAME, IDE_SERVER_ENTRY))))
}

private fun JsonObject.objectAt(key: String): JsonObject = get(key)?.jsonObject ?: JsonObject(emptyMap())

/** Keeps the position of an existing [key], so the file diff stays small. */
private fun JsonObject.with(key: String, value: JsonObject): JsonObject = JsonObject(LinkedHashMap(this).apply { put(key, value) })
