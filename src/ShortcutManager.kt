package com.yourcompany.yourapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Environment
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
            
            Toast.makeText(context, "Ярлык создан", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
fun deleteShortcut(context: Context, scriptName: String, scriptPath: String) {
    try {
        val config = IniHelper.getScriptConfig(scriptName)
        val displayName = config.name.ifEmpty { scriptName }
        
        // ТОЧНО такой же Intent как в createShortcut
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
        IniHelper.updateScriptConfig(scriptName, config.copy(hasShortcut = false))
        
        Toast.makeText(context, "Команда удаления отправлена", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_LONG).show()
    }
}


    
    fun recreateShortcut(context: Context, scriptName: String, scriptPath: String, oldConfig: ScriptConfig, newConfig: ScriptConfig) {
        // Удаляем старый
        deleteShortcut(context, scriptName, scriptPath)
        
        // Небольшая задержка
        android.os.Handler().postDelayed({
            // Создаем новый с новыми параметрами
            createShortcut(context, scriptName, scriptPath, newConfig.icon)
        }, 500)
    }
}