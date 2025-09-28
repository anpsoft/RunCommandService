// TermuxHelper.kt (удаляем дублирующие функции)
package com.yourcompany.yourapp

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

object TermuxHelper {
    const val TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND"

    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isTermuxRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.runningAppProcesses ?: return false
        return runningApps.any { it.processName == "com.termux" }
    }

    fun startTermuxSilently(context: Context) {
        try {
            if (!isTermuxRunning(context)) {
                val intent = Intent()
                intent.component = ComponentName("com.termux", "com.termux.app.TermuxActivity")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Thread.sleep(2000)
            }
        } catch (_: Exception) {
        }
    }

    fun sendCommand(context: Context, scriptPath: String, showToast: Boolean = true) {
        try {
            val termuxScriptPath = scriptPath.replace("/sdcard", "/storage/emulated/0")
            val startShPath = "/data/data/com.termux/files/home/Start.sh"
            val commandIntent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", startShPath)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(termuxScriptPath))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(commandIntent)

            if (showToast) {
                Toast.makeText(context, "Команда отправлена", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("TermuxHelper", "Failed to send command for $scriptPath: ${e.message}")
            if (showToast) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showPermissionDialogIfNeeded(context: Context, onPermissionGranted: () -> Unit) {
        if (!hasPermission(context)) {
            AlertDialog.Builder(context)
                .setTitle("Требуется разрешение")
                .setMessage("Для работы с Termux нужно предоставить разрешение. Перейдите в Настройки > Приложения > ${context.packageManager.getApplicationLabel(context.applicationInfo)} > Разрешения и включите 'Запуск команд Termux'")
                .setPositiveButton("Открыть настройки") { _, _ ->
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .setCancelable(false)
                .show()
        } else {
            onPermissionGranted()
        }
    }
}