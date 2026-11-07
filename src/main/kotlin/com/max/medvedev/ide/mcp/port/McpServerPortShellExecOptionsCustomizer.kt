@file:Suppress("UnstableApiUsage")

package com.max.medvedev.ide.mcp.port

import com.intellij.mcpserver.impl.McpServerService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.platform.eel.EelDescriptor
import com.intellij.platform.eel.provider.LocalEelDescriptor
import org.jetbrains.plugins.terminal.startup.MutableShellExecOptions
import org.jetbrains.plugins.terminal.startup.ShellExecOptionsCustomizer
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/** The variable name that the stdio runner of the MCP server plugin reads. A Claude Code entry reads it too. */
internal const val IJ_MCP_SERVER_PORT: String = "IJ_MCP_SERVER_PORT"

/**
 * Sets [IJ_MCP_SERVER_PORT] in a new shell session to the port of the MCP server of this IDE.
 *
 * A Claude Code entry with the URL `http://127.0.0.1:${IJ_MCP_SERVER_PORT}/stream` then connects to this IDE instance.
 * This is necessary when several IDE instances run, because each instance binds a different port.
 *
 * The customizer removes the variable when the MCP server does not run or the shell is not on the local machine.
 * The IDE process can inherit the variable from the terminal of another IDE, and that value points to the wrong instance.
 *
 * The value is the port at the start of the session. A later restart of the MCP server does not change it.
 */
internal class McpServerPortShellExecOptionsCustomizer : ShellExecOptionsCustomizer {
  override fun customizeExecOptions(project: Project, shellExecOptions: MutableShellExecOptions) {
    shellExecOptions.setEnvironmentVariable(IJ_MCP_SERVER_PORT, portForShell(shellExecOptions.eelDescriptor)?.toString())
  }
}

/** The port for a shell in [shellEnvironment]. It is the port of the running MCP server for a local shell, and `null` for all other shells. */
internal fun portForShell(shellEnvironment: EelDescriptor): Int? =
  if (shellEnvironment == LocalEelDescriptor) runningServerPort() else null

/**
 * The port of the running MCP server, or `null`. A terminal session does not create the MCP server service.
 *
 * `McpServerService` is a class in 2026.1 to 2026.3 and an interface in later versions.
 * A direct call links to only one of the two forms, so the call goes through reflection.
 */
private fun runningServerPort(): Int? {
  val service = ApplicationManager.getApplication().getServiceIfCreated(McpServerService::class.java) ?: return null
  return try {
    portGetter.invoke(service) as Int
  }
  catch (e: InvocationTargetException) {
    // The getter throws an IllegalStateException when the server does not run.
    if (e.cause is IllegalStateException) null else throw e.cause ?: e
  }
}

private val portGetter: Method by lazy { McpServerService::class.java.getMethod("getPort") }
