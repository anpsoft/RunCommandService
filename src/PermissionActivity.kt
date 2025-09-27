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
            IniHelper.init(this)
            checkFirstRun()
            startMainActivity()
        } else {
            requestAllPermissions()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "com.termux.permission.RUN_COMMAND",
            "com.android.launcher.permission.INSTALL_SHORTCUT"
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAllPermissions() {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val hasStoragePermissions = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (hasStoragePermissions) {
                IniHelper.init(this)
            } else {
                Toast.makeText(this, "Без доступа к памяти приложение может работать некорректно", Toast.LENGTH_SHORT).show()
            }
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkFirstRun()
            } else {
                if (ContextCompat.checkSelfPermission(this, "com.android.launcher.permission.INSTALL_SHORTCUT") == PackageManager.PERMISSION_GRANTED) {
                    checkFirstRun()
                }
                Toast.makeText(this, "Некоторые разрешения не предоставлены", Toast.LENGTH_SHORT).show()
            }
            startMainActivity()
        }
    }

    private fun checkFirstRun() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("first_run", true)) {
            IniHelper.cleanupOrphanedConfigs()
            if (ContextCompat.checkSelfPermission(this, "com.android.launcher.permission.INSTALL_SHORTCUT") == PackageManager.PERMISSION_GRANTED) {
                IniHelper.createShortcutsForExisting(this)
            }
            prefs.edit().putBoolean("first_run", false).apply()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}