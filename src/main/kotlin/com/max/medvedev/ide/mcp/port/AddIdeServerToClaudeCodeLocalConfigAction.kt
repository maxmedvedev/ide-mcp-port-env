package com.max.medvedev.ide.mcp.port

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Adds the [IDE_SERVER_NAME] server to the local scope of the project in the Claude Code config.
 * Claude Code then connects to this IDE from a terminal of the project, also without an entry in the user scope.
 */
internal class AddIdeServerToClaudeCodeLocalConfigAction : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.project?.basePath != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val projectPath = project.basePath ?: return
    val configPath = claudeCodeConfigPath
    val error = runWithModalProgressBlocking(project, "Updating Claude Code config") {
      withContext(Dispatchers.IO) {
        try {
          addIdeServerToLocalConfig(configPath, projectPath)
          null
        }
        catch (e: IOException) {
          e
        }
        catch (e: SerializationException) {
          e
        }
        catch (e: IllegalArgumentException) {
          // jsonObject throws it when the config or one of its parts is not an object.
          e
        }
      }
    }
    if (error == null) {
      notify(project, NotificationType.INFORMATION, "Added the '$IDE_SERVER_NAME' MCP server to the local config of $projectPath in $configPath. Restart running Claude Code sessions.")
    }
    else {
      logger<AddIdeServerToClaudeCodeLocalConfigAction>().warn("Cannot update $configPath", error)
      notify(project, NotificationType.ERROR, "Cannot update $configPath: ${error.message}")
    }
  }

  private fun notify(project: Project, type: NotificationType, content: String) {
    NotificationGroupManager.getInstance().getNotificationGroup("IDE MCP Port").createNotification(content, type).notify(project)
  }
}
