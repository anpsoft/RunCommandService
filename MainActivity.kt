package com.yourcompany.yourapp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path") ?: return
            sendTermuxIntent(this, scriptPath)
            finish()
        } else {
            setContent {
                MainScreen { addShortcutToHomeScreen(this, "UpdateWDS", "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh") }
            }
        }
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

@Composable
fun MainScreen(onCreateShortcut: () -> Unit) {
    Button(onClick = onCreateShortcut, modifier = Modifier.fillMaxSize()) {
        Text("Создать иконку для UpdateWDS.sh")
    }
}