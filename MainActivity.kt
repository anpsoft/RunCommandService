package com.yourcompany.yourapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

class MainActivity : Activity() {

    private val scriptPath = "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если Intent пришёл с RUN_SCRIPT, сразу запускаем
        if (intent.action == "RUN_SCRIPT") {
            sendTermuxIntent(this, scriptPath)
            finish()
            return
        }

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        // Кнопка: создать ярлык
        val shortcutButton = Button(this)
        shortcutButton.text = "Создать ярлык"
        shortcutButton.setOnClickListener {
            createShortcut(this, "UpdateWDS", scriptPath)
        }
        layout.addView(shortcutButton)

        // Кнопка: сразу запустить скрипт
        val runButton = Button(this)
        runButton.text = "Запустить скрипт"
        runButton.setOnClickListener {
            sendTermuxIntent(this, scriptPath)
        }
        layout.addView(runButton)

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

    private fun createShortcut(context: Context, name: String, scriptPath: String) {
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = "RUN_SCRIPT"
            putExtra("script_path", scriptPath)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, "shortcut_$name")
            .setShortLabel(name)
            .setIntent(shortcutIntent)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            Toast.makeText(context, "Ярлык создан (или предложен к добавлению)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Создание ярлыка не поддерживается этим лаунчером", Toast.LENGTH_SHORT).show()
        }
    }
}
