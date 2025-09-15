package com.yourcompany.yourapp

import android.app.Activity
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
        const val PERMISSION_REQUEST_CODE = 1
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Проверяем запуск из ярлыка
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path")
            scriptPath?.let { sendTermuxIntent(this, it) }
            finish() // Сразу закрываем без показа UI
            return
        }
        
        // Обычный UI только если запуск не из ярлыка
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
                if (checkAndRequestPermission()) {
                    sendTermuxIntent(this@MainActivity, "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh")
                }
            }
        }
        
        layout.addView(createShortcutButton)
        layout.addView(runCommandButton)
        setContentView(layout)
    }
    
    private fun checkAndRequestPermission(): Boolean {
        if (checkSelfPermission(TERMUX_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(TERMUX_PERMISSION), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Разрешение не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }
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
            
            Toast.makeText(context, "Команда отправлена", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun addShortcutToHomeScreen(context: Context, name: String, scriptPath: String) {
        try {
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = "RUN_SCRIPT"
                putExtra("script_path", scriptPath)
            }
            
            val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher))
            }
            context.sendBroadcast(addIntent)
            Toast.makeText(context, "Ярлык создан", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка создания ярлыка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}