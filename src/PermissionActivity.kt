
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
    private val STORAGE_PERMISSION_REQUEST_CODE = 1
    private val SHORTCUT_PERMISSION_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IniHelper.init(this)
        if (hasStorageAndTermuxPermissions()) {
            if (hasShortcutPermission()) {
                checkFirstRun()
                startMainActivity()
            } else {
                requestShortcutPermission()
            }
        } else {
            requestStorageAndTermuxPermissions()
        }
    }

    private fun hasStorageAndTermuxPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val termux = ContextCompat.checkSelfPermission(this, "com.termux.permission.RUN_COMMAND") == PackageManager.PERMISSION_GRANTED
        return read && write && termux
    }

    private fun hasShortcutPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, "com.android.launcher.permission.INSTALL_SHORTCUT") == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAndTermuxPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "com.termux.permission.RUN_COMMAND"
            ),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestShortcutPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf("com.android.launcher.permission.INSTALL_SHORTCUT"),
            SHORTCUT_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    if (hasShortcutPermission()) {
                        checkFirstRun()
                        startMainActivity()
                    } else {
                        requestShortcutPermission()
                    }
                } else {
                    Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_SHORT).show()
                    if (hasShortcutPermission()) {
                        checkFirstRun()
                        startMainActivity()
                    } else {
                        requestShortcutPermission()
                    }
                }
            }
            SHORTCUT_PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkFirstRun()
                } else {
                    Toast.makeText(this, "Разрешение на ярлыки не предоставлено", Toast.LENGTH_SHORT).show()
                }
                startMainActivity()
            }
        }
    }

    private fun checkFirstRun() {
        try {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("first_run", true)) {
                IniHelper.cleanupOrphanedConfigs(this)
                if (hasShortcutPermission()) {
                    IniHelper.createShortcutsForExisting(this)
                }
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
