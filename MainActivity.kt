package com.yourcompany.yourapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если запущено через Intent RUN_SCRIPT
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path") ?: return
            sendTermuxIntent(this, scriptPath)
            finish()
            return
        }

        // Создаём простой UI без Compose
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val button = Button(this)
        button.text = "Создать иконку для UpdateWDS.sh"
        button.setOnClickListener {
            addShortcutToHomeScreen(this, "UpdateWDS", "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh")
        }
        layout.addView(button)
        setContentView(layout)
    }

    private fun sendTermuxIntent(context: Context, scriptPath: String) {
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", scriptPath)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        context.startService(intent)

        val focusIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName("com.termux", "com.termux.app.TermuxActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(focusIntent)
    }

    private fun addShortcutToHomeScreen(context: Context, name: String, scriptPath: String) {
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = "RUN_SCRIPT"
            putExtra("script_path", scriptPath)
        }

        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher))
        }

        context.sendBroadcast(addIntent)
        Toast.makeText(context, "Иконка создана", Toast.LENGTH_SHORT).show()
    }
}
