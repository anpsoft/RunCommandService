import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

        val createShortcutButton = Button(this).apply {
            text = "Создать ярлык"
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

        // Шапка для списка
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply { text = "Иконка"; width = 48.dp })
            addView(TextView(this@MainActivity).apply { text = "Имя / Описание"; setPadding(8.dp, 0, 0, 0); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            addView(TextView(this@MainActivity).apply { text = ""; width = 48.dp }) // Для активен
            addView(TextView(this@MainActivity).apply { text = ""; width = 48.dp }) // Для ярлык
            addView(TextView(this@MainActivity).apply { text = "Тест"; width = 60.dp })
        }
        layout.addView(headerLayout)

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        adapter = ScriptAdapter(this, ::onScriptSettings, ::onTestRun)
        recyclerView.adapter = adapter
        layout.addView(recyclerView)

        // Кнопки внизу в одной строке
        val bottomButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val refreshButton = Button(this).apply {
            text = "Обновить"
            setOnClickListener { updateScriptList() }
        }
        val createScriptButton = Button(this).apply {
            text = "Новый"
            setOnClickListener { createNewScript() }
        }
        val showAllButton = Button(this).apply {
            text = "Все"
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

    private fun requestPermissions() {
        val permissions = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "com.android.launcher.permission.INSTALL_SHORTCUT"
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
                    scriptFile.createNewFile()
                    scriptFile.setExecutable(true)
                    IniHelper.addScriptConfig(name, ScriptConfig(name = name, isActive = true))
                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(Uri.fromFile(scriptFile), "text/plain")
                    }
                    startActivity(intent)
                    updateScriptList()
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

    private fun onTestRun(script: Script) {
        if (!TermuxHelper.hasPermission(this)) {
            showPermissionDialog()
        } else {
            val file = File(script.path)
            if (!file.canExecute()) {
                file.setExecutable(true)
            }
            TermuxHelper.startTermuxSilently(this)
            Thread.sleep(1000)
            TermuxHelper.sendCommand(this, script.path)
        }
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}