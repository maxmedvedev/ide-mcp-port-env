package com.max.medvedev.ide.mcp.port

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ClaudeCodeLocalConfigTest {
  @TempDir
  lateinit var dir: Path

  private val configPath: Path get() = dir.resolve(".claude.json")

  @Test
  fun `the entry has the url with the port variable`() {
    assertEquals(
      parse("""{"type": "http", "url": "http://127.0.0.1:${'$'}{IJ_MCP_SERVER_PORT}/stream"}"""),
      IDE_SERVER_ENTRY,
    )
  }

  @Test
  fun `a missing config gets the project with the entry`() {
    addIdeServerToLocalConfig(configPath, PROJECT)

    assertEquals(parse("""{"projects": {"$PROJECT": {"mcpServers": {"idea": $ENTRY}}}}"""), readConfig())
  }

  @Test
  fun `the entry keeps all other content and replaces an old entry`() {
    configPath.writeText(
      """
      {
        "numStartups": 12345678901234567890,
        "mcpServers": {"youtrack": {"type": "http", "url": "https://example.com"}},
        "projects": {
          "/other": {"mcpServers": {"idea": {"type": "sse", "url": "http://localhost:1/sse"}}},
          "$PROJECT": {
            "allowedTools": [],
            "mcpServers": {"idea": {"type": "sse", "url": "http://localhost:1/sse"}, "other": {"command": "x"}},
            "hasTrustDialogAccepted": true
          }
        }
      }
      """.trimIndent()
    )

    addIdeServerToLocalConfig(configPath, PROJECT)

    val expected = parse(
      """
      {
        "numStartups": 12345678901234567890,
        "mcpServers": {"youtrack": {"type": "http", "url": "https://example.com"}},
        "projects": {
          "/other": {"mcpServers": {"idea": {"type": "sse", "url": "http://localhost:1/sse"}}},
          "$PROJECT": {
            "allowedTools": [],
            "mcpServers": {"idea": $ENTRY, "other": {"command": "x"}},
            "hasTrustDialogAccepted": true
          }
        }
      }
      """
    )
    assertEquals(expected, readConfig())
    assertEquals(expected.keys.toList(), readConfig().keys.toList())
    assertEquals(listOf(configPath), dir.listDirectoryEntries())
  }

  @Test
  fun `a malformed config stays as it is`() {
    configPath.writeText("{ not json")

    assertThrows<SerializationException> { addIdeServerToLocalConfig(configPath, PROJECT) }

    assertEquals("{ not json", configPath.readText())
  }

  private fun readConfig(): JsonObject = parse(configPath.readText())

  private fun parse(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

  private companion object {
    const val PROJECT = "/work/project"
    val ENTRY = IDE_SERVER_ENTRY.toString()
  }
}
