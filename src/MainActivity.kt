package com.yourcompany.yourapp5

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptAdapter
    private var showAllScripts = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Запрос прав сразу, но НЕ лезем в файлы
        requestPermissions()
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Добавление меню настроек
// В layout, добавить ImageButton для настроек
val settingsButton = android.widget.ImageButton(this).apply {
    setImageResource(android.R.drawable.ic_menu_preferences)
    layoutParams = android.widget.LinearLayout.LayoutParams(48.dp, 48.dp).apply {
        gravity = Gravity.END or Gravity.TOP
    }
    setOnClickListener { showSettingsMenu() }
}
layout.addView(settingsButton, 0) // Сверху справа, но LinearLayout - подкорректировать
        
        val createShortcutButton = Button(this).apply {
            text = "Создать ссылк"
            setOnClickListener {
                TermuxHelper.createShortcut(
                    this@MainActivity,
                    "UpdateWDS",
                    "/sdcard/MyScripts/UpdateWDS.sh",
                    "${packageName}.ShortcutActivity",
                    R.mipmap.ic_shortcut
                )
            }
        }
        
        val runCommandButton = Button(this).apply {
            text = "Отправить команду"
            setOnClickListener {
                if (!TermuxHelper.hasPermission(this@MainActivity)) {
                    showPermissionDialog()
                    } else {
                    TermuxHelper.sendCommand(this@MainActivity, "/sdcard/MyScripts/UpdateWDS.sh")
                }
            }
        }
        
        layout.addView(createShortcutButton)
        layout.addView(runCommandButton)
        
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 4, 4, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val headerIcon = TextView(this).apply {
            text = "Иконка"
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 48.dp
            }
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        
        val headerText = TextView(this).apply {
            text = "Имя / Описание"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(4, 0, 4, 0)
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        
        val headerActive = TextView(this).apply {
            text = "A"
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 48.dp
            }
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        
        val headerShortcut = TextView(this).apply {
            text = "S"
            layoutParams = LinearLayout.LayoutParams(48.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 48.dp
            }
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        
        val headerTest = TextView(this).apply {
            text = "▶️"
            layoutParams = LinearLayout.LayoutParams(60.dp, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = 60.dp
            }
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF0F0F0.toInt())
        }
        
        headerLayout.addView(headerIcon)
        headerLayout.addView(headerText)
        headerLayout.addView(headerActive)
        headerLayout.addView(headerShortcut)
        headerLayout.addView(headerTest)
        layout.addView(headerLayout)
        
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        adapter = ScriptAdapter(this, ::onScriptSettings, ::onTestRun)
        recyclerView.adapter = adapter
        
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        scrollView.addView(recyclerView)
        layout.addView(scrollView)
        
        val bottomButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val refreshButton = Button(this).apply {
            text = "Обновить"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { if (hasStoragePermission()) updateScriptList() }
        }
        val createScriptButton = Button(this).apply {
            text = "Новый"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { if (hasStoragePermission()) createNewScript() }
        }
        val showAllButton = Button(this).apply {
            text = "Все"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showAllScripts = !showAllScripts
                text = if (showAllScripts) "Активные" else "Все"
                if (hasStoragePermission()) updateScriptList()
            }
        }
        bottomButtons.addView(refreshButton)
        bottomButtons.addView(createScriptButton)
        bottomButtons.addView(showAllButton)
        layout.addView(bottomButtons)
        
        setContentView(layout)
    }
    
    
    private fun showSettingsMenu() {
        val items = arrayOf("Настройки", "Инструкция", "О программе")
    AlertDialog.Builder(this)
        .setTitle("Меню")
        .setItems(items) { _, which ->
            when (which) {
                0 -> startActivity(Intent(this, SettingsActivity::class.java))
                1 -> startActivity(Intent(this, InstructionsActivity::class.java))
                2 -> startActivity(Intent(this, AboutActivity::class.java))
            }
        }
        .show()
}
    
    override fun onResume() {
        super.onResume()
        
        if (hasStoragePermission()) {
            checkFirstRun()
            updateScriptList()
        }
        
    }
    
