package com.yourcompany.yourapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
            hint = "Отображаемое имя"
            setText(config.name.ifEmpty { scriptName })
        }

        val descriptionEdit = EditText(this).apply {
            hint = "Описание"
            setText(config.description)
        }

        val fileLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val fileLabel = TextView(this).apply {
            text = "Файл: $scriptName.sh"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val editFileButton = Button(this).apply {
            text = "Редактировать"
            setOnClickListener {
                val scriptFile = File(IniHelper.getScriptsDir(this@ScriptSettingsActivity), "$scriptName.sh")
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(Uri.fromFile(scriptFile), "text/plain")
                }
                val chooser = Intent.createChooser(intent, "Открыть редактором")
                if (chooser.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                } else {
                    Toast.makeText(this@ScriptSettingsActivity, "Нет редактора", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val renameButton = Button(this).apply {
            text = "Переименовать"
            setOnClickListener {
                val newNameEdit = EditText(this@ScriptSettingsActivity).apply { setText(scriptName) }
                AlertDialog.Builder(this@ScriptSettingsActivity)
                    .setTitle("Переименовать файл скрипта")
                    .setView(newNameEdit)
                    .setPositiveButton("ОК") { _, _ ->
                        val newName = newNameEdit.text.toString()
                        if (newName != scriptName && newName.isNotEmpty()) {
                            val oldFile = File(IniHelper.getScriptsDir(this@ScriptSettingsActivity), "$scriptName.sh")
                            val newFile = File(IniHelper.getScriptsDir(this@ScriptSettingsActivity), "$newName.sh")
                            if (oldFile.exists() && !newFile.exists() && oldFile.renameTo(newFile)) {
                                val currentConfig = IniHelper.getScriptConfig(scriptName)
                                IniHelper.deleteScriptConfig(scriptName)
                                IniHelper.addScriptConfig(newName, currentConfig)
                                Toast.makeText(this@ScriptSettingsActivity, "Файл переименован", Toast.LENGTH_SHORT).show()
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
        fileLayout.addView(fileLabel)
        fileLayout.addView(editFileButton)
        fileLayout.addView(renameButton)

        val iconLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val iconView = ImageView(this).apply {
            val iconFile = File(IniHelper.getIconsDir(this@ScriptSettingsActivity), config.icon)
            if (config.icon.isNotEmpty() && iconFile.exists()) {
                setImageURI(Uri.fromFile(iconFile))
            } else {
                setImageResource(ShortcutManager.getIconResource(config.icon))
            }
            layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
        }
        val iconButton = Button(this).apply {
            text = "Выбрать иконку"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showIconPicker(scriptName, config, iconView) }
        }
        iconLayout.addView(iconView)
        iconLayout.addView(iconButton)

        val activeCheckBox = CheckBox(this).apply {
            text = "Показывать в списке"
            isChecked = config.isActive
        }

        val testButton = Button(this).apply {
            text = "Тест скрипта"
            setOnClickListener {
                val scriptPath = "${IniHelper.getScriptsDir(this@ScriptSettingsActivity)}/$scriptName.sh"
                val file = File(scriptPath)
                if (!file.exists()) {
                    Toast.makeText(this@ScriptSettingsActivity, "Скрипт не существует", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!TermuxHelper.hasPermission(this@ScriptSettingsActivity)) {
                    Toast.makeText(this@ScriptSettingsActivity, "Нет разрешения Termux", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TermuxHelper.startTermuxSilently(this@ScriptSettingsActivity)
                Thread.sleep(1500)
                TermuxHelper.sendCommand(this@ScriptSettingsActivity, scriptPath)
            }
        }

        val deleteButton = Button(this).apply {
            text = "Удалить"
            setOnClickListener {
                AlertDialog.Builder(this@ScriptSettingsActivity)
                    .setTitle("Удалить скрипт")
                    .setMessage("Уверены?")
                    .setPositiveButton("Да") { _, _ ->
                        File(IniHelper.getScriptsDir(this@ScriptSettingsActivity), "$scriptName.sh").delete()
                        IniHelper.deleteScriptConfig(scriptName)
                        finish()
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        }

        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val saveButton = Button(this).apply {
            text = "Сохранить"
            setOnClickListener {
                val oldConfig = config
                val newConfig = config.copy(
                    name = nameEdit.text.toString(),
                    description = descriptionEdit.text.toString(),
                    isActive = activeCheckBox.isChecked
                )

                if (oldConfig.hasShortcut && (oldConfig.name != newConfig.name || oldConfig.icon != newConfig.icon)) {
                    showShortcutUpdateDialog(scriptName, oldConfig, newConfig)
                } else {
                    IniHelper.updateScriptConfig(scriptName, newConfig)
                    Toast.makeText(this@ScriptSettingsActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        val cancelButton = Button(this).apply {
            text = "Отмена"
            setOnClickListener { finish() }
        }
        buttonsLayout.addView(saveButton)
        buttonsLayout.addView(cancelButton)

        layout.addView(nameEdit)
        layout.addView(descriptionEdit)
        layout.addView(fileLayout)
        layout.addView(iconLayout)
        layout.addView(activeCheckBox)
        layout.addView(testButton)
        layout.addView(deleteButton)
        layout.addView(buttonsLayout)

        setContentView(layout)

        activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            config = config.copy(isActive = isChecked)
        }
    }

    private fun showIconPicker(scriptName: String, config: ScriptConfig, iconView: ImageView) {
        val iconsDir = File(IniHelper.getIconsDir(this))
        iconsDir.mkdirs()
        val icons = iconsDir.listFiles { _, name -> name.endsWith(".png") || name.endsWith(".jpg") } ?: arrayOf()
        if (icons.isEmpty()) {
            Toast.makeText(this, "Нет иконок в ${IniHelper.getIconsDir(this)}", Toast.LENGTH_SHORT).show()
            return
        }

        val gridView = GridView(this).apply {
            numColumns = 3
            adapter = IconAdapter(this@ScriptSettingsActivity, icons.toList())
            setOnItemClickListener { _, _, position, _ ->
                val selectedIcon = icons[position].name
                val newConfig = config.copy(icon = selectedIcon)
                IniHelper.updateScriptConfig(scriptName, newConfig)
                iconView.setImageURI(Uri.fromFile(icons[position]))
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

    private fun showShortcutUpdateDialog(scriptName: String, oldConfig: ScriptConfig, newConfig: ScriptConfig) {
        val scriptPath = File(IniHelper.getScriptsDir(this), "$scriptName.sh").absolutePath
        ShortcutManager.showShortcutUpdateDialog(this, scriptName, scriptPath, oldConfig, newConfig)
        finish()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}