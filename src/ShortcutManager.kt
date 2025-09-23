package com.yourcompany.yourapp

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.io.File

object ShortcutManager {
    
    fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "icon.png" -> R.mipmap.ic_launcher
            "Terminal.png" -> R.mipmap.ic_shortcut
            "no_icon.png" -> R.mipmap.ic_no_icon
            else -> R.mipmap.ic_no_icon
        }
    }
    
    fun createShortcut(context: Context, scriptName: String, scriptPath: String, iconName: String) {
        try {
            val config = IniHelper.getScriptConfig(scriptName)
            val displayName = config.name.ifEmpty { scriptName }
            
            val shortcutIntent = Intent().apply {
                component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, getIconResource(iconName)))
            }
            
            context.sendBroadcast(addIntent)
            
            // Обновляем конфиг
            IniHelper.updateScriptConfig(scriptName, config.copy(hasShortcut = true))
            
            Toast.makeText(context, "Ярлык создан: $displayName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun deleteShortcut(context: Context, scriptName: String, scriptPath: String, showInstructions: Boolean = true) {
        try {
            val config = IniHelper.getScriptConfig(scriptName)
            val displayName = config.name.ifEmpty { scriptName }
            
            val shortcutIntent = Intent().apply {
                component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val removeIntent = Intent("com.android.launcher.action.UNINSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            }
            
            context.sendBroadcast(removeIntent)
            
            // Обновляем конфиг
            IniHelper.updateScriptConfig(scriptName, config.copy(hasShortcut = false))
            
            if (showInstructions) {
                showRemovalInstructions(context, displayName)
            } else {
                Toast.makeText(context, "Команда удаления отправлена", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка удаления ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showRemovalInstructions(context: Context, displayName: String) {
        AlertDialog.Builder(context)
            .setTitle("Удаление ярлыка")
            .setMessage("Отправлена команда удаления ярлыка '$displayName'.\n\nЕсли ярлык остался на рабочем столе:\n1. Найдите его\n2. Долго нажмите\n3. Выберите 'Удалить'")
            .setPositiveButton("Понятно", null)
            .show()
    }
    
    fun recreateShortcut(context: Context, scriptName: String, scriptPath: String, oldConfig: ScriptConfig, newConfig: ScriptConfig) {
        // Удаляем старый БЕЗ инструкций
        deleteShortcut(context, scriptName, scriptPath, false)
        
        // Создаем новый с новыми параметрами
        android.os.Handler().postDelayed({
            createShortcut(context, scriptName, scriptPath, newConfig.icon)
            Toast.makeText(context, "Ярлык пересоздан", Toast.LENGTH_SHORT).show()
        }, 1000)
    }
    
    // Синхронизация состояния при первом запуске
    fun syncShortcutStates(context: Context, scripts: List<Script>) {
        scripts.forEach { script ->
            val config = IniHelper.getScriptConfig(script.name)
            if (config.hasShortcut) {
                // Ярлык должен быть, но мы не знаем есть ли он реально
                // Просто показываем что он отмечен
            }
        }
    }
}