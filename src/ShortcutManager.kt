package com.yourcompany.yourapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager  
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
    
    fun showManualDeleteDialog(context: Context, scriptName: String) {
        val config = IniHelper.getScriptConfig(scriptName)
        val displayName = config.name.ifEmpty { scriptName }
        
        android.app.AlertDialog.Builder(context)
        .setTitle("Удаление ярлыка")
        .setMessage("Android 7 не позволяет программно удалять ярлыки. Удалите ярлык '$displayName' с рабочего стола вручную.")
        .setPositiveButton("Я удалил") { _, _ ->
            IniHelper.updateScriptConfig(scriptName, config.copy(hasShortcut = false))
            Toast.makeText(context, "Отмечено как удаленный", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("Отменить", null)
        .show()
    }
    
    fun showShortcutUpdateDialog(context: Context, scriptName: String, scriptPath: String, oldConfig: ScriptConfig, newConfig: ScriptConfig) {
        android.app.AlertDialog.Builder(context)
        .setTitle("Обновить ярлык?")
        .setMessage("Имя или иконка изменились. Удалите старый ярлык '${oldConfig.name}' вручную с рабочего стола и создайте новый?")
        .setPositiveButton("Да, создать новый") { _, _ ->
            createShortcut(context, scriptName, scriptPath, newConfig.icon)
            IniHelper.updateScriptConfig(scriptName, newConfig)
        }
        .setNegativeButton("Нет, только настройки") { _, _ ->
            IniHelper.updateScriptConfig(scriptName, newConfig)
        }
        .show()
    }
    
    
/*     fun deleteShortcut(context: Context, scriptName: String, scriptPath: String) {
        try {
            // Точно такой же Intent как при создании
            val shortcutIntent = Intent().apply {
                component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val removeIntent = Intent("com.android.launcher.action.UNINSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, scriptName) // Точно такое же имя
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent) // Точно такой же интент
            }
            
            context.sendBroadcast(removeIntent)
            
            // Проверяем разрешение
            val hasPermission = context.checkPermission(
                "com.android.launcher.permission.UNINSTALL_SHORTCUT",
                android.os.Process.myPid(),
                android.os.Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
            
            Toast.makeText(context, 
                "Команда удаления отправлена. Разрешение: $hasPermission", 
            Toast.LENGTH_LONG).show()
            
            } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } */

fun deleteShortcut(context: Context, scriptName: String, scriptPath: String) {
    try {
        val shortcutIntent = Intent().apply {
            component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
            action = "RUN_SCRIPT"
            putExtra("script_path", scriptPath)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val removeIntent = Intent("com.android.launcher.action.UNINSTALL_SHORTCUT").apply {
            // 🔥 ДОБАВИТЬ ПАКЕТ ДЛЯ MIUI
            setPackage("com.miui.home")
            
            putExtra(Intent.EXTRA_SHORTCUT_NAME, scriptName)
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            
            // 🔥 ДОБАВИТЬ ДЛЯ СОВМЕСТИМОСТИ
            putExtra("duplicate", false)
        }
        
        context.sendBroadcast(removeIntent)
        
        // 🔥 ПОПРОБОВАТЬ ВТОРОЙ ВАРИАНТ ACTION
        try {
            val removeIntent2 = Intent("com.miui.home.action.UNINSTALL_SHORTCUT").apply {
                setPackage("com.miui.home")
                putExtra(Intent.EXTRA_SHORTCUT_NAME, scriptName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            }
            context.sendBroadcast(removeIntent2)
        } catch (e: Exception) {
            // Игнорируем если не поддерживается
        }
        
        val hasPermission = context.checkPermission(
            "com.android.launcher.permission.UNINSTALL_SHORTCUT",
            android.os.Process.myPid(),
            android.os.Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
        
        Toast.makeText(context, 
            "Команды удаления отправлены. Разрешение: $hasPermission", 
            Toast.LENGTH_LONG).show()
            
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
    
    
}