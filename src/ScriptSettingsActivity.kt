import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class ScriptSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scriptName = intent.getStringExtra("script_name")
        if (scriptName == null) {
            finish()
            return
        }
        var config = IniHelper.getScriptConfig(scriptName)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        val nameEdit = EditText(this).apply {
            hint = "Имя скрипта"
            setText(config.name.ifEmpty { scriptName })
        }
        val descriptionEdit = EditText(this).apply {
            hint = "Описание"
            setText(config.description)
        }
        val iconView = ImageView(this).apply {
            setImageResource(if (config.icon.isNotEmpty()) R.mipmap.ic_no_icon else R.mipmap.ic_no_icon)
            layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
        }
        val iconButton = Button(this).apply {
            text = "Выбрать иконку"
            setOnClickListener { showIconPicker(scriptName, config) }
        }
        val activeCheckBox = CheckBox(this).apply {
            isChecked = config.isActive
            contentDescription = "Активен"
        }
        val renameButton = Button(this).apply {
            text = "Переименовать"
            setOnClickListener {
                val newNameEdit = EditText(this@ScriptSettingsActivity).apply { setText(scriptName) }
                AlertDialog.Builder(this@ScriptSettingsActivity)
                    .setTitle("Переименовать скрипт")
                    .setView(newNameEdit)
                    .setPositiveButton("ОК") { _, _ ->
                        val newName = newNameEdit.text.toString()
                        if (newName != scriptName && newName.isNotEmpty()) {
                            val oldFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh")
                            val newFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$newName.sh")
                            if (oldFile.exists() && !newFile.exists() && oldFile.renameTo(newFile)) {
                                IniHelper.renameScriptConfig(scriptName, newName, config.copy(name = newName))
                                Toast.makeText(this@ScriptSettingsActivity, "Скрипт переименован", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this@ScriptSettingsActivity, "Ошибка переименования", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
        val deleteButton = Button(this).apply {
            text = "Удалить"
            setOnClickListener {
                AlertDialog.Builder(this@ScriptSettingsActivity)
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

        layout.addView(nameEdit)
        layout.addView(descriptionEdit)
        layout.addView(iconView)
        layout.addView(iconButton)
        layout.addView(activeCheckBox)
        layout.addView(renameButton)
        layout.addView(deleteButton)

        setContentView(layout)

        val saveChanges = {
            config = config.copy(
                name = nameEdit.text.toString(),
                description = descriptionEdit.text.toString(),
                isActive = activeCheckBox.isChecked
            )
            IniHelper.updateScriptConfig(scriptName, config)
        }

        nameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { saveChanges() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        descriptionEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { saveChanges() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(isActive = isChecked)
            IniHelper.updateScriptConfig(scriptName, config)
        }
    }

    private fun showIconPicker(scriptName: String, config: ScriptConfig) {
        val iconsDir = File(Environment.getExternalStorageDirectory(), "MyScripts/icons")
        iconsDir.mkdirs()
        val icons = iconsDir.listFiles { _, name -> name.endsWith(".png") || name.endsWith(".jpg") } ?: arrayOf()
        if (icons.isEmpty()) {
            Toast.makeText(this, "Нет иконок в /sdcard/MyScripts/icons", Toast.LENGTH_SHORT).show()
            return
        }

        val gridView = android.widget.GridView(this).apply {
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}