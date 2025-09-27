package com.yourcompany.yourapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
        }

        val scriptsEdit = EditText(this).apply {
            hint = "Папка для скриптов"
            setText(IniHelper.getScriptsDir())
        }

        val iconsEdit = EditText(this).apply {
            hint = "Папка для иконок"
            setText(IniHelper.getIconsDir())
        }

        val saveButton = Button(this).apply {
            text = "Сохранить"
            setOnClickListener {
                val scriptsDir = scriptsEdit.text.toString()
                val iconsDir = iconsEdit.text.toString()
                if (scriptsDir.isNotEmpty() && iconsDir.isNotEmpty()) {
                    IniHelper.updateSettings(/* this@SettingsActivity, */ scriptsDir, iconsDir)
                    if (!File(scriptsDir).mkdirs()) Toast.makeText(this@SettingsActivity, "Ошибка создания папки скриптов", Toast.LENGTH_SHORT).show()
                    if (!File(iconsDir).mkdirs()) Toast.makeText(this@SettingsActivity, "Ошибка создания папки иконок", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this@SettingsActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        val cancelButton = Button(this).apply {
            text = "Отмена"
            setOnClickListener { finish() }
        }

        val buttonsLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttonsLayout.addView(saveButton)
        buttonsLayout.addView(cancelButton)

        layout.addView(scriptsEdit)
        layout.addView(iconsEdit)
        layout.addView(buttonsLayout)

        setContentView(layout)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}