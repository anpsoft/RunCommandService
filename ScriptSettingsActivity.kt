import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.GridView
import android.widget.Toast
import java.io.File

class ScriptSettingsActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_settings)

        val scriptName = intent.getStringExtra("script_name")
        if (scriptName == null) {
            finish()
            return
        }
        val config = IniHelper.getScriptConfig(scriptName)

        val nameEdit = findViewById<EditText>(R.id.name_edit)
        val descriptionEdit = findViewById<EditText>(R.id.description_edit)
        val iconView = findViewById<ImageView>(R.id.icon_view)
        val activeCheckBox = findViewById<CheckBox>(R.id.active_checkbox)
        val renameButton = findViewById<Button>(R.id.rename_button)
        val deleteButton = findViewById<Button>(R.id.delete_button)
        val iconButton = findViewById<Button>(R.id.icon_button)

        nameEdit.setText(config.name.ifEmpty { scriptName })
        descriptionEdit.setText(config.description)
        activeCheckBox.isChecked = config.isActive

        activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            IniHelper.updateScriptConfig(scriptName, config.copy(isActive = isChecked))
        }

        iconButton.setOnClickListener {
            showIconPicker(scriptName, config)
        }

        renameButton.setOnClickListener {
            val newNameEdit = EditText(this).apply { setText(scriptName) }
            AlertDialog.Builder(this)
                .setTitle("Переименовать скрипт")
                .setView(newNameEdit)
                .setPositiveButton("ОК") { _, _ ->
                    val newName = newNameEdit.text.toString()
                    if (newName != scriptName && newName.isNotEmpty()) {
                        val oldFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh")
                        val newFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$newName.sh")
                        if (oldFile.exists() && !newFile.exists()) {
                            oldFile.renameTo(newFile)
                            IniHelper.renameScriptConfig(scriptName, newName, config.copy(name = newName))
                            finish()
                        } else {
                            Toast.makeText(this, "Ошибка переименования", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Удалить скрипт")
                .setMessage("Уверены?")
                .setPositiveButton("Да") { _, _ ->
                    File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh").delete()
                    IniHelper.deleteScriptConfig(scriptName)
                    finish()
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }

    private fun showIconPicker(scriptName: String, config: ScriptConfig) {
        val iconsDir = File(Environment.getExternalStorageDirectory(), "MyScripts/icons")
        iconsDir.mkdirs()
        val icons = iconsDir.listFiles { _, name -> name.endsWith(".png") || name.endsWith(".jpg") } ?: return

        val gridView = GridView(this).apply {
            numColumns = 3
            adapter = IconAdapter(this@ScriptSettingsActivity, icons.toList())
            setOnItemClickListener { _, _, position, _ ->
                val selectedIcon = icons[position].name
                val newConfig = config.copy(icon = selectedIcon)
                IniHelper.updateScriptConfig(scriptName, newConfig)
                Toast.makeText(this@ScriptSettingsActivity, "Иконка выбрана", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите иконку")
            .setView(gridView)
            .setNegativeButton("Отмена", null)
            .show()
    }
}