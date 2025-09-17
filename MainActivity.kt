package com.yourcompany.yourapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : Activity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScriptAdapter
    private var showAllScripts = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Существующие кнопки
        val createShortcutButton = Button(this).apply {
            text = "Создать ярлык"
            setOnClickListener { 
                TermuxHelper.createShortcut(
                    this@MainActivity, 
                    "UpdateWDS", 
                    "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh",
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
                    TermuxHelper.sendCommand(this@MainActivity, "/data/data/com.termux/files/home/.shortcuts/UpdateWDS.sh")
                }
            }
        }

        // RecyclerView для списка скриптов
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        adapter = ScriptAdapter(this, ::onScriptSettings, ::onTestRun)
        recyclerView.adapter = adapter
        updateScriptList()

        // Кнопки внизу
        val refreshButton = Button(this).apply {
            text = "Обновить список"
            setOnClickListener { updateScriptList() }
        }

        val createScriptButton = Button(this).apply {
            text = "Создать скрипт"
            setOnClickListener { createNewScript() }
        }

        val showAllButton = Button(this).apply {
            text = "Показать все"
            setOnClickListener {
                showAllScripts = !showAllScripts
                text = if (showAllScripts) "Показать активные" else "Показать все"
                updateScriptList()
            }
        }

        layout.addView(createShortcutButton)
        layout.addView(runCommandButton)
        layout.addView(recyclerView)
        layout.addView(refreshButton)
        layout.addView(createScriptButton)
        layout.addView(showAllButton)
        setContentView(layout)
    }

    private fun updateScriptList() {
        val scriptsDir = File(Environment.getExternalStorageDirectory(), "MyScripts")
        val scripts = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }?.map { file ->
            Script(file.nameWithoutExtension, file.absolutePath)
        }?.filter { showAllScripts || IniHelper.getScriptConfig(it.name).isActive } ?: emptyList()
        adapter.updateScripts(scripts)
    }

    private fun createNewScript() {
        // Диалог для имени скрипта (реализацию добавлю позже, если нужно)
        val scriptName = "new_script" // Заглушка, заменить на диалог
        val scriptFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh")
        scriptFile.createNewFile()
        IniHelper.addScriptConfig(scriptName, ScriptConfig(name = scriptName, isActive = true))
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(Uri.fromFile(scriptFile), "text/plain")
        }
        startActivity(intent)
        updateScriptList()
    }

    private fun onScriptSettings(script: Script) {
        val intent = Intent(this, ScriptSettingsActivity::class.java).apply {
            putExtra("script_name", script.name)
        }
        startActivity(intent)
    }

    private fun onTestRun(script: Script) {
        if (!TermuxHelper.hasPermission(this)) {
            showPermissionDialog()
        } else {
            TermuxHelper.startTermuxSilently(this)
            Thread.sleep(1000)
            TermuxHelper.sendCommand(this, script.path)
        }
    }
}