package com.yourcompany.yourapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import java.io.File

class ScriptSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_script_settings)

        val scriptName = intent.getStringExtra("script_name") ?: return
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
        // Установить иконку (реализация позже)

        activeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            IniHelper.updateScriptConfig(scriptName, config.copy(isActive = isChecked))
        }

        iconButton.setOnClickListener {
            // Открыть выбор иконки (реализация позже)
        }

        renameButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Переименовать скрипт")
                .setMessage("Введите новое имя")
                .setView(EditText(this).apply { setText(scriptName) })
                .setPositiveButton("ОК") { _, _ ->
                    val newName = (it as EditText).text.toString()
                    val oldFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh")
                    val newFile = File(Environment.getExternalStorageDirectory(), "MyScripts/$newName.sh")
                    oldFile.renameTo(newFile)
                    IniHelper.renameScriptConfig(scriptName, newName, config.copy(name = newName))
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Удалить скрипт")
                .setMessage("Вы уверены, что хотите удалить $scriptName?")
                .setPositiveButton("Да") { _, _ ->
                    File(Environment.getExternalStorageDirectory(), "MyScripts/$scriptName.sh").delete()
                    IniHelper.deleteScriptConfig(scriptName)
                    finish()
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }
}