package com.yourcompany.yourapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }
    }

    private fun sendTermuxIntent(context: Context) {
        val intent = Intent("com.termux.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
        }
        context.startService(intent)
        Toast.makeText(context, "Команда отправлена", Toast.LENGTH_SHORT).show()
    }

    private fun addShortcut(context: Context) {
        val shortcutIntent = Intent(context, MainActivity::class.java)
        shortcutIntent.action = Intent.ACTION_MAIN

        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, "UpdateWDS")
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher))
        }

        context.sendBroadcast(addIntent)
        Toast.makeText(context, "Ярлык создан", Toast.LENGTH_SHORT).show()
    }

    @Composable
    fun MainScreen() {
        val context = LocalContext.current
        Button(onClick = { addShortcut(context) }) {
            Text("Создать ярлык")
        }
        Button(onClick = { sendTermuxIntent(context) }) {
            Text("Отправить команду")
        }
    }
}
