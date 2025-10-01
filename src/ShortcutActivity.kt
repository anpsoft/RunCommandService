package com.yourcompany.yourapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import java.io.File

class ShortcutActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IniHelper.init(this) // Инициализация IniHelper
        
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path")
            
            scriptPath?.let { 
                val file = File(it)
                val scriptName = file.nameWithoutExtension
                val config = IniHelper.getScriptConfig(scriptName)
                
                if (!file.exists() || !config.isActive || !config.hasShortcut) {
                    showInvalidShortcutDialog(scriptName)
                    return // Не вызываем finish() здесь, ждем действия в диалоге
                }
                
                if (!TermuxHelper.hasPermission(this)) {
                    showPermissionDialog()
                    return // Не вызываем finish() здесь, ждем действия в диалоге
                }
                
                TermuxHelper.startTermuxSilently(this)
                Thread.sleep(1500)
                TermuxHelper.sendCommand(this, it, showToast = true)
                finish() // Завершаем только после успешного выполнения
            } ?: run {
                finish() // Если scriptPath null, завершаем
            }
        } else {
            finish() // Если action не RUN_SCRIPT, завершаем
        }
    }

    private fun showInvalidShortcutDialog(scriptName: String) {
        AlertDialog.Builder(this)
            .setTitle("Неактивный ярлык")
            .setMessage("Скрипт '$scriptName' удален или неактивен. Удалите этот ярлык с рабочего стола вручную.")
            .setPositiveButton("Перейти к программе") { _, _ ->
                try {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Не удалось открыть приложение: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setNegativeButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }    
    
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Требуется разрешение")
            .setMessage("Для работы с Termux нужно предоставить разрешение в настройках приложения")
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
}