package com.yourcompany.yourapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                sendTermuxIntent(this@MainActivity, "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh") 
            }
        }
        
        layout.addView(createShortcutButton)
        layout.addView(runCommandButton)
        setContentView(layout)
    }
    
    // ВСЯ ОСТАЛЬНАЯ ЛОГИКА ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ
    private fun sendTermuxIntent(context: Context, scriptPath: String) {
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", scriptPath)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        context.startService(intent)
    }
    
    private fun addShortcutToHomeScreen(context: Context, name: String, scriptPath: String) {
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
        Toast.makeText(context, "Иконка создана", Toast.LENGTH_SHORT).show()
    }
}