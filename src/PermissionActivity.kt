package com.yourcompany.yourapp5

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.widget.Toast

class PermissionActivity : Activity() {
    private val PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasAllPermissions()) {
            checkFirstRun()
            startMainActivity()
        } else {
            requestPermissions()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val termux = ContextCompat.checkSelfPermission(this, "com.termux.permission.RUN_COMMAND") == PackageManager.PERMISSION_GRANTED
        val shortcut = ContextCompat.checkSelfPermission(this, "com.android.launcher.permission.INSTALL_SHORTCUT") == PackageManager.PERMISSION_GRANTED
        return read && write && termux && shortcut
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "com.termux.permission.RUN_COMMAND",
                "com.android.launcher.permission.INSTALL_SHORTCUT"
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            checkFirstRun()
            startMainActivity()
        } else {
            Toast.makeText(this, "Требуются все разрешения для работы приложения", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun checkFirstRun() {
        try {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("first_run", true)) {
                IniHelper.init(this)
                IniHelper.cleanupOrphanedConfigs(this)
                IniHelper.createShortcutsForExisting(this)
                prefs.edit().putBoolean("first_run", false).apply()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}