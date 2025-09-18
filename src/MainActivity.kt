import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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

        requestPermissions()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Заголовки
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply { text = "Иконка"; width = 48.dp })
            addView(TextView(this@MainActivity).apply { text = "Имя"; width = 100.dp })
            addView(TextView(this@MainActivity).apply { text = "Описание"; width = 150.dp })
            addView(TextView(this@MainActivity).apply { text = "Активен"; width = 48.dp })
            addView(TextView(this@MainActivity).apply { text = "Ярлык"; width = 48.dp })
            addView(TextView(this@MainActivity).apply { text = "Тест"; width = 60.dp })
            addView(TextView(this@MainActivity).apply { text = "Ред."; width = 60.dp })
        }
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
        updateScriptList()
    }

    override fun onResume() {
        super.onResume()
        updateScriptList()
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

    private fun updateScriptList() {
        val scriptsDir = File(Environment.getExternalStorageDirectory(), "MyScripts")
        scriptsDir.mkdirs()
        val scripts = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }?.map { file ->
            Script(file.nameWithoutExtension, file.absolutePath)
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
                        IniHelper.addScriptConfig(name, ScriptConfig(name = name, isActive = false))
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
        updateScriptList()
    }

    private fun onTestRun(script: Script) {
        // здесь твоя логика тестового запуска
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}