private fun checkFirstRun() {
    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
    if (prefs.getBoolean("first_run", true)) {
        if (!hasStoragePermission()) return
        IniHelper.cleanupOrphanedConfigs(this)
        IniHelper.createShortcutsForExisting(this)
        prefs.edit().putBoolean("first_run", false).apply()
    }
}
    
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "com.android.launcher.permission.INSTALL_SHORTCUT",
            "com.termux.permission.RUN_COMMAND"
        )
        ActivityCompat.requestPermissions(this, permissions, 1)
    }
    
    
    private fun hasStoragePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    
private fun updateScriptList() {
    val scriptsDir = IniHelper.getScriptsDir(this)
    val dirFile = File(scriptsDir)
    dirFile.mkdirs()
    val iconsDir = File(IniHelper.getIconsDir(this))
    iconsDir.mkdirs()
    
    IniHelper.cleanupOrphanedConfigs(this)
    
    val scripts = dirFile.listFiles { _, name -> name.endsWith(".sh") }?.map { file ->
        Script(file.nameWithoutExtension, "$scriptsDir/${file.name}")
    }?.filter { showAllScripts || IniHelper.getScriptConfig(it.name).isActive } ?: emptyList()
    adapter.updateScripts(scripts)
}


    private fun createNewScript() {
        val editText = EditText(this).apply { hint = "Имя скрипта" }
        AlertDialog.Builder(this)
        .setTitle("Новый скрипт")
        .setView(editText)
        .setPositiveButton("OK") { _, _ ->
            val name = editText.text.toString()
            if (name.isNotEmpty()) {
                val scriptFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$name.sh")
                if (scriptFile.createNewFile()) {
                    IniHelper.addScriptConfig(name, ScriptConfig(name = name, isActive = true))
                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(Uri.fromFile(scriptFile), "text/plain")
                    }
                    val chooser = Intent.createChooser(intent, "Открыть редактором")
                    if (chooser.resolveActivity(packageManager) != null) {
                        startActivity(chooser)
                        } else {
                        Toast.makeText(this, "Нет редактора", Toast.LENGTH_SHORT).show()
                    }
                    updateScriptList()
                    } else {
                    Toast.makeText(this, "Файл уже существует", Toast.LENGTH_SHORT).show()
                }
            }
        }
        .setNegativeButton("Отмена", null)
        .show()
    }
    
    private fun onScriptSettings(script: Script) {
        val intent = Intent(this, ScriptSettingsActivity::class.java).apply {
            putExtra("script_name", script.name)
        }
        startActivity(intent)
    }
    
    override fun onStart() {
        super.onStart()
        if (hasStoragePermission()) {
            checkFirstRun()
            updateScriptList()
        }
        
    }
    
    private fun onTestRun(script: Script) {
        if (!TermuxHelper.hasPermission(this)) {
            showPermissionDialog()
            return
        }
        val file = File(script.path)
        if (!file.exists()) {
            Toast.makeText(this, "Скрипт не существует: ${script.path}", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxHelper.startTermuxSilently(this)
        Thread.sleep(500) // 1500
        TermuxHelper.sendCommand(this, script.path)
    }
    
    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
        .setTitle("Требуется разрешение")
        .setMessage("Для работы с Termux нужно предоставить разрешение. Перейдите в Настройки > Приложения > ${packageManager.getApplicationLabel(applicationInfo)} > Разрешения и включите 'Запуск команд Termux'")
        .setPositiveButton("Открыть настройки") { _, _ ->
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
                } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Отмена", null)
        .show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
        ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // Теперь точно есть права — выполняем первый запуск
            checkFirstRun()
            updateScriptList()
            } else {
            Toast.makeText(this, "Разрешения не даны — приложение не сможет работать полностью", Toast.LENGTH_SHORT).show()
        }
    }
    
    
    private val Int.dp: Int
    get() = (this * resources.displayMetrics.density).toInt()
}
