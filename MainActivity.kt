package com.yourcompany.yourapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                onCreateShortcut = { addShortcutToHomeScreen(this, "UpdateWDS", "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh") },
                onRunCommand = { sendTermuxIntent(this, "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh") }
            )
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

@Composable
fun MainScreen(onCreateShortcut: () -> Unit, onRunCommand: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onCreateShortcut) {
            Text("Создать ярлык")
        }
        Button(onClick = onRunCommand) {
            Text("Отправить команду")
        }
    }
}
