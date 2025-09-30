// ShortcutManager.kt
package com.yourcompany.yourapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode


import android.util.Log
import android.widget.Toast
import java.io.File



object ShortcutManager {
    
    fun getIconResource(iconName: String): Int {
        Log.d("ShortcutManager", "getIconResource: icon=$iconName, returning default: ic_no_icon")
        return R.mipmap.ic_no_icon
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

            if (iconName.isNotEmpty()) {
                val iconFile = File(IniHelper.getIconsDir(), iconName)
                if (iconFile.exists() && (iconName.endsWith(".png", true) || iconName.endsWith(".jpg", true))) {
                    Log.d("ShortcutManager", "Using custom icon file: ${iconFile.absolutePath}")

                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath, options)

if (bitmap != null) {
    // копируем в ARGB_8888
    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    // чёрный фон под прозрачность
    canvas.drawColor(Color.BLACK, PorterDuff.Mode.DST_OVER)

    putExtra(Intent.EXTRA_SHORTCUT_ICON, result)
} else {
    Log.w("ShortcutManager", "Failed to decode bitmap, using default")
    putExtra(
        Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_no_icon)
    )
}
                    
                    
                } else {
                    Log.w("ShortcutManager", "Icon file not found or invalid, using default")
                    putExtra(
                        Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                        Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_no_icon)
                    )
                }
            } else {
                Log.d("ShortcutManager", "No icon specified, using default")
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_no_icon)
                )
            }
        }

        context.sendBroadcast(addIntent)
        IniHelper.updateScriptConfig(scriptName, config.copy(hasShortcut = true))
        Toast.makeText(context, "Ярлык создан", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("ShortcutManager", "Failed to create shortcut: ${e.message}")
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
        .setNegativeButton("Отмена", null)
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
    
    fun deleteShortcut(context: Context, scriptName: String, scriptPath: String) {
        try {
            val shortcutIntent = Intent().apply {
                component = ComponentName(context.packageName, "${context.packageName}.ShortcutActivity")
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val removeIntent = Intent("com.android.launcher.action.UNINSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, scriptName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra("duplicate", false)
            }
            
            Log.d("ShortcutManager", "Deleting shortcut: name=$scriptName, path=$scriptPath")
            context.sendBroadcast(removeIntent)
            
            try {
                val removeIntent2 = Intent("com.miui.home.action.UNINSTALL_SHORTCUT").apply {
                    setPackage("com.miui.home")
                    putExtra(Intent.EXTRA_SHORTCUT_NAME, scriptName)
                    putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                }
                context.sendBroadcast(removeIntent2)
                } catch (e: Exception) {
                Log.e("ShortcutManager", "MIUI shortcut removal failed: ${e.message}")
            }
            
            val hasPermission = context.checkPermission(
                "com.android.launcher.permission.UNINSTALL_SHORTCUT",
                android.os.Process.myPid(),
                android.os.Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
            
            Toast.makeText(context,
                "Команды удаления отправлены. Разрешение: $hasPermission",
            Toast.LENGTH_LONG).show()
            
            IniHelper.updateScriptConfig(scriptName, IniHelper.getScriptConfig(scriptName).copy(hasShortcut = false))
            } catch (e: Exception) {
            Log.e("ShortcutManager", "Failed to delete shortcut: ${e.message}")
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}