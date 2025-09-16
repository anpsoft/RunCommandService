package com.yourcompany.yourapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class ShortcutActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent.action == "RUN_SCRIPT") {
            val scriptPath = intent.getStringExtra("script_path")
            
            if (!TermuxHelper.hasPermission(this)) {
                showPermissionDialog()
                return
            }
            
            scriptPath?.let { 
                TermuxHelper.startTermuxSilently(this)
                // Небольшая задержка
                Thread.sleep(1000)
                TermuxHelper.sendCommand(this, it, showToast = true) 
            }
            finish()
            } else {
            finish()
        }
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