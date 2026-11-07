package com.max.medvedev.ide.mcp.port

import com.intellij.mcpserver.impl.McpServerService
import com.intellij.platform.eel.EelDescriptor
import com.intellij.platform.eel.EelOsFamily
import com.intellij.platform.eel.provider.LocalEelDescriptor
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests [portForShell]. `MutableShellExecOptions` is sealed and its implementation is internal to the terminal plugin,
 * so the test does not call the customizer itself.
 */
@TestApplication
class McpServerPortShellExecOptionsCustomizerTest {
  @Test
  fun `a local shell gets the port of the running server`(): Unit = timeoutRunBlocking {
    withServer {
      assertEquals(McpServerService.getInstance().port, portForShell(LocalEelDescriptor))
    }
  }

  @Test
  fun `a local shell gets no port when the server does not run`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.IO) { McpServerService.getInstance().stop() }
    awaitRunning(false)

    assertNull(portForShell(LocalEelDescriptor))
  }

  @Test
  fun `a shell on another machine gets no port`(): Unit = timeoutRunBlocking {
    withServer {
      assertNull(portForShell(OtherEnvironment))
    }
  }

  private suspend fun withServer(block: () -> Unit) {
    withContext(Dispatchers.IO) { McpServerService.getInstance().start() }
    try {
      awaitRunning(true)
      block()
    }
    finally {
      withContext(Dispatchers.IO) { McpServerService.getInstance().stop() }
    }
  }

  private suspend fun awaitRunning(running: Boolean) {
    while (McpServerService.getInstance().isRunning != running) {
      delay(10)
    }
  }

  private object OtherEnvironment : EelDescriptor {
    override val name: String = "Other environment"
    override val osFamily: EelOsFamily = EelOsFamily.Posix
  }
}
