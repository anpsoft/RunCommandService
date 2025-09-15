package com.yourcompany.yourapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    
    companion object {
        const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Проверяем запуск из ярлыка
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path")
            if (checkSelfPermission(TERMUX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog()
                return
            }
            scriptPath?.let { sendTermuxIntent(this, it) }
            finish()
            return
        }
        
        // Обычный UI только если запуск не из ярлыка
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val createShortcutButton = Button(this).apply {
            text = "Создать ярлык"
            setOnClickListener { 
                addShortcutToHomeScreen(this@MainActivity, "UpdateWDS", "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh") 
            }
        }
        
        val runCommandButton = Button(this).apply {
            text = "Отправить команду"
            setOnClickListener { 
                if (checkSelfPermission(TERMUX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    showPermissionDialog()
                } else {
                    sendTermuxIntent(this@MainActivity, "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh")
                }
            }
        }
        
        layout.addView(createShortcutButton)
        layout.addView(runCommandButton)
        setContentView(layout)
    }
    
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Требуется разрешение")
            .setMessage("Для работы с Termux нужно предоставить разрешение. Перейдите в Настройки > Приложения > ${packageManager.getApplicationLabel(applicationInfo)} > Разрешения и включите 'Запуск команд Termux'")
            .setPositiveButton("Открыть настройки") { _, _ ->
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton("Отмена") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun sendTermuxIntent(context: Context, scriptPath: String) {
        try {
            // Сначала запускаем Termux
            val termuxIntent = Intent().apply {
                setClassName("com.termux", "com.termux.app.TermuxActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(termuxIntent)
            
            // Небольшая задержка, чтобы Termux успел запуститься
            Thread.sleep(1000)
            
            // Теперь отправляем команду
            val commandIntent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", scriptPath)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(commandIntent)
            
            if (intent.action != "RUN_SCRIPT") {
                Toast.makeText(context, "Команда отправлена", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun addShortcutToHomeScreen(context: Context, name: String, scriptPath: String) {
        try {
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val uniqueName = name + "_" + System.currentTimeMillis()
            
            val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, uniqueName)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_shortcut))
            }
            context.sendBroadcast(addIntent)
            Toast.makeText(context, "Ярлык '$uniqueName' создан", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}