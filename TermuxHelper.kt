package com.yourcompany.yourapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

object TermuxHelper {
    const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"
    
    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
    
    fun sendCommand(context: Context, scriptPath: String, showToast: Boolean = true) {
        try {
            // Сначала запускаем Termux
            val termuxIntent = Intent().apply {
                setClassName("com.termux", "com.termux.app.TermuxActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(termuxIntent)
            
            // Небольшая задержка
            Thread.sleep(1000)
            
            // Отправляем команду
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
                setClassName(context.packageName, activityClass)
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
            Toast.makeText(context, "Ярлык '$uniqueName' создан", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}