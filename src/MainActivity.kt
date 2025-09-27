package com.yourcompany.yourapp5

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuItem
import java.io.File

class MainActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptAdapter
    private var showAllScripts = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            IniHelper.init(this)
            setupUI()
            updateScriptList()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
            setupUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateScriptList()
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val createShortcutButton = Button(this).apply {
            text = "Создать ссылку"
            setOnClickListener {
                TermuxHelper.createShortcut(
                    this@MainActivity,
                    "UpdateWDS",
                    "${Environment.getExternalStorageDirectory()}/MyScripts/UpdateWDS.sh",
                    "${packageName}.ShortcutActivity",
                    R.mipmap.ic_shortcut
                )
            }
        }

        val runCommandButton = Button(this).apply {
            text = "Отправить команду"
            setOnClickListener {
                TermuxHelper.showPermissionDialogIfNeeded(this@MainActivity) {
                    TermuxHelper.sendCommand(this@MainActivity, "${Environment.getExternalStorageDirectory()}/MyScripts/UpdateWDS.sh")
                }
            }
        }

        val testDeleteButton = Button(this).apply {
            text = "ТЕСТ УДАЛЕНИЯ ЯРЛЫКА"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setOnClickListener {
                val scriptPath = "${Environment.getExternalStorageDirectory()}/MyScripts/UpdateWDS.sh"
                ShortcutManager.deleteShortcut(this@MainActivity, "UpdateWDS", scriptPath)
            }
        }

        layout.addView(createShortcutButton)
        layout.addView(testDeleteButton)
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
        adapter = ScriptAdapter(this, { script: Script ->
            val intent = Intent(this, ScriptSettingsActivity::class.java).apply {
                putExtra("script_name", script.name)
            }
            startActivity(intent)
        }, { script: Script ->
            onTestRun(script)
        })
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
            setOnClickListener { updateScriptList() }
        }
        val createScriptButton = Button(this).apply {
            text = "Новый"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { createNewScript() }
        }
        val showAllButton = Button(this).apply {
            text = "Все"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                showAllScripts = !showAllScripts
                text = if (showAllScripts) "Активные" else "Все"
                updateScriptList()
            }
        }
        bottomButtons.addView(refreshButton)
        bottomButtons.addView(createScriptButton)
        bottomButtons.addView(showAllButton)
        layout.addView(bottomButtons)

        setContentView(layout)
    }

    private fun updateScriptList() {
        try {
            val scriptsDir = File(Environment.getExternalStorageDirectory(), "MyScripts")
            scriptsDir.mkdirs()
            File(scriptsDir, "icons").mkdirs()

            IniHelper.cleanupOrphanedConfigs(this)

            val scripts = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }?.map { file ->
                Script(file.nameWithoutExtension, "${scriptsDir.absolutePath}/${file.name}")
            }?.filter { showAllScripts || IniHelper.getScriptConfig(it.name).isActive } ?: emptyList()
            adapter.updateScripts(scripts)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка чтения скриптов: ${e.message}", Toast.LENGTH_SHORT).show()
            adapter.updateScripts(emptyList())
        }
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
                    try {
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
                    } catch (e: Exception) {
                        Toast.makeText(this, "Ошибка создания скрипта: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun onTestRun(script: Script) {
        try {
            TermuxHelper.showPermissionDialogIfNeeded(this) {
                val file = File(script.path)
                if (!file.exists()) {
                    Toast.makeText(this, "Скрипт не существует: ${script.path}", Toast.LENGTH_SHORT).show()
                    return@showPermissionDialogIfNeeded
                }
                TermuxHelper.startTermuxSilently(this)
                Thread.sleep(500)
                TermuxHelper.sendCommand(this, script.path)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка выполнения скрипта: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menu?.add(0, 1, 0, "О программе")
        menu?.add(0, 2, 0, "Инструкции")
        menu?.add(0, 3, 0, "Настройки")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                try {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    Toast.makeText(this, "AboutActivity не найдена: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }
            2 -> {
                try {
                    val intent = Intent(this, InstructionsActivity::class.java)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    Toast.makeText(this, "InstructionsActivity не найдена: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }
            3 -> {
                try {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    Toast.makeText(this, "SettingsActivity не найдена: ${e.message}", Toast.LENGTH_SHORT).show()
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}