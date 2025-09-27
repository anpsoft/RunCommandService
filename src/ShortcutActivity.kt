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
        
        
        
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path")
            
            scriptPath?.let { 
                val file = File(it)
                val scriptName = file.nameWithoutExtension
                val config = IniHelper.getScriptConfig(scriptName)
                
                if (!file.exists() || !config.isActive || !config.hasShortcut) {
                    showInvalidShortcutDialog(scriptName)
                    return
                }
                
                if (!TermuxHelper.hasPermission(this)) {
                    showPermissionDialog()
                    return
                }
                
                TermuxHelper.startTermuxSilently(this)
                Thread.sleep(1500)
                TermuxHelper.sendCommand(this, it, showToast = true)
            }
            
            finish()
        } else {
            finish()
        }
    }

    private fun showInvalidShortcutDialog(scriptName: String) {
        AlertDialog.Builder(this)
            .setTitle("Неактивный ярлык")
            .setMessage("Скрипт '$scriptName' удален или неактивен. Удалите этот ярлык с рабочего стола вручную.")
            .setPositiveButton("OK") { _, _ ->
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