package com.yourcompany.yourapp

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

object TermuxHelper {
    const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"

    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isTermuxRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.runningAppProcesses ?: return false
        return runningApps.any { it.processName == "com.termux" }
    }

    fun startTermuxSilently(context: Context) {
        try {
            if (!isTermuxRunning(context)) {
                val intent = Intent()
                intent.component = ComponentName("com.termux", "com.termux.app.TermuxActivity")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Thread.sleep(3000) // Пауза только если запускаем
            }
        } catch (_: Exception) {
            // молча игнорируем ошибки
        }
    }

    fun sendCommand(context: Context, scriptPath: String, showToast: Boolean = true) {
        try {
            val commandIntent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", scriptPath)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(commandIntent)

            if (showToast) {
                Toast.makeText(context, "Команда отправлена", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun createShortcut(context: Context, name: String, scriptPath: String, activityClass: String, iconResource: Int) {
        try {
            val shortcutIntent = Intent().apply {
                component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val uniqueName = name + "_" + System.currentTimeMillis()

            val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, uniqueName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, iconResource))
            }
            context.sendBroadcast(addIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}